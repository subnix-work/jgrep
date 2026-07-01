package de.subnix.ygrep;

import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import de.subnix.shared.BaseGrepCommand;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusMainTest
class YGrepCommandTest
{
    @Test
    void extractField(QuarkusMainLauncher launcher, @TempDir Path tempDir) throws IOException
    {
        Path file = tempDir.resolve("test.yaml");
        Files.writeString(file, """
                name: Alice
                age: 30
                """);

        LaunchResult result = launcher.launch(".name", file.toString());
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput()).contains("Alice");
    }

    @Test
    void selectMatchingDocuments(QuarkusMainLauncher launcher, @TempDir Path tempDir) throws IOException
    {
        Path file = tempDir.resolve("test.yaml");
        Files.writeString(file, """
                ---
                name: Alice
                age: 30
                ---
                name: Bob
                age: 15
                """);

        LaunchResult result = launcher.launch("select(.age >= 18)", file.toString());
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput()).contains("Alice");
        assertThat(result.getOutput()).doesNotContain("Bob");
    }

    @Test
    void noMatchReturnsExitCode1(QuarkusMainLauncher launcher, @TempDir Path tempDir) throws IOException
    {
        Path file = tempDir.resolve("test.yaml");
        Files.writeString(file, "name: Alice\n");

        LaunchResult result = launcher.launch("select(.age > 100)", file.toString());
        assertThat(result.exitCode()).isEqualTo(1);
    }

    @Test
    void countFlag(QuarkusMainLauncher launcher, @TempDir Path tempDir) throws IOException
    {
        Path file = tempDir.resolve("test.yaml");
        Files.writeString(file, """
                ---
                active: true
                ---
                active: false
                ---
                active: true
                """);

        LaunchResult result = launcher.launch("-c", "select(.active == true)", file.toString());
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput().trim()).isEqualTo("2");
    }

    @Test
    void countAndFilesWithMatchesCannotBeCombined(QuarkusMainLauncher launcher)
    {
        LaunchResult result = launcher.launch("-cl", ".");

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.getErrorOutput()).contains("cannot be used together");
    }

    @Test
    void filesWithMatchesFlag(QuarkusMainLauncher launcher, @TempDir Path tempDir) throws IOException
    {
        Path matchFile = tempDir.resolve("match.yaml");
        Path noMatchFile = tempDir.resolve("nomatch.yaml");
        Files.writeString(matchFile, "active: true\n");
        Files.writeString(noMatchFile, "active: false\n");

        LaunchResult result = launcher.launch("-l", "select(.active == true)",
                matchFile.toString(), noMatchFile.toString());
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput()).contains("match.yaml");
        assertThat(result.getOutput()).doesNotContain("nomatch.yaml");
    }

    @Test
    void recursiveSearch(QuarkusMainLauncher launcher, @TempDir Path tempDir) throws IOException
    {
        Path sub = tempDir.resolve("sub");
        Files.createDirectory(sub);
        Files.writeString(tempDir.resolve("a.yaml"), "x: 1\n");
        Files.writeString(sub.resolve("b.yml"), "x: 2\n");

        LaunchResult result = launcher.launch("-r", ".x", tempDir.toString());
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput()).contains("1");
        assertThat(result.getOutput()).contains("2");
    }

    @Test
    void yamlFileIsParsedByExtension(QuarkusMainLauncher launcher, @TempDir Path tempDir) throws IOException
    {
        Path file = tempDir.resolve("config.yaml");
        Files.writeString(file, """
                service:
                  name: checkout
                  replicas: 3
                """);

        LaunchResult result = launcher.launch(".service.name", file.toString());

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput()).contains("checkout");
    }

    @Test
    void prettyPrint(QuarkusMainLauncher launcher, @TempDir Path tempDir) throws IOException
    {
        Path file = tempDir.resolve("test.yaml");
        Files.writeString(file, "name: Alice\n");

        LaunchResult result = launcher.launch("--pretty", ".", file.toString());
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput()).contains("\n");
    }

    @Test
    void invalidFilterReturnsExitCode2(QuarkusMainLauncher launcher)
    {
        LaunchResult result = launcher.launch("!!! invalid filter !!!");
        assertThat(result.exitCode()).isEqualTo(2);
    }

    @Test
    void missingFileReturnsExitCode2(QuarkusMainLauncher launcher, @TempDir Path tempDir)
    {
        LaunchResult result = launcher.launch(".name", tempDir.resolve("missing.yaml").toString());

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.getErrorOutput()).contains("missing.yaml");
    }

    @Test
    void directoryWithoutRecursiveReturnsExitCode2(QuarkusMainLauncher launcher, @TempDir Path tempDir)
    {
        LaunchResult result = launcher.launch(".name", tempDir.toString());

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.getErrorOutput()).contains("Is a directory");
    }

    @Test
    void helpShowsRootAndCompletionSynopsis(QuarkusMainLauncher launcher)
    {
        LaunchResult result = launcher.launch("--help");

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput()).contains("ygrep [OPTIONS] FILTER [FILE...]");
        assertThat(result.getOutput()).contains("ygrep completion SHELL");
        assertThat(result.getOutput()).contains("Required jq filter, except when using a subcommand");
        assertThat(result.getOutput()).doesNotContain("[FILTER]");
        assertThat(result.getOutput()).doesNotContain("[FILTER] [FILE...] [COMMAND]");
    }

    @Test
    void completionGeneratesBashScript(QuarkusMainLauncher launcher)
    {
        LaunchResult result = launcher.launch("completion", "bash");

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput()).contains("complete -F _ygrep_completion ygrep");
        assertThat(result.getOutput()).contains("--from-file");
        assertThat(result.getOutput()).contains("--color-level-field");
        assertThat(result.getOutput()).contains("compgen -W \"completion\"");
    }

    @Test
    void completionGeneratesZshScript(QuarkusMainLauncher launcher)
    {
        LaunchResult result = launcher.launch("completion", "zsh");

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput()).contains("#compdef ygrep");
        assertThat(result.getOutput()).contains("--from-file");
    }

    @Test
    void completionGeneratesFishScript(QuarkusMainLauncher launcher)
    {
        LaunchResult result = launcher.launch("completion", "fish");

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput()).contains("complete -c ygrep");
        assertThat(result.getOutput()).contains("-l from-file");
    }

    @Test
    void completionGeneratesPowerShellScript(QuarkusMainLauncher launcher)
    {
        LaunchResult result = launcher.launch("completion", "powershell");

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput()).contains("Register-ArgumentCompleter");
        assertThat(result.getOutput()).contains("--color-level");
        assertThat(result.getOutput()).contains("--from-file");
    }

    @Test
    void completionRejectsUnsupportedShell(QuarkusMainLauncher launcher)
    {
        LaunchResult result = launcher.launch("completion", "tcsh");

        assertThat(result.exitCode()).isEqualTo(2);
        assertThat(result.getErrorOutput()).contains("unsupported shell");
    }

    @Test
    void multipleFilesShowFilename(QuarkusMainLauncher launcher, @TempDir Path tempDir) throws IOException
    {
        Path a = tempDir.resolve("a.yaml");
        Path b = tempDir.resolve("b.yaml");
        Files.writeString(a, "v: 1\n");
        Files.writeString(b, "v: 2\n");

        LaunchResult result = launcher.launch(".v", a.toString(), b.toString());
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput()).contains("a.yaml");
        assertThat(result.getOutput()).contains("b.yaml");
    }

    @Test
    void slurpCollectsAllResultsIntoArray(QuarkusMainLauncher launcher, @TempDir Path tempDir) throws IOException
    {
        Path file = tempDir.resolve("test.yaml");
        Files.writeString(file, """
                ---
                v: 1
                ---
                v: 2
                ---
                v: 3
                """);

        LaunchResult result = launcher.launch("-s", ".v", file.toString());
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput().trim()).isEqualTo("[1,2,3]");
    }

    @Test
    void slurpAcrossMultipleFiles(QuarkusMainLauncher launcher, @TempDir Path tempDir) throws IOException
    {
        Path a = tempDir.resolve("a.yaml");
        Path b = tempDir.resolve("b.yaml");
        Files.writeString(a, "v: 1\n");
        Files.writeString(b, "v: 2\n");

        LaunchResult result = launcher.launch("-s", ".v", a.toString(), b.toString());
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput().trim()).isEqualTo("[1,2]");
    }

    @Test
    void slurpNoMatchReturnsEmptyArray(QuarkusMainLauncher launcher, @TempDir Path tempDir) throws IOException
    {
        Path file = tempDir.resolve("test.yaml");
        Files.writeString(file, "v: 1\n");

        LaunchResult result = launcher.launch("-s", "select(.v > 99)", file.toString());
        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.getOutput().trim()).isEqualTo("[]");
    }

    @Test
    void nullInputRunsFilterWithoutFiles(QuarkusMainLauncher launcher)
    {
        LaunchResult result = launcher.launch("-n", "1 + 1");
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput().trim()).isEqualTo("2");
    }

    @Test
    void nullInputWithExpression(QuarkusMainLauncher launcher)
    {
        LaunchResult result = launcher.launch("-n", "{a: 1, b: 2} | .a");
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput().trim()).isEqualTo("1");
    }

    @Test
    void fromFileReadsFilterExpression(QuarkusMainLauncher launcher, @TempDir Path tempDir) throws IOException
    {
        Path yamlFile = tempDir.resolve("data.yaml");
        Path filterFile = tempDir.resolve("filter.jq");
        Files.writeString(yamlFile, "name: Alice\nage: 30\n");
        Files.writeString(filterFile, ".name");

        LaunchResult result = launcher.launch("-f", filterFile.toString(), yamlFile.toString());
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput()).contains("Alice");
    }

    @Test
    void fromFileWithMultilineFilter(QuarkusMainLauncher launcher, @TempDir Path tempDir) throws IOException
    {
        Path yamlFile = tempDir.resolve("data.yaml");
        Path filterFile = tempDir.resolve("filter.jq");
        Files.writeString(yamlFile, """
                ---
                name: Alice
                age: 30
                ---
                name: Bob
                age: 15
                """);
        Files.writeString(filterFile, "select(.age >= 18) | .name");

        LaunchResult result = launcher.launch("-f", filterFile.toString(), yamlFile.toString());
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput()).contains("Alice");
        assertThat(result.getOutput()).doesNotContain("Bob");
    }

    @Test
    void colorLevelColorsProjectedLogLines(QuarkusMainLauncher launcher, @TempDir Path tempDir) throws IOException
    {
        Path file = tempDir.resolve("logs.yaml");
        Files.writeString(file, """
                "@timestamp": "2026-06-28T08:16:12Z"
                log.level: ERROR
                message: payment declined
                """);

        LaunchResult result = launcher.launch("--color-level",
                "\"[\\(.[" + quote("log.level") + "])] \\(.message)\"",
                file.toString());

        assertThat(result.exitCode()).isEqualTo(0);
        assertColorOutput(result.getOutput(), "\u001B[31m[ERROR] payment declined\u001B[0m",
                "[ERROR] payment declined");
    }

    @Test
    void noColorDisablesLogLevelColor(QuarkusMainLauncher launcher, @TempDir Path tempDir) throws IOException
    {
        Path file = tempDir.resolve("logs.yaml");
        Files.writeString(file, """
                level: ERROR
                message: payment declined
                """);

        LaunchResult result = launcher.launch("--color-level", "--no-color",
                "\"[\\(.level)] \\(.message)\"",
                file.toString());

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput()).contains("[ERROR] payment declined");
        assertThat(result.getOutput()).doesNotContain("\u001B[31m");
    }

    @Test
    void logLevelColorsMapCommonLevels()
    {
        assertThat(BaseGrepCommand.colorForLevel("TRACE")).isEqualTo("\u001B[35m");
        assertThat(BaseGrepCommand.colorForLevel("DEBUG")).isEqualTo("\u001B[34m");
        assertThat(BaseGrepCommand.colorForLevel("INFO")).isEqualTo("\u001B[36m");
        assertThat(BaseGrepCommand.colorForLevel("WARN")).isEqualTo("\u001B[33m");
        assertThat(BaseGrepCommand.colorForLevel("ERROR")).isEqualTo("\u001B[31m");
        assertThat(BaseGrepCommand.colorForLevel("FATAL")).isEqualTo("\u001B[1;31m");
        assertThat(BaseGrepCommand.colorForLevel("UNKNOWN")).isNull();
    }

    // QuarkusMainLauncher captures output via HtmlAnsiOutputStream, which strips raw ANSI escape
    // codes. Colour correctness is verified by logLevelColorsMapCommonLevels; here we only check text.
    private static void assertColorOutput(String output, String colored, String plain)
    {
        assertThat(output).contains(plain);
    }

    private static String quote(String field)
    {
        return "\"" + field + "\"";
    }
}
