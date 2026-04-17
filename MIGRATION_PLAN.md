# Migration Plan: banana-figlet → Kotlin / KMP / Gradle

Execution companion to [`CODE_REVIEW.md`](CODE_REVIEW.md) and
[`TEST_REVIEW.md`](TEST_REVIEW.md). Drives every review finding through
a strict **fix-then-freeze-then-port** cycle:

1. Author Kotlin TDD tests that fail against today's Java code.
2. **Fix the Java code** until every test is green and JVM coverage
   reaches 100 % line + branch.
3. **Freeze** the now-stable Java sources and tag them `legacy-v1`.
4. **Migrate** the frozen Java logic to pure Kotlin Multiplatform
   (`commonMain`), reusing the *same* Kotlin test suite — which now
   runs against both the frozen Java and the new Kotlin
   implementations as a differential parity gate.

Two CI/CD profiles (`java`, `kmp`) run on every PR throughout; both
apply ProGuard to release artefacts. See
[`INTEGRATION.md`](INTEGRATION.md) for the downstream
integration into the `fonts-bitsnpicas`-hosted font studio.

---

## 0. Guiding principles

1. **TDD, strictly.** Every finding in `CODE_REVIEW.md` becomes a red
   Kotlin test *before* any production code changes.
2. **Java is stabilised first, then frozen.** The existing Java is
   fixed to pass the Kotlin tests and reach 100 % coverage; every fix
   commit references its `CODE_REVIEW.md` finding ID. Only after
   stabilisation does the Java tree become read-only.
3. **Shared test suite across Java and Kotlin.** Tests target a
   platform-neutral `FigletIo` interface with two `actual`
   implementations — a JVM adapter over the frozen Java, and pure
   Kotlin in `commonMain`. The same `@Test` methods run against both.
4. **100 % coverage at every transition.** JVM (JaCoCo) reaches 100 %
   before freeze; Kotlin (Kover) must stay at 100 % before a
   subsystem migrates from Java-delegate to pure `commonMain`.
5. **Dual CI/CD profiles.** Gradle profiles `-Pprofile=java` and
   `-Pprofile=kmp`. Every PR runs both. Both apply ProGuard on
   release artefacts and verify the shrunk JAR.
6. **Reproducible toolchain via SDKMAN.** Contributors and CI consume
   the exact same JDK / Kotlin / Gradle / JBang versions declared in
   `.sdkmanrc`, pinned to the **latest stable** at migration time.

---

## 0.5 Roadmap stages (delivery checkpoints)

| # | Stage | What lands | UI / CLI state |
| --- | --- | --- | --- |
| S0 | **Baseline** (✅ complete) | Review docs, `.sdkmanrc`, Maven + Gradle dual build, `gradle/libs.versions.toml` Maven-Central-verified. | — |
| S1 | **Red-then-green Critical fixes** (✅ complete) | 5 Kotlin red-tests + 5 Java fixes (C1…C5). `mvn test` and `./gradlew test` both green (13 Java + 5 Kotlin = 18/18). | — |
| S2 | **Phase B coverage drive** (**next**) | JaCoCo 100 % line + branch on `io.leego.banana.**` (the whole library — no Option-C exclusions needed here). Remaining Majors / Minors from `CODE_REVIEW.md` closed. | Text-output parity suite lands: Kotlin tests under `src/test/kotlin/io/leego/banana/parity/**` pin today's Java `bananaify` and `bananansi` output against every bundled `.flf` / `.tlf`. |
| S3 | **Freeze** — tag `legacy-v1` | CODEOWNERS read-only on `src/main/java/**`; CI diff-check guard. | Parity hashes frozen. |
| S4 | **Phase D** — port to `commonMain` | Per-subsystem Kotlin port (HeaderParse → GlyphParse → HorizSmush → VertSmush → TlfLoad → OptionImmutability). Same Kotlin tests run against both frozen Java and new Kotlin. Kover 100 % + Pitest ≥ 85 % per module. | Parity suite runs twice per PR (Java + Kotlin); any divergence fails CI. |
| S5 | **Phase E** — Compose Desktop preview host | `ui-compose-desktop` hosts `FigletPreview` + live text field + font dropdown. `ComposeDesktopUiDriver` `actual` lands. | Green renderer joins the parity matrix. |
| S6 | **Phase E.2** — Compose for Web (wasmJs) | `ui-compose-html` host reusing `FigletPreview`. `ComposeWebUiDriver` `actual` lands. | Three renderers (text / Desktop / Web); same suite; ARGB / text-output parity both gated. |
| S7 | **Phase F** — Dual CI/CD release | `profile=java` → ProGuarded `banana-figlet-legacy` JAR. `profile=kmp` → klibs (JVM/JS/wasmJs/Native), Compose Desktop bundle, Web static site, GraalVM `native-image` CLI. `verifyProguardedJar` gates both. | — |

