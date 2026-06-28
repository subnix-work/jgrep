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
}
