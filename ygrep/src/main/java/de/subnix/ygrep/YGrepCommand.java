package de.subnix.ygrep;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import de.subnix.shared.BaseGrepCommand;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine.Command;

import java.nio.file.Path;

@Command(
    name = "ygrep",
    mixinStandardHelpOptions = true,
    version = "1.2.0",
    description = "grep for YAML using jq filters",
    customSynopsis = {
        "ygrep [OPTIONS] FILTER [FILE...]",
        "ygrep completion SHELL"
    },
    subcommands = CompletionCommand.class
)
@TopCommand
public class YGrepCommand extends BaseGrepCommand
{
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @Override
    protected boolean isSupportedFile(Path path)
    {
        String name = path.toString();
        return name.endsWith(".yaml") || name.endsWith(".yml");
    }

    @Override
    protected ObjectMapper inputMapper()
    {
        return yamlMapper;
    }
}
