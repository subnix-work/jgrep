# jgrep

`grep` for JSON and YAML — filter and search structured files using [jq](https://jqlang.github.io/jq/) expressions.

```bash
# Extract a field
jgrep '.name' users.json

# Filter documents
jgrep 'select(.age >= 18)' users.json

# Search recursively, count matches
jgrep -rc 'select(.status == "active")' ./data/

# Collect all matching values into a JSON array
jgrep -s '.name' users.ndjson

# Run a filter without any input file
jgrep -n '{version: "1.1.0", ok: true}'

# Read complex filter from a file
jgrep -f my-filter.jq events.ndjson

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
./target/jgrep-1.1.0-runner '.name' file.json
```

## Usage

```
Usage: jgrep [-clsnhV] [-f=FILE] [--pretty] [--yaml] [--color-level] [--color-level-field FIELD] [--no-color] [FILTER] [FILE...]

  FILTER   jq filter expression (e.g. '.name', 'select(.age > 18)', '.items[]').
           Required unless -f is used.
  FILE     JSON or YAML files to search. Reads from stdin if omitted.

Options:
  -r, --recursive            Recurse into directories (searches *.json, *.yaml, *.yml files)
  -l, --files-with-matches   Only print filenames that contain matches
  -c, --count                Print match count per file
  -s, --slurp                Collect all results into a single JSON array
  -n, --null-input           Use null as input (no file needed; evaluate filter directly)
  -f, --from-file=FILE       Read filter expression from a file
      --pretty               Pretty-print JSON output
      --yaml                 Read input as YAML (also auto-detected for *.yml and *.yaml files)
      --color-level          Color each output line by log level
      --color-level-field    Field used by --color-level (e.g. app.level)
      --no-color             Disable colored output (also respects $NO_COLOR)
  -h, --help                 Show this help message
  -V, --version              Print version

Subcommands:
  completion SHELL           Generate shell completion script
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

# Collect all names into a JSON array
jgrep -s '.name' users.ndjson

# Aggregate across multiple files
jgrep -s '.price' products/*.json | jgrep -n 'add / length'

# Evaluate an expression without input
jgrep -n 'now | strftime("%Y-%m-%d")'

# Use a multi-line filter stored in a file
jgrep -f filter.jq events.ndjson

# Combine slurp + pretty-print
jgrep -s --pretty 'select(.active)' users.ndjson

# Read YAML by extension
jgrep '.service.name' deployment.yaml

# Force YAML for stdin or non-yaml filenames
cat deployment.yaml | jgrep --yaml '.spec.template.spec.containers[].image'
```

### Shell completion

Generate completion scripts for common shells:

```bash
# Bash
jgrep completion bash > ~/.local/share/bash-completion/completions/jgrep

# Zsh
jgrep completion zsh > ~/.zsh/completions/_jgrep

# Fish
jgrep completion fish > ~/.config/fish/completions/jgrep.fish

# PowerShell
jgrep completion powershell >> $PROFILE
```

### Human-readable Kubernetes / ECS logs

Kubernetes and ECS logs are often NDJSON: one JSON object per line. You can filter structured fields and then project each match into plain text:

```bash
# Turn JSON log events into readable text lines
cat ecs.ndjson | jgrep \
  '"\(.["@timestamp"]) [\(.["log.level"])] \(.["service.name"])/\(.kubernetes.pod): \(.message)"'

# Filter specific objects, unpack nested tracking fields, and remove JSON braces
cat ecs.ndjson | jgrep \
  'select(.["log.level"] == "ERROR" and .kubernetes.namespace == "shop") |
   "\(.["@timestamp"]) ERROR \(.["service.name"]): \(.message) trace=\(.custom_tracker.trace_id)"'

# Color whole output lines by log level directly in jgrep
cat ecs.ndjson | jgrep --color-level \
  '"[\(.["log.level"])] \(.["@timestamp"]) \(.message) trace=\(.custom_tracker.trace_id)"'

# Use a custom level field
cat app.ndjson | jgrep --color-level --color-level-field app.level \
  '"[\(.app.level)] \(.message)"'
```

## Filter syntax

jgrep uses full [jq 1.6](https://jqlang.github.io/jq/manual/) syntax. Supports NDJSON (newline-delimited JSON) natively — multiple JSON documents per file are each filtered independently.

**Exit codes** (same as `grep`):
- `0` — at least one match found
- `1` — no matches found
- `2` — error (invalid filter, file not found)

## What's new in 1.1.0

- **`-s` / `--slurp`** — collect all matching results from all files into one JSON array
- **`-n` / `--null-input`** — evaluate a filter without reading any file (useful for jq expressions that generate data or combine results from pipes)
- **`-f` / `--from-file`** — load the filter expression from a `.jq` file instead of the command line; useful for complex multi-line filters
- **Syntax highlighting** — JSON output is colorized (keys, strings, numbers, booleans) when writing to a terminal; disable with `--no-color` or `$NO_COLOR`
- **Better error messages** — parse errors now include the line and column number

## Tech stack

- [Quarkus](https://quarkus.io/) + [Picocli](https://picocli.info/) — CLI framework
- [jackson-jq](https://github.com/eiiches/jackson-jq) — jq implementation in Java
- GraalVM Native Image via Docker — single binary, no JVM needed, ~8ms startup

## License

[Apache 2.0](LICENSE)