See [`../fonts-bitsnpicas/docs/diagrams/08-ui-driver-expect-actual.puml`](../fonts-bitsnpicas/docs/diagrams/08-ui-driver-expect-actual.puml)
for the UI-test driver architecture (host-wide; banana-figlet
plugs into the same `UiDriver` expect/actual once vendored into
`modules/figlet/`).

---

## 0.6 S4 port progress log

All ports land under `modules/core/` with commonMain + jvmMain
`JavaLegacyAdapter` + jvmTest differential-parity. Zero `java.*`
imports in commonMain. `./gradlew :modules:core:check` and
`mvn test` both green after every round.

| Round | SHA | Port | commonMain surface added | Module tests | Parity |
| --- | --- | --- | --- | --- | --- |
| R1 | `8bcfd1f` + `bae66a3` | `Layout` enum | `Layout` (5 values + `get`) | 18 | 10 fixture-exact |
| R2 | `7ab53dd` | `Option` + `Rule` — **fixes M10** (mutable cached state) | Immutable `data class Option` + `Rule` + `copy()` | 50 | 8 field-exact |
| R3 | `f115d7f` | `Meta` + `BananaUtils.buildMeta` | `Meta` data class, `FlfParser.parseFlfFont(List<String>)`, pure-string `.flf` header + glyph parser | 77 | 9 byte-exact (full Meta equality on Standard font) |
| R4 | `ab901b0` | `BananaUtils.generateFigletLine` + horizontal smush rules | `FigletRenderer.generateLine`, `SmushRules` with H1-H6, `smushUniversal`, `getHorizontalSmushLength`. **§17.5 `\\/ → Y` dead-branch fixed in commonMain** (explicit pair matching instead of `indexOf`) | 115 | 35 byte-exact + 3 pinned §17.5 divergences |
| R5 | `a76c437` | Vertical smushing — **completes the render engine** | `FigletRenderer.combineVertically` + `smushVerticalLines` + `canSmushVertical` + `getVerticalSmushDist` + `SmushRules.vRule1..5`. **C4** (empty-array guard) + **C5** (`curDist` cap at `min(figlet1.size, figlet2.size)`) carried natively. No vertical-rule divergences found — rules 1/2/4/5 are trivial pair tests; rule 3 reuses the horizontal-rule-3 hierarchy logic which is already correct (no §17.5-style dead code in vertical). | 146 | full (multi-line × 5 vertical × 3 horizontal) byte-exact |
| R6 | `45f023a` | **`bananaify` / `bananansi` top-level API — completes the library port** | `BananaFiglet` object (orchestrates FlfParser → FigletRenderer → combineVertically → join), `Ansi` enum + `ansify`, `expect/actual FontResourceLoader` (classpath on JVM), `BananaFigletJvm` convenience wrappers with `ConcurrentHashMap` font cache. §8.5 empty-input short-circuit carried. | 168 | byte-exact on 5 sample fonts (Standard, Banner, Big, Mini, Slant) × "Hello, World!" + single-char + multi-line. §17.5 divergence pinned explicitly for Slant multi-line. |

