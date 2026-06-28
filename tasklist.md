# jgrep — Bauplan mit Quarkus

## 1. Projektsetup
- [x] Quarkus-Projekt generieren (`quarkus create app`)
- [x] Extensions hinzufügen: `picocli`, `jackson`
- [x] `jackson-jq` Dependency in `pom.xml` eintragen (Apache 2.0)
- [x] GraalVM Native Image Extension konfiguriert (via `native`-Profil)

## 2. CLI-Interface (Picocli)
- [x] `@Command` Einstiegspunkt (`JGrepCommand`)
- [x] Argumente definieren: jq-Filter, Dateipfade, Flags (`-r`, `-l`, `-c`, ...)
- [x] stdin-Support (kein Dateipfad = lese von stdin)

## 3. JSON-Parsing & Matching
- [x] JSON einlesen mit Jackson (`MappingIterator<JsonNode>`, NDJSON-Support)
- [x] jq-Filter via `jackson-jq` auswerten (`.user.name`, `.items[]`, `select(...)`)
- [x] Ergebnisse formatieren (Dateiname, Treffer)

## 4. Features
- [x] Rekursive Dateisuche (`-r`)
- [x] Nur Dateinamen ausgeben (`-l`)
- [x] Treffer zählen (`-c`)
- [x] Farbige Ausgabe (ANSI, `NO_COLOR`-Standard, `--no-color`)
- [x] Pretty-print Treffer (`--pretty`)

## 5. Native Build
- [x] `application.properties` für Native konfiguriert (`--initialize-at-run-time`)
- [x] GraalVM Reflection-Hints für jackson-jq (`NativeConfiguration.java`)
- [x] `./mvnw package -Pnative -Dquarkus.native.container-build=true` — gebaut via Docker (18s, 44MB)

## 6. Tests
- [x] Unit-Tests für jq-Filter-Auswertung (6 Tests)
- [x] Integration-Tests mit echten JSON-Fixtures (9 Tests — `@QuarkusMainTest`)
- [x] Performance-Test: 10.000 Dokumente in 32ms (312.500 docs/sec)

## 7. Distribution
- [x] Native Binary gebaut: `target/jgrep-1.0.0-SNAPSHOT-runner` (44MB, 8ms Startup)
- [x] GitHub Release v1.0.0 mit Binary: https://github.com/subnix-work/jgrep/releases/tag/v1.0.0
- [x] LICENSE-Datei (Apache 2.0) angelegt
