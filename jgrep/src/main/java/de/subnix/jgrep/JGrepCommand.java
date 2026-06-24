package de.subnix.jgrep;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import net.thisptr.jackson.jq.JsonQuery;
import net.thisptr.jackson.jq.exception.JsonQueryException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@Command(
    name = "jgrep",
    mixinStandardHelpOptions = true,
    version = "1.0.0",
    description = "grep for JSON using jq filters"
)
public class JGrepCommand implements Callable<Integer>
{
    @Parameters(index = "0", paramLabel = "FILTER",
                description = "jq filter (e.g. '.name', 'select(.age > 18)')")
    String filter;

    @Parameters(index = "1..*", paramLabel = "FILE",
                description = "JSON files to search (reads from stdin if omitted)")
    List<Path> files;

    @Option(names = {"-r", "--recursive"}, description = "Recurse into directories")
    boolean recursive;

    @Option(names = {"-l", "--files-with-matches"}, description = "Only print filenames with matches")
    boolean filesOnly;

    @Option(names = {"-c", "--count"}, description = "Print match count per file")
    boolean countOnly;

    @Option(names = {"--pretty"}, description = "Pretty-print JSON output")
    boolean pretty;

    @Option(names = {"--no-color"}, description = "Disable colored output")
    boolean noColor;

    @Inject
    ObjectMapper mapper;

    @Inject
    JsonMatcher matcher;

    private static final String CYAN = "\u001B[36m";
    private static final String RESET = "\u001B[0m";

    @Override
    public Integer call()
    {
        JsonQuery query;
        try
        {
            query = matcher.compile(filter);
        }
        catch (JsonQueryException e)
        {
            System.err.println("jgrep: invalid filter: " + e.getMessage());
            return 2;
        }

        boolean foundAnyMatch = false;

        if (files == null || files.isEmpty())
        {
            if (processStream(System.in, null, query, false) > 0)
            {
                foundAnyMatch = true;
            }
        }
        else
        {
            List<Path> allFiles = resolveFiles(files);
            boolean showFilename = allFiles.size() > 1;
            for (Path file : allFiles)
            {
                try (InputStream is = Files.newInputStream(file))
                {
                    if (processStream(is, file.toString(), query, showFilename) > 0)
                    {
                        foundAnyMatch = true;
                    }
                }
                catch (IOException e)
                {
                    System.err.println("jgrep: " + file + ": " + e.getMessage());
                }
            }
        }

        return foundAnyMatch ? 0 : 1;
    }

    private List<Path> resolveFiles(List<Path> paths)
    {
        List<Path> result = new ArrayList<>();
        for (Path path : paths)
        {
            if (Files.isDirectory(path))
            {
                if (recursive)
                {
                    try (Stream<Path> walk = Files.walk(path))
                    {
                        walk.filter(Files::isRegularFile)
                            .filter(p -> p.toString().endsWith(".json"))
                            .sorted()
                            .forEach(result::add);
                    }
                    catch (IOException e)
                    {
                        System.err.println("jgrep: " + path + ": " + e.getMessage());
                    }
                }
                else
                {
                    System.err.println("jgrep: " + path + ": Is a directory");
                }
            }
            else
            {
                result.add(path);
            }
        }
        return result;
    }

    private int processStream(InputStream is, String filename, JsonQuery query, boolean showFilename)
    {
        int matchCount = 0;
        try
        {
            var parser = mapper.getFactory().createParser(is);
            MappingIterator<JsonNode> iterator = mapper.readerFor(JsonNode.class).readValues(parser);
            while (iterator.hasNextValue())
            {
                JsonNode node;
                try
                {
                    node = iterator.nextValue();
                }
                catch (JsonParseException e)
                {
                    System.err.println("jgrep: " + source(filename) + ": parse error: " + e.getOriginalMessage());
                    continue;
                }

                List<JsonNode> results;
                try
                {
                    results = matcher.apply(query, node);
                }
                catch (JsonQueryException e)
                {
                    System.err.println("jgrep: filter error: " + e.getMessage());
                    continue;
                }

                if (!results.isEmpty())
                {
                    matchCount += results.size();
                    if (!countOnly && !filesOnly)
                    {
                        for (JsonNode result : results)
                        {
                            printResult(filename, showFilename, result);
                        }
                    }
                }
            }
        }
        catch (IOException e)
        {
            System.err.println("jgrep: " + source(filename) + ": " + e.getMessage());
        }

        if (countOnly)
        {
            String prefix = (showFilename && filename != null) ? colorize(filename + ":", CYAN) : "";
            System.out.println(prefix + matchCount);
        }
        else if (filesOnly && matchCount > 0 && filename != null)
        {
            System.out.println(colorize(filename, CYAN));
        }

        return matchCount;
    }

    private void printResult(String filename, boolean showFilename, JsonNode result)
    {
        String output = formatResult(result);
        if (showFilename && filename != null)
        {
            System.out.println(colorize(filename + ":", CYAN) + output);
        }
        else
        {
            System.out.println(output);
        }
    }

    private String formatResult(JsonNode node)
    {
        try
        {
            if (pretty)
            {
                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
            }
            else if (node.isTextual())
            {
                return node.asText();
            }
            else
            {
                return mapper.writeValueAsString(node);
            }
        }
        catch (IOException e)
        {
            return node.toString();
        }
    }

    private String source(String filename)
    {
        return filename != null ? filename : "stdin";
    }

    private boolean useColor()
    {
        if (noColor) return false;
        if (System.getenv("NO_COLOR") != null) return false;
        return System.console() != null;
    }

    private String colorize(String text, String color)
    {
        return useColor() ? color + text + RESET : text;
    }
}