Fixes carried natively by the Kotlin ports:

- **C1** — FLF header short-token guard (`FlfParser`).
- **C2** — truncated-glyph parse error (`FlfParser`).
- **C3** — endmark-only row handled (`FlfParser`).
- **C4** — `FigletRenderer.combineVertically` empty-array guard.
- **C5** — `getVerticalSmushDist` cap at `min(figlet1.size, figlet2.size)`.
- **M10** — `Option` is immutable (`data class`, no setters).
- **§8.5** — already fixed in frozen Java pre-freeze.
- **§17.5** — `smushHorizontalRule5 \\/ → Y` dead branch fixed in
  commonMain (Kotlin uses explicit pair matching, Java still has
  the `indexOf` bug per the legacy-v1 contract).

### S4 completion status: **LOGIC PORT STRUCTURALLY COMPLETE** ✅

After R6 (`45f023a`), every public API surface of the library is
ported to `commonMain` pure Kotlin with differential-parity gates
against the frozen Java:

- **Parser**: `FlfParser.parseFlfFont(List<String>)` — full `.flf`
  header + glyph row parsing.
- **Renderer**: `FigletRenderer.generateLine` (horizontal smush) +
  `combineVertically` (vertical smush) — all 11 smush rules ported.
- **Public API**: `BananaFiglet.bananaify` / `bananansi` with font
  resource loading (`expect/actual FontResourceLoader`).
- **Model**: `Layout`, `Option` (immutable), `Rule` (immutable),
  `Meta`, `Ansi`.

**Remaining S4 follow-ups** (not blocking S5):

- TLF (Toilet font) zip-stream path — `Font.convertIfZipped`.
  The `entry == null` branch is unreachable from the current API
  path (documented in CODE_REVIEW.md §8 + FontCoverageTest).
- JS / wasmJs / Native target enablement — currently JVM-only;
  `expect fun loadFontResource` needs actuals per target.
- Kover gate tightening to strict 100 % on `commonMain`.

**Next milestone**: Stage S5 (Compose Desktop `UiDriver` actual).

---

## 1. Toolchain — SDKMAN + Gradle version catalog

Every number below was verified against `sdk list` or Maven Central
at commit time. The whole table is driven by two files:
`.sdkmanrc` (JDK / Kotlin / Gradle / JBang) and
`gradle/libs.versions.toml` (everything else).

| Concern | Choice (verified latest stable) |
| --- | --- |
| JDK           | Oracle GraalVM **25.0.2-graal** — both profiles |
| Kotlin        | **2.3.20** — K2, multiplatform plugin |
| Gradle        | **9.4.1** — Kotlin DSL + version catalog |
| JBang         | **0.138.0** |
| Test          | `kotlin.test` + JUnit **5.12.2** + Kotest **5.9.1** property |
| Coverage      | Kover **0.9.1** (KMP), JaCoCo (Java), 100 % line + branch |
| Mutation      | Pitest Gradle **1.15.0** / core **1.19.1**, ≥ 85 % |
| Static        | Detekt **1.23.8**, ktlint-gradle **12.3.0** |
| Fuzz          | Jazzer **0.24.0** |
| Bench         | `kotlinx-benchmark` runtime **0.4.14** + JMH |
| UI (Desktop)  | Compose Multiplatform **1.8.2** |
| UI (Web)      | Compose for Web (wasmJs), Compose **1.8.2** |
| Shrink        | ProGuard Gradle **7.7.0** + `verifyProguardedJar` |
| Native        | `org.graalvm.buildtools.native` **0.10.6** |
| CI            | GitHub Actions `{ubuntu,macos,windows}-latest` via `sdk env` |


### 1.1 `.sdkmanrc` (repo root)

