# Spec: DTC filter chips (phone) and state icons (AA)

2026-10-20 · branch `feat/dtc-filters-aa-polish` (ObdGraphs only) · Status: implemented, on-device checks pending

Roadmap item 10 (`dtc-roadmap.md`).

## Summary

**Problem.** With 30+ codes over 13 modules the phone list is long, and finding the codes failing right now means
scrolling past stored and pending ones. On AA every code row had the same warning triangle, only tinted, so pending,
history and permanent codes looked alike.

**Scope.** `:datalogger` (filter and row icon logic, shared), `:app` (chip row in the DTC dialog, EN + PL strings),
`:automotive` (row icons). No ObdMetrics change.

## Behaviour changes

| Area | Before | After |
|---|---|---|
| Phone DTC dialog | List only | Chip row above the list: "All (n)", "Active (n)", "Stored (n)", "Pending (n)", each with its state chip's icon and colour |
| Phone, a state filter picked | — | Only that state's codes, under their module with the shown count; modules without a match (clean, no response, refused) and the "no longer reported" section are left out; "No codes in this state." when nothing matches |
| Phone, a chip with count 0 | — | Greyed (disabled), unless it is the picked one |
| Phone, no codes | — | No chip row; the filter falls back to All |
| Phone, reopening the dialog | — | Starts on All |
| Shared report, Clear Codes, Clear this module | All codes | Unchanged: all codes, whatever the filter |
| AA code row | Triangle; red active, yellow stored, plain otherwise | The phone chip's glyph per state: active red, stored yellow, pending blue, history plain, permanent-only plain; triangle for a code with no state |

## Settings / keys touched

None. The filter is not stored (back to All on every open).

## Implementation

* `dtc/DtcFilter.kt` (`:datalogger`): `DtcFilter` (ALL / ACTIVE / STORED / PENDING, each with its `DtcState`),
  `dtcFilterCounts()` (code rows per filter, gone rows left out) and `filteredBy(filter, emptyMessage)`.
* Phone pipeline (`refreshList`): `toDtcListItems → markedWith → filteredBy → withModuleSummaries → collapsedBy →
  withModuleClearActions → withGoneSection` (the last only under All). Filtering before the summaries makes a header's
  count and mini chips describe what is shown. Counts come from the unfiltered list.
* Chips are compact Material `Chip.Choice` (`item_dtc_filter_chip.xml`, 12sp, 16 dp icon) in a single-selection
  `ChipGroup` (`dialog_dtc.xml`, between the legend and the column headers) that wraps to a second line. Wrong turn: a
  single line in a `HorizontalScrollView` cut "Pending" off at phone width with nothing showing the row scrolls. Labels: `DtcFilter.labelRes()` (the state chips' names, `dtc.filter.all`).
* AA: `DtcStatus.rowIcon()` (`DtcStatus.kt`) picks a `DtcRowIcon`; `DiagnosticTroubleCodesScreen.row` maps it to
  `dtc_row_*` (`:automotive`, copies of the phone's `dtc_state_*` glyphs at 24 dp: AA cannot see `:app` resources) and
  a `CarColor`. A state wins over permanent; the status line still says both.

## Backward compatibility

No stored data changes. AA keeps red / yellow for active / stored; pending is now blue instead of plain.

## Tests

* `DtcListItemTest`: matching codes under their module with the shown count; modules without a match left out, clean
  ones included; empty message; All leaves the list unchanged; flat default-ECU list; counts per filter without gone rows.
* `DtcStatusTest`: AA row icon per state, permanent only without a state.
* `DtcFilterChipsTest` (`:app`): every state filter is named like its state chip, `dtc.filter.*` exist in EN and PL, the
  chip string takes a label and a count.

## Risks and verification checklist

* [ ] Phone, list with several states: counts add up, each chip shows only its codes, headers show the shown count.
* [ ] Phone, EN and PL at phone width: every chip visible (wrapped to a second line if needed), none clipped.
* [ ] Phone, filter on Active, then Refresh / Clear: list and counts update, the chip stays picked; after a clear with
  no codes left the chip row hides.
* [ ] Phone, filter on, collapse / expand a module and "Clear this module": still work (the clear covers the whole module).
* [ ] Phone dark theme: chip icons readable.
* [ ] AA (DHU): active / stored / pending rows show different glyphs and colours.

## Out of scope

* A filter on AA: its lists are short and its strip takes two actions (Read, Clear).
* Filters for History or Permanent: under All; the roadmap asked for the three states a mechanic acts on.
