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
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Command(
    name = "jgrep",
    mixinStandardHelpOptions = true,
    version = "1.1.0",
    description = "grep for JSON using jq filters"
)
public class JGrepCommand implements Callable<Integer>
{
    @Parameters(index = "0", paramLabel = "FILTER", arity = "0..1",
                description = "jq filter expression. Required unless -f is used.")
    String filterArg;

    @Parameters(index = "1..*", paramLabel = "FILE",
                description = "JSON files to search (reads from stdin if omitted)")
    List<Path> files;

    @Option(names = {"-r", "--recursive"}, description = "Recurse into directories")
    boolean recursive;

    @Option(names = {"-l", "--files-with-matches"}, description = "Only print filenames with matches")
    boolean filesOnly;

    @Option(names = {"-c", "--count"}, description = "Print match count per file")
    boolean countOnly;

    @Option(names = {"-s", "--slurp"}, description = "Collect all results into a single JSON array")
    boolean slurp;

    @Option(names = {"-n", "--null-input"}, description = "Use null as input instead of reading files")
    boolean nullInput;

    @Option(names = {"-f", "--from-file"}, paramLabel = "FILE",
            description = "Read filter expression from a file")
    Path filterFile;

    @Option(names = {"--pretty"}, description = "Pretty-print JSON output")
    boolean pretty;

    @Option(names = {"--no-color"}, description = "Disable colored output")
    boolean noColor;

    @Inject
    ObjectMapper mapper;

    @Inject
    JsonMatcher matcher;

    private static final String CYAN    = "\u001B[36m";
    private static final String GREEN   = "\u001B[32m";
    private static final String YELLOW  = "\u001B[33m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String RESET   = "\u001B[0m";

    // Matches JSON keys, string values, numbers, and keywords for syntax highlighting
    private static final Pattern JSON_TOKEN_PATTERN = Pattern.compile(
        "(?<key>\"(?:[^\"\\\\]|\\\\.)*\")(?=\\s*:)" +
        "|(?<str>\"(?:[^\"\\\\]|\\\\.)*\")" +
        "|(?<num>-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)" +
        "|(?<kw>true|false|null)"
    );

    @Override
    public Integer call()
    {
        String filter = resolveFilter();
        if (filter == null) return 2;

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

        List<JsonNode> slurpBuffer = slurp ? new ArrayList<>() : null;
        boolean foundAnyMatch;

        if (nullInput)
        {
            foundAnyMatch = runNullInput(query, slurpBuffer);
        }
        else
        {
            List<Path> allFiles = resolveFiles();
            boolean showFilename = allFiles != null && allFiles.size() > 1;

            if (allFiles == null || allFiles.isEmpty())
            {
                foundAnyMatch = processStream(System.in, null, query, false, slurpBuffer);
            }
            else
            {
                foundAnyMatch = false;
                for (Path file : allFiles)
                {
                    try (InputStream is = Files.newInputStream(file))
                    {
                        if (processStream(is, file.toString(), query, showFilename, slurpBuffer))
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
        }

        if (slurp)
        {
            foundAnyMatch = !slurpBuffer.isEmpty();
            printSlurpResult(slurpBuffer);
        }

        return foundAnyMatch ? 0 : 1;
    }

    private String resolveFilter()
    {
        if (filterFile != null)
        {
            try
            {
                return Files.readString(filterFile).trim();
            }
            catch (IOException e)
            {
                System.err.println("jgrep: cannot read filter file: " + e.getMessage());
                return null;
            }
        }
        if (filterArg == null)
        {
            System.err.println("jgrep: filter expression required (or use -f to read from file)");
            return null;
        }
        return filterArg;
    }

    private List<Path> resolveFiles()
    {
        List<Path> inputPaths = new ArrayList<>();
        // When -f is used, filterArg slot holds the first file path instead
        if (filterFile != null && filterArg != null)
        {
            inputPaths.add(Path.of(filterArg));
        }
        if (files != null) inputPaths.addAll(files);
        if (inputPaths.isEmpty()) return inputPaths;

        List<Path> result = new ArrayList<>();
        for (Path path : inputPaths)
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

    private boolean runNullInput(JsonQuery query, List<JsonNode> slurpBuffer)
    {
        List<JsonNode> results;
        try
        {
            results = matcher.apply(query, mapper.nullNode());
        }
        catch (JsonQueryException e)
        {
            System.err.println("jgrep: filter error: " + e.getMessage());
            return false;
        }

        if (results.isEmpty()) return false;

        if (slurpBuffer != null)
        {
            slurpBuffer.addAll(results);
        }
        else if (countOnly)
        {
            System.out.println(results.size());
        }
        else
        {
            for (JsonNode result : results)
            {
                printResult(null, false, result);
            }
        }
        return true;
    }

    private boolean processStream(InputStream is, String filename, JsonQuery query,
                                  boolean showFilename, List<JsonNode> slurpBuffer)
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
                    var loc = e.getLocation();
                    String location = loc != null
                        ? " (line " + loc.getLineNr() + ", col " + loc.getColumnNr() + ")"
                        : "";
                    System.err.println("jgrep: " + source(filename) + ": parse error" + location
                        + ": " + e.getOriginalMessage());
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
                    if (slurpBuffer != null)
                    {
                        slurpBuffer.addAll(results);
                    }
                    else if (!countOnly && !filesOnly)
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

        if (slurpBuffer == null)
        {
            if (countOnly)
            {
                String prefix = (showFilename && filename != null) ? colorize(filename + ":", CYAN) : "";
                System.out.println(prefix + matchCount);
            }
            else if (filesOnly && matchCount > 0 && filename != null)
            {
                System.out.println(colorize(filename, CYAN));
            }
        }

        return matchCount > 0;
    }

    private void printSlurpResult(List<JsonNode> results)
    {
        try
        {
            var arrayNode = mapper.createArrayNode();
            results.forEach(arrayNode::add);
            String output = pretty
                ? mapper.writerWithDefaultPrettyPrinter().writeValueAsString(arrayNode)
                : mapper.writeValueAsString(arrayNode);
            if (useColor()) output = colorizeJson(output);
            System.out.println(output);
        }
        catch (IOException e)
        {
            System.err.println("jgrep: error formatting output: " + e.getMessage());
        }
    }

    private void printResult(String filename, boolean showFilename, JsonNode result)
    {
        String output = formatResult(result);
        if (useColor() && !result.isTextual()) output = colorizeJson(output);
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
            if (node.isTextual())
            {
                return node.asText();
            }
            else if (pretty)
            {
                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
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

    private String colorizeJson(String json)
    {
        if (json == null || json.isBlank()) return json;
        var sb = new StringBuilder();
        var m = JSON_TOKEN_PATTERN.matcher(json);
        int last = 0;
        while (m.find())
        {
            sb.append(json, last, m.start());
            if (m.group("key") != null)
                sb.append(CYAN).append(m.group("key")).append(RESET);
            else if (m.group("str") != null)
                sb.append(GREEN).append(m.group("str")).append(RESET);
            else if (m.group("num") != null)
                sb.append(YELLOW).append(m.group("num")).append(RESET);
            else
                sb.append(MAGENTA).append(m.group("kw")).append(RESET);
            last = m.end();
        }
        sb.append(json, last, json.length());
        return sb.toString();
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