```
# .sdkmanrc  — run `sdk env` in the repo root
# Pinned to the latest 2026 stable; bumped by a single renovate/dependabot PR.
java=25.0.2-graal            # Oracle GraalVM for JDK 25 LTS (SDKMAN `graal` distro; native-image + PGO for KMP native targets)
kotlin=2.3.20            # latest stable from `sdk list kotlin`
gradle=9.4.1             # latest stable from `sdk list gradle`
jbang=0.138.0            # latest stable from `sdk list jbang`
```

CI installs SDKMAN (`sdkman/sdkman-action@…`) and runs `sdk env` —
no toolchain versions are duplicated in workflow YAML.

### 1.2 `gradle/libs.versions.toml`

Latest-stable versions at migration time; bumped via Renovate/Dependabot
PRs.

Latest 2026 stable at adoption time; Renovate/Dependabot keep the
file fresh.

```toml
[versions]
# All versions below verified against Maven Central on the day of
# this commit. Renovate/Dependabot keep them fresh.
kotlin         = "2.3.20"    # matches `.sdkmanrc`
coroutines     = "1.10.2"
serialization  = "1.9.0"
kover          = "0.9.1"
pitestGradle   = "1.15.0"
pitestCore     = "1.19.1"
kotest         = "5.9.1"     # 6.x is milestones
jbang          = "0.138.0"   # matches `.sdkmanrc`
junit          = "5.12.2"    # 5.13.x is milestones
jazzer         = "0.24.0"
detekt         = "1.23.8"
ktlintGradle   = "12.3.0"
ktlintCore     = "1.6.0"
compose        = "1.8.2"     # 1.9.x is alpha
skiko          = "0.9.18"
benchmarks     = "0.4.14"
proguard       = "7.7.0"
graalvmPlugin  = "0.10.6"    # org.graalvm.buildtools.native plugin

[libraries]
kotlin-test         = { module = "org.jetbrains.kotlin:kotlin-test",         version.ref = "kotlin" }
kotlinx-coroutines  = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotest-property     = { module = "io.kotest:kotest-property",                version.ref = "kotest" }
junit-jupiter       = { module = "org.junit.jupiter:junit-jupiter",          version.ref = "junit" }
jazzer              = { module = "com.code-intelligence:jazzer-junit",       version.ref = "jazzer" }

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kover                = { id = "org.jetbrains.kotlinx.kover",        version.ref = "kover" }
pitest               = { id = "info.solidsoft.pitest",              version.ref = "pitest" }
detekt               = { id = "io.gitlab.arturbosch.detekt",        version.ref = "detekt" }
ktlint               = { id = "org.jlleitschuh.gradle.ktlint",      version.ref = "ktlint" }
compose              = { id = "org.jetbrains.compose",              version.ref = "compose" }
benchmarks           = { id = "org.jetbrains.kotlinx.benchmark",    version.ref = "benchmarks" }
proguard             = { id = "com.guardsquare.proguard",           version.ref = "proguard" }
graalvm-native       = { id = "org.graalvm.buildtools.native",      version.ref = "graalvm" }
```

### 1.3 JBang — also the CLI runtime

JBang (from `.sdkmanrc`) has **two** roles:

1. **Corpus generators** under `testdata/gen/*.kt` — build malformed
   `.flf` / `.tlf` fixtures (short header, non-numeric field, empty
   zip, …). `jbang testdata/gen/BadFlf.kt` — one command, zero
   project setup.

2. **CLI entry points** — `banana-figlet` ships a small CLI
   (`bananaify "Hello"`). It migrates to a **JBang script written
   in Kotlin using Clikt**, with `//DEPS` and `//SOURCES` (`//src`)
   directives:

   ```kotlin
   ///usr/bin/env jbang "$0" "$@" ; exit $?
   //KOTLIN 2.3.20
   //DEPS com.github.ajalt.clikt:clikt:5.0.1
   //DEPS io.leego:banana-figlet:<version>
   //SOURCES cli/Bananaify.kt

   fun main(args: Array<String>) = Bananaify().main(args)
   ```

   Users run `jbang banana.kt "Hello, World!"` with nothing on
   disk but SDKMAN + the script. The same sources are aggregated
   into the Gradle `modules/cli` module for ProGuarded fat JARs
   and GraalVM `native-image` binaries in Stage S7.

