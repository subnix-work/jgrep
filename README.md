# jgrep

`grep` for JSON — filter and search JSON files using [jq](https://jqlang.github.io/jq/) expressions.

```bash
# Extract a field
jgrep '.name' users.json

# Filter documents
jgrep 'select(.age >= 18)' users.json

# Search recursively, count matches
jgrep -rc 'select(.status == "active")' ./data/

# Multiple files, show filename
jgrep '.email' alice.json bob.json

# Pipe from stdin
curl -s https://api.example.com/users | jgrep 'select(.role == "admin")'
```

## Installation

Download the native binary for your platform from the [Releases](https://github.com/subnix-work/jgrep/releases) page — no JVM required.

**Or build from source:**

```bash
git clone https://github.com/subnix-work/jgrep.git
cd jgrep/jgrep

# JVM build (requires Java 21+)
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar '.name' file.json

# Native binary via Docker (no GraalVM needed locally)
./mvnw package -Pnative -Dquarkus.native.container-build=true
./target/jgrep-1.0.0-SNAPSHOT-runner '.name' file.json
```

## Usage

```
Usage: jgrep [-clh] [--pretty] [--no-color] [-V] FILTER [FILE...]

  FILTER   jq filter expression (e.g. '.name', 'select(.age > 18)', '.items[]')
  FILE     JSON files to search. Reads from stdin if omitted.

Options:
  -r, --recursive            Recurse into directories (searches *.json files)
  -l, --files-with-matches   Only print filenames that contain matches
  -c, --count                Print match count per file
      --pretty               Pretty-print JSON output
      --no-color             Disable colored output (also respects $NO_COLOR)
  -h, --help                 Show this help message
  -V, --version              Print version
```

## Examples

```bash
# All users older than 18
jgrep 'select(.age > 18)' users.ndjson

# Extract nested field
jgrep '.address.city' customers.json

# Find files containing errors
jgrep -l 'select(.level == "ERROR")' logs/*.json

# Count active items per file
jgrep -rc 'select(.active == true)' ./data/

# Array items matching condition
jgrep '.orders[] | select(.total > 100)' orders.json

# Pretty-print matching documents
jgrep --pretty 'select(.status == "pending")' jobs.json
```

## Filter syntax

jgrep uses full [jq 1.6](https://jqlang.github.io/jq/manual/) syntax. Supports NDJSON (newline-delimited JSON) natively — multiple JSON documents per file are each filtered independently.

**Exit codes** (same as `grep`):
- `0` — at least one match found
- `1` — no matches found
- `2` — error (invalid filter, file not found)

## Tech stack

- [Quarkus](https://quarkus.io/) + [Picocli](https://picocli.info/) — CLI framework
- [jackson-jq](https://github.com/eiiches/jackson-jq) — jq implementation in Java
- GraalVM Native Image via Docker — single binary, no JVM needed, ~8ms startup

## License

[MIT](LICENSE)
