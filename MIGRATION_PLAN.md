# Migration Plan: banana-figlet → Kotlin / KMP / Gradle

Execution companion to [`CODE_REVIEW.md`](CODE_REVIEW.md) and
[`TEST_REVIEW.md`](TEST_REVIEW.md). Converts every review finding into
a **failing** Kotlin test (TDD), drives 100 % Kotlin test coverage on
JVM, then migrates production code to **pure Kotlin Multiplatform
`commonMain`**. Legacy Java sources remain untouched as a reference
implementation. CI/CD runs in dual mode: `java-legacy` (Maven/JUnit 4)
and `kmp` (Gradle/KMP).

---

## 0. Guiding principles

1. **Legacy Java is frozen.** `src/main/java/io/leego/banana/**` is
   read-only; no edits. It serves as the reference renderer for
   differential tests and remains publishable on the legacy release
   channel.
2. **Strict TDD.** Every `CODE_REVIEW.md` finding is encoded as a
   failing Kotlin test **before** any Kotlin implementation lands. The
   legacy Java code is never patched.
3. **Differential parity gate.** For every font in the catalogue and
   every representative input, the new Kotlin renderer's output must
   be byte-identical to the legacy Java renderer's output (minus the
   exact bugs `CODE_REVIEW.md` is explicitly fixing — those get
   divergence tests).
4. **100 % Kotlin coverage before pure-KMP migration.** A format /
   subsystem only moves from Java-delegate to `commonMain` once Kover
   reports 100 % line + branch coverage.
5. **Dual CI/CD.** Two pipelines on every PR:
   - `java-legacy` — Maven, JDK 8, JUnit 4.13.1 suite.
   - `kmp` — Gradle, JDK 21, KMP matrix (JVM / JS / Native / wasmJs).

---

## 1. Repository layout after migration

```
banana-figlet/
├── src/main/java/io/leego/banana/**        ← legacy Java (frozen)
├── src/test/java/io/leego/banana/**        ← legacy JUnit 4 suite
├── pom.xml                                  ← legacy Maven build (kept)
├── settings.gradle.kts                      ← new KMP root
├── build.gradle.kts
├── kmp/
│   ├── core/                                ← library
│   │   ├── src/commonMain/kotlin/…          ← pure parser + renderer
│   │   ├── src/commonTest/kotlin/…
│   │   ├── src/jvmMain/kotlin/…             ← File I/O, ZipInputStream
│   │   ├── src/jvmTest/kotlin/…             ← legacy-delegate tests
│   │   ├── src/jsMain/kotlin/…              ← JS File API / fetch
│   │   ├── src/wasmJsMain/kotlin/…
│   │   └── src/nativeMain/kotlin/…
│   ├── cli/                                 ← Kotlin CLI (replacement for Java examples)
│   ├── ui-compose-desktop/                  ← Compose Desktop host
│   ├── ui-compose-html/                     ← Compose for Web
│   └── ui-shared/                           ← shared Compose UI
├── testdata/                                ← .flf + .tlf corpus
├── .github/workflows/
│   ├── java-legacy.yml
│   └── kmp.yml
```

Maven remains authoritative for the Java-legacy build; Gradle is
authoritative for KMP.

---

## 2. Toolchain

| Concern | Choice |
| --- | --- |
| Build          | Gradle 8.x Kotlin DSL, version catalog `gradle/libs.versions.toml` |
| Kotlin         | 2.x K2, multiplatform plugin with JVM / JS / wasmJs / Native |
| Test framework | `kotlin.test` (common) + JUnit 5 Jupiter (JVM) + Kotest property-based |
| Coverage       | Kover 0.8.x, `minBound = 100` on line + branch for `commonMain` |
| Mutation       | Pitest (JVM), quality gate 85 % |
| Property tests | Kotest `property` for smushing-rule fuzzing |
| Fuzz           | Jazzer on the `.flf` and `.tlf` parsers, seeded from `testdata/` |
| Static         | Detekt + ktlint |
| UI (Desktop)   | Compose Multiplatform Desktop |
| UI (Web)       | Compose for Web (wasmJs preferred, js fallback) |
| Load / perf    | `kotlinx-benchmark` + JMH, regression gate ±10 % |
| CI             | GitHub Actions matrix: `java-legacy` + `kmp` on ubuntu/macos/windows |

---

## 3. Test strategy — every level

| Level | Source set | Runner | Purpose |
| --- | --- | --- | --- |
| Unit | `commonTest` / `jvmTest` | `kotlin.test` | Per-function parser + smushing rule logic |
| Integration | `jvmTest` | JUnit 5 | `bananaify` end-to-end against every font in `testdata/` |
| UI (Desktop) | `ui-compose-desktop:jvmTest` | Compose UI test | Renders a figlet in a Compose window, asserts DOM |
| UI (Web) | `ui-compose-html:wasmJsTest` | Compose Web test | Renders figlet in browser, asserts node tree |
| API (contract) | `jvmTest` / `jsTest` | JUnit 5 | Public `BananaUtils` surface stability |
| E2E | `:e2e` | JUnit 5 | KMP CLI runs side-by-side with Java CLI, diff outputs |
| Load / perf | `:benchmarks` | `kotlinx-benchmark` | Throughput: N chars × M fonts; regression gate |
| Fuzz | `jvmTest` | Jazzer | Random `.flf` / `.tlf` headers and bodies |