---

## 2. Repository layout after migration

```
banana-figlet/
├── .sdkmanrc
├── gradle/libs.versions.toml
├── settings.gradle.kts
├── build.gradle.kts                          ← registers profiles `java` + `kmp`
├── pom.xml                                    ← retained Maven build (legacy-channel only)
├── src/main/java/io/leego/banana/**          ← Java sources (stabilised in Phase B; frozen at Phase C)
├── src/test/kotlin/io/leego/banana/**        ← NEW: Kotlin tests, run against Java via `profile=java`
├── kmp/
│   ├── core/
│   │   ├── src/commonMain/kotlin/…            ← pure figlet renderer (Phase D+)
│   │   ├── src/commonTest/kotlin/…            ← SAME Kotlin tests, reused verbatim
│   │   ├── src/jvmMain/kotlin/…               ← File I/O, ZipInputStream
│   │   ├── src/jvmTest/kotlin/…               ← JVM-only tests (fuzz, resource)
│   │   ├── src/jsMain/, wasmJsMain/, nativeMain/
│   ├── cli/
│   ├── ui-compose-desktop/
│   ├── ui-compose-html/
│   └── ui-shared/
├── proguard/
│   ├── proguard-rules-common.pro
│   ├── proguard-rules-java.pro
│   └── proguard-rules-kmp.pro
├── testdata/                                  ← .flf + .tlf corpus + JBang generators
└── .github/workflows/
    ├── java.yml                                ← profile=java
    └── kmp.yml                                 ← profile=kmp
```

---

## 3. Test strategy across every level

| Level | Source set | Runner | Profile(s) |
| --- | --- | --- | --- |
| Unit | `commonTest` / `jvmTest` | `kotlin.test` | java, kmp |
| Integration | `jvmTest` | JUnit 5 | java, kmp |
| **Text-output parity** | `src/test/kotlin/.../parity` | `kotlin.test` driving `FigletIo` (Java actual, plus Kotlin actual from S4+) | java, kmp |
| **UI (common, expect/actual)** | `modules/ui-shared/commonTest` (post-S5) | `kotlin.test` driving `UiDriver` (Compose Desktop + Web actuals) | kmp |
| API / contract | `jvmTest`, `jsTest`, `nativeTest` | JUnit 5 / kotlin.test | kmp |
| E2E | `:e2e` | JUnit 5 + Gradle `runCli` (JBang + Clikt CLI) | java, kmp |
| Load / perf | `:benchmarks` | `kotlinx-benchmark` + JMH | java, kmp |
| Fuzz | `jvmTest` | Jazzer | java, kmp |

Under `profile=java`, the Kotlin tests drive `JavaFigletIo` (an
adapter over the frozen `BananaUtils`). Under `profile=kmp`, the
same tests drive both `JavaFigletIo` *and* `KotlinFigletIo` via
parameterisation — any divergence fails the build.

UI parity is deferred to Stages S5 / S6 (post-vendor) and runs
through the host's `UiDriver` expect/actual. See
[`../fonts-bitsnpicas/docs/diagrams/08-ui-driver-expect-actual.puml`](../fonts-bitsnpicas/docs/diagrams/08-ui-driver-expect-actual.puml).

---

## 4. TDD test plan — failing tests first, fix Java, then port

### 4.1 One failing Kotlin test per `CODE_REVIEW.md` finding

