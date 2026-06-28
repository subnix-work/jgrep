package de.subnix.jgrep;

import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusMainTest
class JGrepCommandTest
{
    @Test
    void extractField(QuarkusMainLauncher launcher, @TempDir Path tempDir) throws IOException
    {
        Path file = tempDir.resolve("test.json");
        Files.writeString(file, "{\"name\": \"Alice\", \"age\": 30}");

        LaunchResult result = launcher.launch(".name", file.toString());
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput()).contains("Alice");
    }

    @Test
    void selectMatchingDocuments(QuarkusMainLauncher launcher, @TempDir Path tempDir) throws IOException
    {
        Path file = tempDir.resolve("test.json");
        Files.writeString(file, """
                {"name": "Alice", "age": 30}
                {"name": "Bob", "age": 15}
                """);

        LaunchResult result = launcher.launch("select(.age >= 18)", file.toString());
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput()).contains("Alice");
        assertThat(result.getOutput()).doesNotContain("Bob");
    }

    @Test
    void noMatchReturnsExitCode1(QuarkusMainLauncher launcher, @TempDir Path tempDir) throws IOException
    {
        Path file = tempDir.resolve("test.json");
        Files.writeString(file, "{\"name\": \"Alice\"}");

        LaunchResult result = launcher.launch("select(.age > 100)", file.toString());
        assertThat(result.exitCode()).isEqualTo(1);
    }

    @Test
    void countFlag(QuarkusMainLauncher launcher, @TempDir Path tempDir) throws IOException
    {
        Path file = tempDir.resolve("test.json");
        Files.writeString(file, """
                {"active": true}
                {"active": false}
                {"active": true}
                """);

        LaunchResult result = launcher.launch("-c", "select(.active == true)", file.toString());
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput().trim()).isEqualTo("2");
    }

    @Test
    void filesWithMatchesFlag(QuarkusMainLauncher launcher, @TempDir Path tempDir) throws IOException
    {
        Path matchFile = tempDir.resolve("match.json");
        Path noMatchFile = tempDir.resolve("nomatch.json");
        Files.writeString(matchFile, "{\"active\": true}");
        Files.writeString(noMatchFile, "{\"active\": false}");

        LaunchResult result = launcher.launch("-l", "select(.active == true)",
                matchFile.toString(), noMatchFile.toString());
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput()).contains("match.json");
        assertThat(result.getOutput()).doesNotContain("nomatch.json");
    }

    @Test
    void recursiveSearch(QuarkusMainLauncher launcher, @TempDir Path tempDir) throws IOException
    {
        Path sub = tempDir.resolve("sub");
        Files.createDirectory(sub);
        Files.writeString(tempDir.resolve("a.json"), "{\"x\": 1}");
        Files.writeString(sub.resolve("b.json"), "{\"x\": 2}");

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
    void yamlFlagForcesYamlParsing(QuarkusMainLauncher launcher, @TempDir Path tempDir) throws IOException
    {
        Path file = tempDir.resolve("config.txt");
        Files.writeString(file, """
                service:
                  name: checkout
                """);

        LaunchResult result = launcher.launch("--yaml", ".service.name", file.toString());

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput()).contains("checkout");
    }

    @Test
    void recursiveSearchIncludesYamlFiles(QuarkusMainLauncher launcher, @TempDir Path tempDir) throws IOException
    {
        Path sub = tempDir.resolve("sub");
        Files.createDirectory(sub);
        Files.writeString(tempDir.resolve("a.json"), "{\"x\": 1}");
        Files.writeString(sub.resolve("b.yaml"), "x: 2\n");

        LaunchResult result = launcher.launch("-r", ".x", tempDir.toString());

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput()).contains("1");
        assertThat(result.getOutput()).contains("2");
    }

    @Test
    void prettyPrint(QuarkusMainLauncher launcher, @TempDir Path tempDir) throws IOException
    {
        Path file = tempDir.resolve("test.json");
        Files.writeString(file, "{\"name\": \"Alice\"}");

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
    void helpShowsRootAndCompletionSynopsis(QuarkusMainLauncher launcher)
    {
        LaunchResult result = launcher.launch("--help");

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput()).contains("jgrep [OPTIONS] FILTER [FILE...]");
        assertThat(result.getOutput()).contains("jgrep completion SHELL");
        assertThat(result.getOutput()).doesNotContain("[FILTER] [FILE...] [COMMAND]");
    }

    @Test
    void completionGeneratesBashScript(QuarkusMainLauncher launcher)
    {
        LaunchResult result = launcher.launch("completion", "bash");

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput()).contains("complete -F _jgrep_completion jgrep");
        assertThat(result.getOutput()).contains("--yaml");
        assertThat(result.getOutput()).contains("--from-file");
        assertThat(result.getOutput()).contains("--color-level-field");
    }

    @Test
    void completionGeneratesZshScript(QuarkusMainLauncher launcher)
    {
        LaunchResult result = launcher.launch("completion", "zsh");

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput()).contains("#compdef jgrep");
        assertThat(result.getOutput()).contains("--yaml[Read input as YAML]");
        assertThat(result.getOutput()).contains("--from-file");
    }

    @Test
    void completionGeneratesFishScript(QuarkusMainLauncher launcher)
    {
        LaunchResult result = launcher.launch("completion", "fish");

        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput()).contains("complete -c jgrep");
        assertThat(result.getOutput()).contains("-l yaml");
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
        Path a = tempDir.resolve("a.json");
        Path b = tempDir.resolve("b.json");
        Files.writeString(a, "{\"v\": 1}");
        Files.writeString(b, "{\"v\": 2}");

        LaunchResult result = launcher.launch(".v", a.toString(), b.toString());
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput()).contains("a.json");
        assertThat(result.getOutput()).contains("b.json");
    }

    @Test
    void slurpCollectsAllResultsIntoArray(QuarkusMainLauncher launcher, @TempDir Path tempDir) throws IOException
    {
        Path file = tempDir.resolve("test.json");
        Files.writeString(file, """
                {"v": 1}
                {"v": 2}
                {"v": 3}
                """);

        LaunchResult result = launcher.launch("-s", ".v", file.toString());
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput().trim()).isEqualTo("[1,2,3]");
    }

    @Test
    void slurpAcrossMultipleFiles(QuarkusMainLauncher launcher, @TempDir Path tempDir) throws IOException
    {
        Path a = tempDir.resolve("a.json");
        Path b = tempDir.resolve("b.json");
        Files.writeString(a, "{\"v\": 1}");
        Files.writeString(b, "{\"v\": 2}");

        LaunchResult result = launcher.launch("-s", ".v", a.toString(), b.toString());
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput().trim()).isEqualTo("[1,2]");
    }

    @Test
    void slurpNoMatchReturnsEmptyArray(QuarkusMainLauncher launcher, @TempDir Path tempDir) throws IOException
    {
        Path file = tempDir.resolve("test.json");
        Files.writeString(file, "{\"v\": 1}");

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
        Path jsonFile = tempDir.resolve("data.json");
        Path filterFile = tempDir.resolve("filter.jq");
        Files.writeString(jsonFile, "{\"name\": \"Alice\", \"age\": 30}");
        Files.writeString(filterFile, ".name");

        LaunchResult result = launcher.launch("-f", filterFile.toString(), jsonFile.toString());
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput()).contains("Alice");
    }

    @Test
    void fromFileWithMultilineFilter(QuarkusMainLauncher launcher, @TempDir Path tempDir) throws IOException
    {
        Path jsonFile = tempDir.resolve("data.json");
        Path filterFile = tempDir.resolve("filter.jq");
        Files.writeString(jsonFile, """
                {"name": "Alice", "age": 30}
                {"name": "Bob", "age": 15}
                """);
        Files.writeString(filterFile, "select(.age >= 18) | .name");

        LaunchResult result = launcher.launch("-f", filterFile.toString(), jsonFile.toString());
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput()).contains("Alice");
        assertThat(result.getOutput()).doesNotContain("Bob");
    }

    @Test
    void parseErrorIsReportedButProcessingContinues(QuarkusMainLauncher launcher, @TempDir Path tempDir) throws IOException
    {
        Path file = tempDir.resolve("mixed.json");
        // First doc is valid; second is broken; overall run still exits 0 (matched first doc)
        Files.writeString(file, """
                {"valid": true}
                {invalid json}
                """);

        LaunchResult result = launcher.launch(".", file.toString());
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.getOutput()).contains("true");
    }

    @Test
    void colorLevelColorsProjectedLogLines(QuarkusMainLauncher launcher, @TempDir Path tempDir) throws IOException
    {
        Path file = tempDir.resolve("logs.ndjson");
        Files.writeString(file, """
                {"@timestamp":"2026-06-28T08:16:12Z","log.level":"ERROR","message":"payment declined"}
                """);

        LaunchResult result = launcher.launch("--color-level",
                "\"[\\(.[" + quote("log.level") + "])] \\(.message)\"",
                file.toString());

        assertThat(result.exitCode()).isEqualTo(0);
        assertColorOutput(result.getOutput(), "\u001B[31m[ERROR] payment declined\u001B[0m",
                "[ERROR] payment declined");
    }

    @Test
    void colorLevelCanUseCustomNestedField(QuarkusMainLauncher launcher, @TempDir Path tempDir) throws IOException
    {
        Path file = tempDir.resolve("logs.ndjson");
        Files.writeString(file, """
                {"app":{"level":"WARN"},"message":"slow downstream response"}
                """);

        LaunchResult result = launcher.launch("--color-level", "--color-level-field", "app.level",
                "\"[\\(.app.level)] \\(.message)\"",
                file.toString());

        assertThat(result.exitCode()).isEqualTo(0);
        assertColorOutput(result.getOutput(), "\u001B[33m[WARN] slow downstream response\u001B[0m",
                "[WARN] slow downstream response");
    }

    @Test
    void noColorDisablesLogLevelColor(QuarkusMainLauncher launcher, @TempDir Path tempDir) throws IOException
    {
        Path file = tempDir.resolve("logs.ndjson");
        Files.writeString(file, """
                {"level":"ERROR","message":"payment declined"}
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
        assertThat(JGrepCommand.colorForLevel("TRACE")).isEqualTo("\u001B[35m");
        assertThat(JGrepCommand.colorForLevel("DEBUG")).isEqualTo("\u001B[34m");
        assertThat(JGrepCommand.colorForLevel("INFO")).isEqualTo("\u001B[36m");
        assertThat(JGrepCommand.colorForLevel("WARN")).isEqualTo("\u001B[33m");
        assertThat(JGrepCommand.colorForLevel("ERROR")).isEqualTo("\u001B[31m");
        assertThat(JGrepCommand.colorForLevel("FATAL")).isEqualTo("\u001B[1;31m");
        assertThat(JGrepCommand.colorForLevel("UNKNOWN")).isNull();
    }

    private static void assertColorOutput(String output, String colored, String plain)
    {
        if (System.getenv("NO_COLOR") == null)
        {
            assertThat(output).contains(colored);
        }
        else
        {
            assertThat(output).contains(plain);
            assertThat(output).doesNotContain("\u001B[");
        }
    }

    private static String quote(String field)
    {
        return "\"" + field + "\"";
    }
}
