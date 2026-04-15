# Test Review: banana-figlet

## Inventory

| Type | Present? | Notes |
| --- | --- | --- |
| Unit tests       | **Partial** | `src/test/java/io/leego/banana/BananaUtilsTests.java` — 246 lines, 10 `@Test` methods. |
| Integration tests | No | — |
| UI tests          | N/A | Library has no UI. |
| API/contract tests | No | Public API surface (`BananaUtils.bananaify` overloads, `Ansi`, `Option`) is exercised only through the unit suite. |
| End-to-end tests  | No | — |
| Load/perf tests   | No | — |

Build tooling: Maven (`pom.xml`) with JUnit 4.13.1 under `test` scope.

---

## Findings in `BananaUtilsTests.java`

### Critical

**T1. Two "tests" have no assertions and only exist to generate docs.**
`src/test/java/io/leego/banana/BananaUtilsTests.java:166` (`testGenerateFontDocs`) and
`src/test/java/io/leego/banana/BananaUtilsTests.java:186` (`testGenerateLayoutDocs`).

Both open a `PrintWriter` to `docs/FONTS.md` / `docs/LAYOUTS.md` and dump
generated output. They have **zero assertions**, they cannot fail unless
they throw, and they produce side effects in the working directory on
every test run. This means:

1. They pollute the working tree during `mvn test`.
2. They fail on any environment where `docs/` isn't writable
   (CI containers, read-only filesystems, IDE scratch workspaces).
3. They cannot be parallelised safely.
4. They inflate the reported test count without adding coverage.

*Fix:* Move them to a separate `mvn exec:java` target (or a main class
under a `tools/` package) so "generate docs" and "run tests" are
different commands. If kept in test code, at minimum write to a temp
directory obtained from `java.nio.file.Files.createTempDirectory` and
assert the output is non-empty.

**T2. Three tests print to stdout and assert nothing.**
`BananaUtilsTests.java:208` (`testAllFonts`),
`:217` (`testAllAnsis`),
`:226` (`testAllLayouts`).

Each iterates every enum value, calls the library, and prints output.
No assertion — a `NullPointerException` would fail these, but any
subtler wrong output (wrong font rendered, corrupted smushing, lost
colours) will pass green. They also produce huge amounts of console
noise, making real failures hard to spot.

*Fix:* At minimum assert `!actual.isEmpty()` and that the output
contains the expected number of lines per font height. Better: snapshot
a small stable sample (e.g. `"Hello"` in each font) and compare, so
changes surface as diffs.

### Major

**T3. No negative / error-path tests at all.**
The suite covers the happy path only. There are no tests for:

- malformed `.flf` headers (short header, non-numeric fields, missing
  hardblank) — see critical findings **1, 2, 6, 7** in
  [`CODE_REVIEW.md`](CODE_REVIEW.md);
- empty input text;
- inputs containing only whitespace / only newlines;
- inputs with characters absent from the font (fall-through behaviour);
- `.tlf` (Toilet) fonts despite the library advertising support
  (commit `1dd361b` "Supports toilet (tlf) fonts");
- smushing with mismatched glyph heights (the `getVerticalSmushDist`
  OOB discussed in the code review);
- vertical smushing at all with assertions on layout corner cases.

Every critical bug surfaced in `CODE_REVIEW.md` would have been caught
by a single targeted negative test. Adding even a dozen such cases
would dramatically shrink the crash surface.

**T4. Only 6 of the 300+ fonts are pinned.**
`testFont3DASCII`, `testFontANSIShadow`, `testFontSmall`, `testFontBloody`,
plus the default in `testExample` and `testMultiline`. The library loads
300+ fonts, many of which have quirky smushing rules. Silent regressions
in any of the unpinned fonts go unnoticed.

*Fix:* Parameterise a single "render Hello" test across every `Font`
value and snapshot the result with a checksum (SHA-256 of output) so
regressions fail loudly without storing hundreds of KB of expected
strings in-source.

**T5. `bananansi()` / ANSI output is never asserted.**
`testAllAnsis` (`:217`) calls `bananansi` but prints and asserts
nothing. The whole ANSI styling pipeline — colour codes, reset
sequences, wrapping — has no correctness test.

*Fix:* Assert that the output starts with an expected ANSI escape,
ends with a reset code, and contains the rendered body between them.

**T6. Brittle expected strings.**
Tests embed enormous exact expected outputs with hardcoded whitespace
(including trailing spaces). Any whitespace-preserving editor touching
this file will break the suite. IDE configurations that trim trailing
whitespace on save will silently corrupt the expected strings.

*Fix:* Normalise trailing whitespace before comparing, or store
expected output as `src/test/resources/*.txt` read via
`Files.readAllBytes` (with a Git attribute preserving trailing
whitespace on those resource files).

### Minor

**T7. JUnit 4 (4.13.1) is outdated.**
`pom.xml` — JUnit 4 has been superseded by JUnit Jupiter (5.x) for
years. Moving to JUnit 5 unlocks parameterised tests
(`@ParameterizedTest` + `@EnumSource(Font.class)`) which directly
enables the improvements above. JUnit Vintage can run the legacy tests
during migration.