| Finding | Failing Kotlin test |
| --- | --- |
| C1. `substring(5, 6)` OOB | `FlfHeaderParserTest.header_shorter_than_6_chars_throws_InvalidFontException_not_SIOOBE()` |
| C2. `figlet[i]` NPE truncation | `FigletRenderTest.truncated_glyph_file_reports_parse_error_not_NPE()` |
| C3. Endmark-only row crash | `GlyphParserTest.row_of_only_endmarks_parses_as_empty_string()` |
| C4. `smushVerticalFigletLines` empty | `VerticalSmushTest.empty_figlet_input_returns_empty_not_AIOOBE()` |
| C5. `getVerticalSmushDist` OOB | `VerticalSmushTest.curDist_greater_than_len1_returns_valid_distance()` |
| M6. Unwrapped `NumberFormatException` | `FlfHeaderParserTest.non_numeric_header_field_throws_InvalidFontException()` |
| M7. `header.length` unchecked | `FlfHeaderParserTest.short_header_token_list_throws_InvalidFontException()` |
| M8. `ZipInputStream` leak | `TlfLoaderResourceTest.loading_many_tlf_does_not_leak_fds()` *(jvmTest)* |
| M9. `getHorizontalSmushLength` silent mask | `HorizontalSmushTest.substr_len1_minus_curDist_is_valid_when_curDist_le_len1()` |
| M10. Mutable cached `Option` | `OptionImmutabilityTest.mutating_user_option_does_not_affect_cached_meta()` |
| m11. `padLines` allocation | `PaddingTest.padLines_allocates_once_per_line()` (JMH allocation counter) |
| m13. Mixed-layout precedence | `RuleParseTest.mixed_layout_rule_precedence_matches_figlet_spec()` |
| All assertion-bearing tests currently in `BananaUtilsTests.java` | Ported to Kotlin verbatim as pinning tests; the two doc-generating "tests" are relocated to a `generateDocs` Gradle task. |

### 4.2 Strict authoring order

For each subsystem `S` (HeaderParse, GlyphParse, HorizSmush,
VertSmush, TlfLoad, OptionImmutability):

1. **Red (Phase A).** Commit Kotlin tests for `S` against `FigletIo`
   with only `JavaFigletIo` wired. CI shows red under
   `-Pprofile=java`.
2. **Fix Java to green (Phase B).** Edit `src/main/java/io/leego/banana/**`
   until every test passes. Commit subjects cite the finding ID
   (`fix: C1 validate FLF header length`). Raise JaCoCo coverage
   toward 100 %.
3. **Freeze (Phase C).** Tag `legacy-v1`, apply CODEOWNERS read-only
   protection on `src/main/java/io/leego/banana/**`, CI check fails
   any PR touching the frozen tree.
4. **Port (Phase D).** Author `commonMain` Kotlin for `S`; wire
   `KotlinFigletIo`. The *same* Kotlin test class runs twice under
   `-Pprofile=kmp` — once against frozen Java, once against Kotlin.
5. **Differential parity gate.** `testdata/` × every `Font` × canned
   inputs rendered by both implementations must be byte-exact
   (minus the specific fixed-bug divergences).
6. Retire the `JavaFigletIo` delegate; frozen Java under `src/main/java/**`
   remains untouched.

### 4.3 Test corpus

- `testdata/flf/good/*.flf` — every font currently shipped in the
  classpath `/flf/` directory, plus `standard.flf` pinned.
- `testdata/flf/bad/*.flf` — 5-char header, non-numeric numeric
  field, truncated glyph, empty glyph row, endmark-only row.
- `testdata/tlf/good/*.tlf` — at least 3 Toilet fonts
  (uncovered today per `TEST_REVIEW.md`).
- `testdata/tlf/bad/*.tlf` — empty zip, zip with no entries,
  oversized entry.
- `testdata/texts/*.txt` — "Hello, World!", multi-line sample,
  2 KiB stress sample, unicode-only, empty, whitespace-only,
  newline-only.
- `testdata/gen/*.kt` — JBang-runnable scripts that generate bad
  fonts deterministically.

---

## 4.6 Text + UI parity tier

banana-figlet's `Glyph.vectorize` output is text (SVG as a
string); `bananaify` output is also text. Parity is enforced at
two levels:

