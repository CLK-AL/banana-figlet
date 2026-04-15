# Code Review: banana-figlet

## Overview

banana-figlet is a Java library for rendering FIGlet ASCII art fonts. It loads
`.flf` (FIGlet) and `.tlf` (Toilet) fonts, parses character glyphs, and renders
text using configurable horizontal/vertical smushing rules. The main entry
point is `BananaUtils`, which exposes `bananaify()` and `bananansi()`.

Key components:

- `BananaUtils` — core rendering and font parsing (~1000 LOC)
- `Font` — enum managing 300+ pre-loaded fonts and resource loading
- `Option` / `Rule` / `RuleEnum` — font configuration and smushing rules
- `Meta` — caches parsed font data (font, option, glyph map, comment)
- `Ansi` — ANSI colour/styling support
- `Layout` — layout modes (`FULL`, `FITTED`, `SMUSH_U`, `SMUSH_R`)

---

## Critical

### 1. No length check before reading the hardblank

`BananaUtils.java:224`

```java
String hardBlank = header[0].substring(5, 6);
```

FIGlet requires the hardblank at position 5 of the first header token, but a
truncated or malformed `.flf` produces `StringIndexOutOfBoundsException`.

**Fix:** Validate `header[0].length() >= 6` and throw a descriptive
`IllegalArgumentException`.

### 2. `generateFigletLine` NPE on truncated glyphs

`BananaUtils.java:297`

The loop dereferences `figlet[i]` without verifying the slot was populated. A
truncated font leaves `null` entries, producing `NullPointerException` at
render time.

**Fix:** Either reject incomplete glyphs during load (`buildMeta`) or guard
`if (figlet[i] == null) continue;` at the call site.

### 3. Endmark-only row crashes `charRow.substring`

`BananaUtils.java:264-275`

If a glyph line is entirely endmarks (or becomes empty after trimming), the
inner `while` leaves `charIndex == -1`, then
`charRow.substring(0, charIndex + 1)` becomes `substring(0, 0)` — fine — but
`charRow.charAt(charIndex)` earlier in the loop throws
`StringIndexOutOfBoundsException`.

**Fix:** If `charIndex < 0`, short-circuit to an empty string for this row.

### 4. `smushVerticalFigletLines` crashes on empty FIGlets

`BananaUtils.java:552-553`

`figlet1[0].length()` / `figlet2[0].length()` are read without checking that
the outer arrays are non-empty. An empty input text or a font with zero-height
glyphs triggers `ArrayIndexOutOfBoundsException`.

**Fix:** Guard `if (figlet1.length == 0 || figlet2.length == 0) return figlet1;`.

### 5. `getVerticalSmushDist` can index past `subLines1`

`BananaUtils.java:586`

`subLines1` has length `len1 - curDist` while `subLines2` has length
`min(maxDist, curDist)`. When `curDist > len1`, `subLines1.length == 0` but the
loop still accesses `subLines1[i]` for `i` up to `subLines2.length`.

**Fix:** Use `Math.min(subLines1.length, subLines2.length)` as the loop bound
(or reconsider the slicing logic).

---

## Major

### 6. `NumberFormatException` not wrapped in `buildMeta`

`BananaUtils.java:225-232`

`Integer.parseInt(header[n])` is used without a `try/catch`. A malformed
numeric header field propagates an unhelpful `NumberFormatException` out of
the library.

**Fix:** Wrap in `try/catch (NumberFormatException)` and rethrow as
`IllegalArgumentException("Invalid font header field …", e)`.

### 7. Missing `header.length` bounds check

`BananaUtils.java:223-229`

Code indexes `header[1]` through `header[5]` with no `header.length >= 6`
check. Short headers trigger `ArrayIndexOutOfBoundsException`.

**Fix:** Validate header token count before indexing.

### 8. `Font.convertIfZipped` can leak a `ZipInputStream`

`Font.java:672-677`

If `getNextEntry()` fails or the caller is later short-circuited, the stream
is leaked. Caller eventually closes the returned stream, but intermediate
failure paths leak.

**Fix:** Perform the initial entry read inside a try-with-resources and only
hand the stream off once valid.

### 9. `getHorizontalSmushLength` silently masks invariant violations

`BananaUtils.java:423`

`substr(text1, len1 - curDist, len1)` silently returns `""` when
`curDist > len1`, masking logic errors in the overlap loop.

**Fix:** Add a defensive `assert curDist <= len1;` or explicit check.

### 10. Cached `Option` is mutable

`Option.java`

`Option` has public setters and is cached on `Meta`. Any mutation to a user's
`Option` reference silently affects every future render of that font, and
creates a thread-safety hazard.

**Fix:** Make `Option` immutable (final fields, builder/copy-constructor
only) or always defensively copy when reading from `Meta`.

---

## Minor

### 11. `padLines` uses `+=` on `String`

`BananaUtils.java:564-572`

Line concatenation in a loop via `+=` allocates a new `String` per row. For
wide figlets this is measurable.

**Fix:** Pre-compute the padded suffix once and concatenate with
`StringBuilder`.

### 12. `Integer.MAX_VALUE` sentinel in `generateFigletLine`

`BananaUtils.java:295`

Using `Integer.MAX_VALUE` as "unset" is correct but obscure. Swap for
`OptionalInt` or an explicit `initialised` flag to make intent clear.

### 13. Unparenthesised mixed `&&`/`||` in `getSmushRule`

`BananaUtils.java:521-527`

The code reads `A && B || C || D`, which works but is hard to audit. Add
explicit parentheses around the `&&` groups.

### 14. Vertical smushing passes `null` as `hardBlank`

`BananaUtils.java:697, 699, 714`

`smushUniversal(..., null)` works only because `s2 == hardBlank` happens to
be false when `hardBlank` is `null`. Document that `hardBlank` may be `null`
for vertical smushing, or split the method.

### 15. `convertIfZipped` produces unhelpful error messages for empty zips

`Font.java:654-679`

A `.tlf` with no entries yields "Failed to convert the InputStream" without
explaining that the archive is empty.

**Fix:** Inspect `entry == null` explicitly and throw a targeted error.

---

## Nit

### 16. Typo in comment

`BananaUtils.java:263`

"endmarks my differ" → "endmarks may differ".

### 17. Magic indices in `smushHorizontalRule5`

`BananaUtils.java:795-801`

Hard-coded `0`, `3`, `6` into the rule string. Extract as named constants to
make the layout of the rule explicit.

### 18. `Ansi.ansify` counts nulls instead of filtering

`Ansi.java:118-119`

Filtering the style array with `Arrays.stream(styles).filter(Objects::nonNull)`
is clearer than the current null-count approach.

---

## Summary

| Severity | Count |
| --- | --- |
| Critical | 5 |
| Major    | 5 |
| Minor    | 5 |
| Nit      | 3 |

**Top priorities:**

1. Harden `buildMeta` against malformed headers — validate lengths, wrap
   `NumberFormatException`, and guard `figlet[i]` population.
2. Fix the edge-case crashes in `generateFigletLine`,
   `smushVerticalFigletLines`, and `getVerticalSmushDist`.
3. Make cached `Option` immutable to remove a silent shared-state hazard.

Addressing items 1 and 2 removes the library's main user-facing crash
surface; item 3 removes a correctness foot-gun when fonts are reused.