---

## 4. TDD test plan — one failing Kotlin test per `CODE_REVIEW.md` finding

Each test is first authored red against a JVM delegate that calls the
legacy Java code; it will fail because the bug still exists. After the
Kotlin re-implementation lands, the test goes green.

### 4.1 Critical (must fail red first)

| Finding | Failing Kotlin test |
| --- | --- |
| 1. `header[0].substring(5, 6)` OOB | `FlfHeaderParserTest.`​`header_shorter_than_6_chars_throws_InvalidFontException_not_SIOOBE()` |
| 2. `figlet[i]` NPE for truncated glyphs | `FigletRenderTest.`​`truncated_glyph_file_reports_parse_error_not_NPE()` |
| 3. Endmark-only row crashes | `GlyphParserTest.`​`row_of_only_endmarks_parses_as_empty_string()` |
| 4. `smushVerticalFigletLines` empty | `VerticalSmushTest.`​`empty_figlet_input_returns_empty_not_AIOOBE()` |
| 5. `getVerticalSmushDist` OOB | `VerticalSmushTest.`​`curDist_greater_than_len1_returns_valid_distance()` |

### 4.2 Major

| Finding | Failing test |
| --- | --- |
| 6. Unwrapped `NumberFormatException` | `FlfHeaderParserTest.non_numeric_header_field_throws_InvalidFontException()` |
| 7. Unchecked `header.length` | `FlfHeaderParserTest.short_header_token_list_throws_InvalidFontException()` |
| 8. Leaked `ZipInputStream` | `TlfLoaderResourceTest.`​`loading_many_tlf_does_not_leak_fds()` (JVM-only) |
| 9. `getHorizontalSmushLength` silent `substr` mask | `HorizontalSmushTest.`​`substr_len1_minus_curDist_is_valid_when_curDist_le_len1()` |
| 10. Mutable cached `Option` | `OptionImmutabilityTest.`​`mutating_user_option_does_not_affect_cached_meta()` |

### 4.3 Minor + Nit

- `PaddingTest.padLines_allocates_once_per_line()` — verified via
  JMH allocation counter.
- `GenerateFigletLineTest.integer_max_value_sentinel_replaced_by_optional()`
  — API surface check.
- `RuleParseTest.mixed_layout_rule_precedence_matches_figlet_spec()`
  — pins current behaviour.

### 4.4 Test sequencing

For each subsystem `S` (HeaderParse, GlyphParse, HorizSmush, VertSmush,
TlfLoad):

1. **JVM delegate** in `jvmMain`: `class ${S}JavaAdapter` wraps the
   legacy `BananaUtils.*` method.
2. **Red tests** from §4.1–4.3 written against the adapter. CI must
   show them red on the adapter-only commit.
3. **Pinning positive tests** against the adapter for current correct
   behaviour (port every non-doc test from `BananaUtilsTests.java`).
4. **`commonMain` implementation** of `S` in Kotlin. Both suites must
   pass.
5. **Differential suite**: `testdata/` × every `Font` × `Hello, World!`
   rendered by both Java and Kotlin. Output must be byte-equal
   (except for the known divergences introduced by the fix).
6. Remove the JVM delegate for `S`. Kotlin `commonMain` is now
   authoritative.

### 4.5 Test corpus

- `testdata/flf/good/*.flf` — every font currently shipped in the
  classpath `/flf/` directory, plus `standard.flf` pinned explicitly.
- `testdata/flf/bad/*.flf` — 5-char header, non-numeric numeric
  field, truncated glyph, empty glyph row, endmark-only row.
- `testdata/tlf/good/*.tlf` — at least 3 Toilet fonts to pin the
  uncovered `.tlf` path flagged in `TEST_REVIEW.md`.
- `testdata/tlf/bad/*.tlf` — empty zip, zip with no entries,
  oversized entry.
- `testdata/texts/*.txt` — `Hello, World!`, the multi-line sample
  from `BananaUtilsTests`, a 2 KiB stress sample, a unicode-only
  sample, empty string, whitespace-only, newline-only.
- `testdata/gen/` — Kotlin scripts that generate the bad fonts
  deterministically.

---

## 5. 100 % Kotlin coverage gate

- `koverVerify { rule { bound { minValue = 100; metric = LINE };
   bound { minValue = 100; metric = BRANCH } } }` on `commonMain`.
- Pitest mutation ≥ 85 % on `commonMain` + `jvmMain`.
- **No coverage exceptions**; if code cannot be covered in common
  source it must move to platform-specific source sets with their
  own 100 % gate.