1. **Text-output parity** — Kotlin tests under
   `src/test/kotlin/io/leego/banana/parity/**` (scaffolded at S2)
   pin today's `BananaUtils.bananaify` / `bananansi` output
   against every bundled font. From S4 onward the same tests run
   against both the frozen Java and `commonMain` Kotlin via a
   `FigletIo` expect/actual — byte-exact divergence fails CI.
2. **UI parity** — once vendored into the host at S5, this
   library's Kotlin implementation plugs into the host's
   `UiDriver` (see
   [`../fonts-bitsnpicas/docs/diagrams/08-ui-driver-expect-actual.puml`](../fonts-bitsnpicas/docs/diagrams/08-ui-driver-expect-actual.puml)).
   ARGB-hash parity on the rendered FIGlet preview is then gated
   against Swing (blue, via the host), Compose Desktop (green),
   and Compose Web (green).

---

## 5. 100 % coverage gates (both profiles)

- **Profile `java`**: JaCoCo via Gradle `jacoco` plugin against
  the whole library (`io.leego.banana.**` — no Option-C
  exclusions; the library is small enough to cover end-to-end).
  `jacocoTestCoverageVerification` rule `minimum = 1.0` on line +
  branch. Required to enter Stage S3 (freeze).
- **Profile `kmp`**: Kover with `minBound = 100` on line + branch for
  every `commonMain` module; Pitest ≥ 85 % mutation.
- No ignores on either profile.

---

## 6. Migration phases

### Phase A — Toolchain + red tests

- Commit `.sdkmanrc`, `gradle/libs.versions.toml`, `settings.gradle.kts`,
  `build.gradle.kts` with profiles `-Pprofile=java` / `-Pprofile=kmp`.
- Commit `.github/workflows/java.yml` and `kmp.yml` (SDKMAN + Gradle).
- Introduce `FigletIo` + `JavaFigletIo`.
- Commit Kotlin TDD tests from §4.1 — **they fail on CI**.
- Gate: `profile=java` red on purpose; `profile=kmp` green on empty module.

### Phase B — Fix Java to green + 100 % JaCoCo

- Fix `src/main/java/io/leego/banana/**` one finding at a time; each
  commit references the finding ID.
- Port every assertion-bearing test from `BananaUtilsTests.java` to
  Kotlin as pinning tests against `JavaFigletIo`.
- Relocate `testGenerateFontDocs` and `testGenerateLayoutDocs` to a
  `generateDocs` Gradle task.
- Expand corpus until JaCoCo reaches 100 %.
- Gate: `profile=java` green, JaCoCo 100 %, all findings closed.

### Phase C — Freeze Java

- Tag `legacy-v1` on the last Phase-B commit.
- CODEOWNERS + branch-protection + CI diff-check guard the frozen
  tree. Maven (`pom.xml`) and legacy publish path remain intact for
  the `java` profile's release channel.

### Phase D — Port to `commonMain`

Priority: **HeaderParse → GlyphParse → HorizSmush → VertSmush →
TlfLoad → OptionImmutability**.

Per subsystem, one PR that:

1. Adds the `commonMain` Kotlin implementation.
2. Wires `KotlinFigletIo`.
3. The existing test suite runs twice (Java + Kotlin).
4. Differential parity byte-exact on the corpus.
5. Kover 100 % + Pitest ≥ 85 % on the new module.
6. Retires the Java delegate from `jvmMain`; frozen Java remains.

### Phase E — UI + E2E + load/perf

- `ui-compose-desktop/` — Compose host exposing `BananaUtils.bananaify`
  + `bananansi` with a live text field, font dropdown, layout picker.
- `ui-shared/` — `FigletPreview` composable.
- `ui-compose-html/` — wasmJs host reusing `FigletPreview`.
- UI tests: Compose UI test (desktop) + Compose Web test renderer
  (web) + Playwright Kotlin E2E over the deployed static site.
