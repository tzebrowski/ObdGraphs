# Spec: failure type text

2026-10-15 · branch `feat/dtc-failure-type` (ObdGraphs only) · Status: implemented, on-device checks pending

Roadmap item 7 (`dtc-roadmap.md`).

## Summary

**Problem.** A UDS code carries a failure type byte (FTB), the `-14` in `P0120-14` (SAE J2012 / ISO 15031-6). The app
showed it only raw. ObdMetrics decodes it into `failureType` (`DtcComponent`), but its table
(`DefaultDtcDictionary.FAILURE_TYPE_MAP`) is empty and the PL translation file has no entries, so every description
reads "Unknown Subtype".

**Scope.** `:datalogger` (lookup), `:common` (EN + PL texts), `:app` (expanded row, shared report).

## Behaviour changes

| Area | Before | After |
|---|---|---|
| Expanded code row (phone) | "System: … \| Category: …" | Plus "Failure type: 14 · Circuit short to ground or open" |
| Shared report | No failure type text | Same line after System / Category |
| Byte not in the table | — | ObdMetrics' text if it has a real one (not "Unknown Subtype"), else the category by the high nibble ("General electrical failure"); `F0`–`FF` "Manufacturer-specific failure type" |
| No FTB, not one hex byte, reserved `A0`–`EF` without decoder text | — | No line |

## Settings / keys touched

None.

## Implementation

* `dtc/DtcFailureType.kt`: `FAILURE_TYPES` (byte → string id), `FAILURE_TYPE_CATEGORIES` (high nibble → string id),
  `failureTypeText(code, fallback, getString)` and `DiagnosticTroubleCode.failureTypeLine(getString)`. Callers pass
  `context::getString`, so the lookup stays Android-free and testable.
* Strings `dtc_ftb_<HEX>`, `dtc_ftb_category_<nibble>`, `dtc_ftb_line` in `common/src/main/res/values{,-pl}`, shared
  by phone and AA.
* Order: app table first (translated), then the decoder's own text, then the category. The decoder text would stay
  English on a PL phone unless a profile's translation file supplies it.

## Backward compatibility

Display only. Stored codes (prefs, history) keep their `failureType`; the text is computed on display, so old reads
show it too.

## Tests

`DtcFailureTypeTest` (`:datalogger`, plain JUnit): specific bytes, case / whitespace, fallback order, reserved and
invalid bytes, the line format, and that every table entry points at the string named after its byte and exists in
EN and PL (parses `common`'s `strings.xml`).

## Risks and verification checklist

* [ ] A code with an FTB (e.g. 2.0 GME `P0120-14`): expanded row shows the line, EN and PL.
* [ ] Shared report contains the line.
* [ ] A code without an FTB: no empty line in the expanded row.
* Some ECUs use bytes for their own meaning (manufacturer-specific use of the generic range). The text is the
  standard meaning; treat it as a hint.

## Out of scope

* AA: rows hold two text lines (description + state) and both are used.
* Filling ObdMetrics' `FAILURE_TYPE_MAP` upstream.
* Subtypes added in newer J2012 revisions beyond the table (they fall back to the category).
