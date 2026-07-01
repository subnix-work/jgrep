package de.subnix.jgrep;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.subnix.shared.BaseGrepCommand;
import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine.Command;

import java.nio.file.Path;

@Command(
    name = "jgrep",
    mixinStandardHelpOptions = true,
    version = "1.3.0",
    description = "grep for JSON using jq filters",
    customSynopsis = {
        "jgrep [OPTIONS] FILTER [FILE...]",
        "jgrep completion SHELL"
    },
    subcommands = CompletionCommand.class
)
@TopCommand
public class JGrepCommand extends BaseGrepCommand
{
    @Override
    protected boolean isSupportedFile(Path path)
    {
        return path.toString().endsWith(".json");
    }

    @Override
    protected ObjectMapper inputMapper()
    {
        return mapper;
    }
}