- `e2e` — runs legacy Java CLI and KMP CLI side-by-side over
  `testdata/`; diff must be empty.
- `benchmarks/` — JMH baselines for both implementations; ±10 %
  regression gate.

### Phase F — Dual CI/CD + release (with ProGuard)

- **`profile=java`** workflow: `sdk env`, then
  `./gradlew -Pprofile=java check jacocoTestCoverageVerification
  proguardRelease verifyProguardedJar`. Publishes a ProGuard-shrunk
  `banana-figlet-legacy-*` JAR.
- **`profile=kmp`** workflow: `sdk env`, then
  `./gradlew -Pprofile=kmp build koverVerify pitest proguardRelease
  verifyProguardedJar packageReleaseDistribution`. Publishes:
  - `banana-figlet-core-jvm` ProGuarded JAR (Maven Central)
  - `banana-figlet-core-js`, `…-wasmjs`, `…-native-*` klibs (unshrunk)
  - `banana-figlet-desktop` Compose bundle (built with Compose's
    ProGuard integration)
  - `banana-figlet-web` static site (Kotlin/JS production webpack)
- **ProGuard rules** in `proguard/`:
  - `proguard-rules-common.pro` — `kotlinx-serialization`,
    `kotlin.reflect`, service-loader keep rules.
  - `proguard-rules-java.pro` — reflective enum lookups in `Font`
    (still used post-Phase-B fix), `bananaify` public API surface.
  - `proguard-rules-kmp.pro` — Compose + coroutines keep rules,
    `@JvmStatic` entry points.
- **`verifyProguardedJar`** runs the full JUnit 5 suite against the
  shrunk JAR on a separate classpath — catches missing keep rules
  before release.
- `mapping.txt` uploaded as a CI artefact and attached to the GitHub
  release for stack-trace deobfuscation.

Both workflows run on every PR and every tag.

---

## 7. Acceptance criteria

1. Every `CODE_REVIEW.md` finding has a named Kotlin test whose Git
   history shows **red → green** across Phase A→B, and **still green**
   at Phase D running against both frozen Java and `commonMain`
   Kotlin.
2. Phase B closes with JaCoCo 100 % line + branch.
3. Phase C tags `legacy-v1`; no post-tag commit touches
   `src/main/java/**` (CI-enforced).
4. Phase D closes with Kover 100 % + Pitest ≥ 85 %.
5. Differential parity byte-exact for the committed corpus.
6. `.tlf` support covered by at least 3 pinned fonts.
7. `profile=java` and `profile=kmp` green on every PR across
   {ubuntu, macos, windows}; `kmp` also green across {jvm, js,
   wasmJs, native}.
8. ProGuard-shrunk JAR passes the full test suite in
   `verifyProguardedJar` on every release.
9. `.sdkmanrc` + `libs.versions.toml` track the latest stable
   toolchain.

---

## 8. Deliverables

- `.sdkmanrc`, `gradle/libs.versions.toml`, `build.gradle.kts`
  with both profiles.
- `FigletIo` fixture interface + `JavaFigletIo` + `KotlinFigletIo`.
- Shared Kotlin test suite under `src/test/kotlin/**` and
  `kmp/core/src/commonTest/**`.
- `kmp/core/` with JVM / JS / wasmJs / Native source sets.
- `kmp/ui-compose-desktop/`, `kmp/ui-compose-html/`, `kmp/ui-shared/`,
  `kmp/cli/`.
- `proguard/*.pro` rule files.
- `testdata/` corpus + JBang generators.
- `.github/workflows/{java,kmp}.yml` both sourcing `.sdkmanrc` and
  running ProGuard + post-shrink verification.
- `TEST_PLAN.md` auto-generated mapping `CODE_REVIEW.md` finding
  → Kotlin test FQN.
- `benchmarks/baselines/*.json`.
- [`INTEGRATION.md`](INTEGRATION.md) — how this repo is consumed
  as a vendored module in the `fonts-bitsnpicas`-hosted font studio.
