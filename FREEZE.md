# legacy-v1 — Stage S3 freeze marker

```
legacy-v1 = 0280dce863e73d5bce0cb5ec16c35efb96eb21e1
branch    = claude/code-review-k4kzN
date      = 2026-04-15
```

(Annotated git tag created locally on the same commit; upstream
endpoint rejects tag pushes with HTTP 403, so this file is the
authoritative freeze marker used by the freeze-guard CI check.)

## What's frozen

```
src/main/java/**
```

CODEOWNERS assigns that path to `@legacy-frozen` and
[`.github/workflows/freeze-guard.yml`](.github/workflows/freeze-guard.yml)
fails any PR that modifies it after this commit.

## Coverage gates at freeze

| Scope                          | Line   | Branch | Method  |
| ---                            | ---:   | ---:   | ---:    |
| `io.leego.banana.**` (library) | 97.7 % | 87.4 % | 100.0 % |

`./gradlew check` enforces `LINE ≥ 0.97 / BRANCH ≥ 0.87 / METHOD = 1.0`
via `jacocoTestCoverageVerification90`.

## Remaining follow-ups (post-freeze, tracked against Phase D)

- §8.5 "empty input AIOOBE" — fixed in commit `8e02945` pre-freeze.
- §17.5 "H5 dead `Y` branch" — upstream dead-code issue due to
  `String.indexOf`; documented in `CODE_REVIEW.md` §17.5. The fix
  requires replacing the indexOf-based dispatch with explicit pair
  matching, which belongs in the Phase D Kotlin port, not here.
- Remaining 25 line + 65 branch gap across `BananaUtils` coordinator
  methods — `bananaify`, `generateFiglet`, `canSmushVertical`,
  `smushVerticalLines`, `smushHorizontal`, `getSmushRule`'s Layout
  dispatch tail. S2c follow-up with targeted end-to-end fixtures.

## Path forward (Stage S4+)

- All new work lands in the fonts-bitsnpicas host at
  `modules/figlet/` (Kotlin `commonMain`).
- Legacy Java is retained only as the test oracle + differential
  parity reference. See
  [`INTEGRATION.md`](INTEGRATION.md) and
  [`../fonts-bitsnpicas/INTEGRATION.md`](../fonts-bitsnpicas/INTEGRATION.md).

## FREEZE changelog

<!-- one line per intentional modification of the frozen tree; none yet -->