- A subsystem only migrates to pure `commonMain` (and its JVM
  delegate deleted) when the gate is satisfied on that subsystem.

---

## 6. Migration phases

### Phase A — Gradle + CI scaffolding (1 PR)

- Add Gradle KMP root alongside existing `pom.xml`; both builds
  coexist.
- Commit `.github/workflows/java-legacy.yml` (Maven) and
  `kmp.yml` (Gradle).
- Gate: both lanes green on an empty Kotlin module.

### Phase B — Legacy-delegate + corpus (1 PR)

- `jvmMain` adapters that delegate every `BananaUtils` entry point
  to the legacy Java class.
- Commit `testdata/` corpus (good + bad).
- Port every **assertion-bearing** test from `BananaUtilsTests.java`
  to Kotlin, running against the Java delegate.
- **Do not port** the doc-generating tests
  (`testGenerateFontDocs`, `testGenerateLayoutDocs`) — relocate them
  to a Gradle `generateDocs` task.
- Gate: all positive Kotlin tests green against Java delegate.

### Phase C — TDD-fix subsystems one at a time

Priority order: **HeaderParse → GlyphParse → HorizSmush → VertSmush →
TlfLoad → OptionImmutability**.

Each subsystem is one PR that:

1. Adds the **red** negative tests (they fail against the Java
   delegate).
2. Adds the pure-Kotlin `commonMain` implementation.
3. Wires the negative tests through the Kotlin implementation
   (now green).
4. Runs the differential suite against the corpus — must be
   byte-exact except for the specific divergence the fix
   introduces.
5. Raises Kover to 100 % on that subsystem.
6. Deletes the Java delegate for that subsystem.

### Phase D — UI migration

- `ui-compose-desktop/` — Compose Desktop app exposing
  `BananaUtils.bananaify` and `bananansi` with a live text field
  and font dropdown.
- `ui-shared/` — `FigletPreview` composable.
- `ui-compose-html/` — Compose for Web wasmJs target hosting
  `FigletPreview`.
- UI tests:
  - Desktop: Compose UI test asserts the rendered string after
    font-change.
  - Web: Compose Web renderer tests assert `<pre>` text content;
    Playwright Kotlin E2E test drives the published static site.

### Phase E — E2E + load/perf

- `e2e-tests` module: a Gradle task builds the legacy Java CLI (from
  the legacy Maven build) and the new KMP CLI, runs both over
  `testdata/`, and diffs the output. Any divergence fails the build.
- `benchmarks/` JMH gates: throughput of `bananaify` across
  `Font.values()` for 1 KiB input, regression threshold ±10 %,
  baseline committed.

### Phase F — Dual CI/CD + release

- **`java-legacy` workflow** — `mvn -B verify`, publishes the classic
  JAR under `io.leego:banana-figlet-legacy` to Maven Central.
- **`kmp` workflow** — `./gradlew build kmpPublish`, publishes:
  - `banana-figlet-core-jvm` (Maven Central)
  - `banana-figlet-core-js` (npm)
  - `banana-figlet-core-wasmjs` (npm)
  - `banana-figlet-native-*` klibs (Maven Central)
  - `banana-figlet-desktop` signed bundle (Compose Desktop)
  - `banana-figlet-web` static site (GitHub Pages)
- Tag-driven release pipeline runs both lanes; legacy channel has
  a `-legacy` version suffix.

---

## 7. Acceptance criteria

1. Every `CODE_REVIEW.md` finding has a **named** Kotlin test whose
   first commit shows it red on the Java delegate and whose matching
   fix commit shows it green on Kotlin.
2. `./gradlew koverVerify` = 100 % line + branch on `commonMain`
   and every non-UI module.
3. `./gradlew pitestReport` ≥ 85 %.
4. Differential suite byte-exact parity with legacy Java for the
   committed corpus (minus documented intentional divergences).
5. `.tlf` support is covered by at least 3 pinned fonts in
   `testdata/tlf/good/`.
6. `java-legacy` lane still green on every PR; **no file under
   `src/main/java/**` modified since Phase A**.
7. `kmp` lane green on {ubuntu, macos, windows} × {jvm, js, wasmJs,
   native}.

---

## 8. Deliverables

- Gradle KMP root + version catalog + `libs.versions.toml`.
- `kmp/core/` with `commonMain` / `commonTest` / JVM / JS / wasmJs /
  Native source sets.
- `kmp/ui-compose-desktop/`, `kmp/ui-compose-html/`, `kmp/ui-shared/`
  Compose modules.
- `kmp/cli/` Kotlin CLI replacement.
- `testdata/` corpus + generator scripts.
- `.github/workflows/java-legacy.yml` and `kmp.yml`.
- `TEST_PLAN.md` mapping each `CODE_REVIEW.md` finding → Kotlin test
  FQN (auto-generated).
- `benchmarks/baselines/*.json`.
