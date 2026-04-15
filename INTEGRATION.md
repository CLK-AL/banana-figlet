# INTEGRATION.md — banana-figlet as a vendored module

This repository is a **consumed module** of the
`fonts-bitsnpicas`-hosted font studio. The authoritative integration
design lives in that repo at
[`fonts-bitsnpicas/INTEGRATION.md`](https://github.com/CLK-AL/fonts-bitsnpicas/blob/claude/code-review-k4kzN/INTEGRATION.md).

## Role

`banana-figlet` contributes the **FIGlet rendering subsystem** to
the integrated font studio:

- `modules/figlet/` inside the host — pure `commonMain` Kotlin
  translation of `BananaUtils.java`, `Font.java`, `Option.java`,
  and the smushing rules.
- Implements the host's `modules/core` interfaces
  (`Font`, `Glyph`, `FontReader<FigletFont>`).
- `FigletGlyph` implements both `rasterize(PixelSink)` and the
  new `vectorize(SvgSink, VectorizeOptions)` surface — each
  printable cell of a FIGlet row becomes a unit-square path, or
  a colour path when the source `bananansi` output carries ANSI
  colour metadata.

## Integration timeline

| Phase | How the host consumes this repo |
| --- | --- |
| **A** (weeks) | Host pulls `io.leego:banana-figlet:<latest>` as a **Gradle dependency**. Swing editor gains a "FIGlet preview" panel with zero code migration here. |
| **B** (post-freeze) | Sources are **vendored** into `modules/figlet/` of the host. Maven dep is dropped — `commonMain` cannot depend on a JVM-only JAR. Kotlin translation uses this repo's **frozen** `src/main/java/**` (post-`legacy-v1`) as the byte-exact reference for the differential parity gate. |

Full phase diagram:
[`docs/diagrams/05-integration-phases.puml`](../fonts-bitsnpicas/docs/diagrams/05-integration-phases.puml)
in the host.

## What this repo owes the host

1. **Freeze a stable Java.** Complete Phase A + B of this repo's
   [`MIGRATION_PLAN.md`](MIGRATION_PLAN.md) so the host has a
   reference implementation for differential parity tests.
2. **Expose `.tlf` (Toilet) font support.** The host uses both
   `.flf` and `.tlf`; at least three `.tlf` fixtures must be pinned
   under `testdata/tlf/good/`.
3. **Deterministic output.** `bananaify` must be a pure function of
   its inputs (no ambient state). This is a hard requirement for
   the host's differential parity gate.
4. **Immutable cached `Option`.** Finding M10 in `CODE_REVIEW.md` —
   must be fixed before vendoring, otherwise shared mutable state
   surfaces in the host's Compose UI.

## What the host owes this repo

- The host's `modules/core` interfaces are the **single target**
  for `FigletFont` / `FigletGlyph` — any breaking change to those
  interfaces is a host-side commit accompanied by a matching
  update in `modules/figlet/` (same PR).
- `third_party/MANIFEST.toml` in the host records the upstream SHA
  each time `scripts/vendor-pull.sh` pulls this repo. The host is
  the source of truth for "which revision of banana-figlet is
  vendored"; this repo's own tags are informational after Phase B.

## File-path map (Phase B)

```
banana-figlet/                                     fonts-bitsnpicas (host)
src/main/java/io/leego/banana/BananaUtils.java  ─► modules/figlet/src/commonMain/kotlin/figlet/Rendering.kt
src/main/java/io/leego/banana/Font.java         ─► modules/figlet/src/commonMain/kotlin/figlet/Font.kt
src/main/java/io/leego/banana/Option.java       ─► modules/figlet/src/commonMain/kotlin/figlet/Option.kt
src/main/java/io/leego/banana/Ansi.java         ─► modules/figlet/src/commonMain/kotlin/figlet/Ansi.kt
src/main/java/io/leego/banana/Meta.java         ─► modules/figlet/src/commonMain/kotlin/figlet/Meta.kt
src/main/java/io/leego/banana/Rule.java         ─► modules/figlet/src/commonMain/kotlin/figlet/Rule.kt
src/main/resources/flf/*.flf                    ─► modules/figlet/src/commonMain/resources/flf/*.flf
                                                   (assets; loaded via kotlinx-resources)
src/test/kotlin/**                              ─► run untouched under both profiles
                                                   on the host via a shared test module
```

## Differential-parity contract

The host's `SvgParityTest` and `FigletRenderTest` run with both
profiles simultaneously and compare:

- **Text output.** For a grid of (font, input) pairs, the shrunk
  Java output (frozen `legacy-v1`) and the `commonMain` Kotlin
  output must be byte-identical.
- **SVG output.** Same inputs → `vectorize` emits identical SVG
  up to `VectorizeOptions.svgPrecision`.

Any divergence fails the host's build. Intentional divergences
(e.g. the fixes for the Critical findings) are documented as
explicit tests with a justification comment.

## CI / release channels

The host runs two profiles — `java` and `kmp`. This repo retains
its own dual setup as described in
[`MIGRATION_PLAN.md`](MIGRATION_PLAN.md):

- `java` profile publishes `banana-figlet-legacy-*` JAR
  (ProGuard-shrunk) for standalone users.
- `kmp` profile is mostly for tests + library dev; the
  **user-facing** KMP artefact is `fonts-bitsnpicas-figlet-*`
  published by the host.

## See also

- This repo: [`MIGRATION_PLAN.md`](MIGRATION_PLAN.md) — Phase A+B
  scaffolding + red-green commit schedule.
- Host: [`fonts-bitsnpicas/INTEGRATION.md`](https://github.com/CLK-AL/fonts-bitsnpicas/blob/claude/code-review-k4kzN/INTEGRATION.md)
  — overall design, unified class model, PUML diagrams.