**T8. Tests leave files in `docs/` across runs.**
Combined with T1, re-running tests after a local `docs/` edit overwrites
uncommitted changes silently. Contributors working on docs can lose
work.

*Fix:* See T1. At minimum add `@Ignore` (or remove from the default
`mvn test` profile) and document the doc-generation command separately.

**T9. `getFieldName` uses reflection to find a font's enum name.**
`BananaUtilsTests.java:236-245`. `Font` appears to be a class with
public static fields (per `CODE_REVIEW.md` the code was recently
converted from enum to class). The reflection walk is slow and fragile;
if `Font` exposes `.getName()` or `.name()`, prefer that. If not, cache
the field-to-instance map once at class load and reuse.

### Nit

**T10. Test class named `…Tests` (plural) instead of `…Test`.**
Minor deviation from the prevailing Java-test naming convention. Not
worth renaming mid-flight but adopt `Test` (singular) for new test
classes.

**T11. No `@Before`/`@BeforeClass` cleanup.**
Because `testGenerateFontDocs` writes to `docs/FONTS.md`, if it runs
before one of the pinned-output tests that indirectly reads it… it
won't today, but the lack of isolation is a latent hazard.

---

## What is missing

1. **`.tlf` (Toilet) font support is uncovered.** The library
   advertises it (commit `1dd361b`) and `Font.convertIfZipped` handles
   zip-packed TLFs. No test loads or renders a `.tlf`. Any regression
   in zip handling or TLF parsing ships silently.
2. **Thread-safety behaviour is uncovered.** `Meta` caches `Option`;
   `Option` is mutable (finding 10 in `CODE_REVIEW.md`). A concurrent
   test exercising two threads rendering the same font with different
   options would expose the shared-mutable-state bug.
3. **Smushing edge cases.** No test targets `smushVerticalFigletLines`,
   `getHorizontalSmushLength`, or the individual `smushHorizontalRuleN`
   functions directly. All coverage is end-to-end via `bananaify`.
4. **Error-reporting contract.** There is no test asserting that a
   malformed font throws a specific exception type. Consumers have no
   guarantee of error behaviour.
5. **Performance.** No JMH or even wall-clock benchmark exists, despite
   the known string-concat inefficiencies (finding 11 in
   `CODE_REVIEW.md`).

## Recommendations

1. **Delete or relocate `testGenerateFontDocs` and `testGenerateLayoutDocs`.**
   These are not tests; treat them as a documentation tool.
2. **Migrate to JUnit 5** and introduce a parameterised
   "render `Hello` in every Font, assert non-empty & snapshot hash" test
   to catch regressions across the full font catalogue.
3. **Add a negative-input test class** (`BananaFontParsingTest`) that
   covers the critical findings in `CODE_REVIEW.md` one-by-one — each
   finding becomes one test.
4. **Add at least one `.tlf` test** to pin the Toilet-font path.
5. **Assert `bananansi` output.** Even simple prefix/suffix assertions
   would catch breakage of the ANSI wrapping logic.

## Summary

| Severity | Count |
| --- | --- |
| Critical | 2 (docs-generating "tests", assertion-free iterations) |
| Major    | 4 (no negative tests, sparse font coverage, no ANSI assertions, brittle expected strings) |
| Minor    | 3 (JUnit 4, uncommitted-doc risk, reflection in helper) |
| Nit      | 2 |

The single most valuable next step is adding negative-path tests for the
five critical parsing bugs in `CODE_REVIEW.md`; the next-most-valuable is
separating documentation generation from the test lifecycle.

---

## Execution plan (KMP / Gradle / TDD / 100 % coverage)

The concrete execution is in [`MIGRATION_PLAN.md`](MIGRATION_PLAN.md).
In short:

- Each test level (unit, integration, UI, API, E2E, load, fuzz) is
  wired to a dedicated Gradle KMP source set (§3 of the plan).
- Every `CODE_REVIEW.md` finding becomes one named Kotlin test in
  `commonTest` / `jvmTest`, authored **red** against a JVM delegate
  over the legacy Java code (§4 of the plan).
- Every assertion-bearing test currently in `BananaUtilsTests.java`
  is ported to Kotlin against the delegate to pin current correct
  behaviour.
- The two doc-generating "tests" are relocated to a dedicated
  Gradle `generateDocs` task and removed from `mvn test` / `gradle
  test`.
- `.tlf` support gets at least three pinned fonts in
  `testdata/tlf/good/`.
- 100 % Kotlin line + branch coverage (Kover) and ≥ 85 % mutation
  coverage (Pitest) are enforced per subsystem before that subsystem
  moves from `jvmMain` (Java delegate) to pure `commonMain`.
- Legacy Java under `src/main/java/**` stays frozen; dual
  `java-legacy` (Maven / JUnit 4) and `kmp` (Gradle / KMP matrix)
  CI lanes both stay green on every PR.
