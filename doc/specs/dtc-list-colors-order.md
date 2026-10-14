# Spec: DTC list colours and module order

2026-10-13 · branch `feat/dtc-colors-order` · Status: implemented, manual checks pending

## Summary

**Problem.** In the DTC list (phone dialog and AA screen) modules, codes and "no codes" lines looked alike, and
clean modules came first: with modules sorted by name, the Engine ECU's codes sat below five clean modules. On the
phone's dark theme the descriptions were drawn in `Color.DKGRAY` and hard to read.

**Scope.** `:datalogger` (shared ordering and counts), `:app` (phone dialog colours), `:automotive` (AA rows).

## Behaviour changes

| Area | Before | After |
|---|---|---|
| Module order (phone + AA) | All modules by name | Modules with codes first (by name), then clean modules (by name) |
| Module header, phone | Indigo, every module | Amber with the count, e.g. "ENGINE ECU (13)"; clean modules grey |
| Module header, AA | Name | "Engine ECU (13)" for a module with codes (headers allow no colour) |
| Module sections, phone (added 2026-10-14) | Always open | Tap the header to collapse / expand (arrow at its end). Modules with codes start expanded, clean ones collapsed (a 13-module scan was mostly "no codes" lines). Choice kept while the dialog is open |
| Module header summary, phone (added 2026-10-14) | Count only | Mini chips after the name, open or collapsed: state icon + count per state (active / stored / pending / history), "MIL" if any code requests it, "new" icon + count; a green check for a clean module. Same icons and colours as the row chips and the legend |
| "No codes reported", phone | Grey italic | Green italic |
| "No codes reported", AA | Plain row | Row with a green check icon |
| Code row, phone | Red code, dark grey description | Red code, description in the dialog's primary text colour (unknown: secondary, italic) |
| Code row, AA | Plain row | Red warning icon; status line in yellow |

## Settings / keys touched

None.

## Implementation

* `DtcListItem.toDtcListItems` sorts the module labels by "has codes" then name, and sets
  `ModuleHeader.codeCount`; `toDtcSections` carries it into `DtcSection.codeCount`. `ModuleHeader.displayName()`
  builds "Name (n)". The count is not part of `itemKey()`, so a read that changes it keeps the row identity.
* Phone (`DiagnosticTroubleCodeViewAdapter`): colours from resources, night-aware. New `dtc_module_with_codes`
  (day `#B45309`, night `#FBBF24`); codes keep `cardinal`, "no codes" uses `philippine_green`, text uses
  `dialog_text_primary` / `dialog_text_secondary`.
* Collapsible modules (shared logic in `DtcListItem.kt`, phone only): `ModuleHeader` gains `summary`
  (`DtcModuleSummary`: counts by `DtcState`, MIL, new) and `expanded`, neither in `itemKey()`. The dialog builds
  `toDtcListItems → markedWith → withModuleSummaries → collapsedBy { moduleExpanded[it] } → withGoneSection`;
  `collapsedBy` drops a collapsed module's rows and message, defaulting to `isExpandedByDefault()` (has codes). The
  gone section is never hidden by a module toggle. Header: `item_dtc_module_header.xml`, chips built in
  `HeaderViewHolder` with style `DtcMiniBadge` and the adapter's `showBadge`, each with a content description
  ("7 Active"). The "no longer reported" header reuses `item_dtc_module_header.xml` (green title, `GONE_BADGE` chip with
  the count), so every section header looks alike; an earlier green banner stood out too much. AA keeps plain section
  headers: its sections cannot collapse.
* AA (`DiagnosticTroubleCodesScreen`): car-app 1.4 allows colour spans only in row text lines. Section headers and
  row titles are text-only, so codes are marked with a tinted `CarIcon` (`dtc_fault`, red) and modules without codes
  with `dtc_ok` (green). The status line uses `ForegroundCarColorSpan` (yellow).

## Backward compatibility

No stored data changes. `ModuleHeader(module)` keeps working (`codeCount` defaults to 0).

## Tests

* `DtcListItemTest`: modules with codes come first, each group by name, with their counts; header display name with
  and without a count; the unknown-module case now leads the list. Module summaries (states, MIL, new; no-state codes
  left out); default expansion and the user's choice; a collapsed module never hides the gone section.

## Risks and verification checklist

- [ ] Phone, dark theme: amber headers with counts on top, green "no codes" lines, readable descriptions.
- [ ] Phone, light theme: the same colours readable on the light dialog.
- [ ] AA (DHU): module with codes first with "(n)"; red icons on codes, yellow status line, green checks on clean modules.
- Risk: the host decides how a tinted icon and coloured text render; some hosts may ignore the colours.

## Out of scope

Colouring AA section headers (not supported by the template), ordering codes by severity within a module.
