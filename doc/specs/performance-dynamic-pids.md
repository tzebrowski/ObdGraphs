# Spec: Performance shows any selected PID

2026-10-06 · branch `feat/performance-dynamic-pids` · Status: implemented, manual checks pending

## Summary

The Performance screen (Android Auto and phone) can now show any PID the user selects, the same way
Trip Info does since `trip-info-dynamic-pids.md`. The gauges become configurable, and the top grid fits
more PIDs by shrinking. Existing setups render as before.

**Problem.** What Performance draws is decided by the profile lists `pref.query.performance.top`
(grid) and `pref.query.performance.bottom` (gauges); the PID dialog offered only those lists plus the
brake boosting PIDs. A selected PID outside them was queried but silently not drawn, and the user had
no way to choose the gauges. More than 15 grid PIDs (bundled profiles list 21) grew downwards and
pushed the gauges off the screen.

**Scope.** `:screen_renderer` (planning, cache, drawer, JVM tests), `:datalogger` (pref constants,
dialog helper), `:app` (PID dialog, preference, EN/PL strings), `:automotive` (refresh event).

**Constraint.** No breaking changes: same pref keys and stored values, no migration, an unchanged
layout for any selection the old dialog could produce.

## Behaviour changes

| Area | Before | After |
| --- | --- | --- |
| Performance PID dialog | Lists profile top + bottom + brake boosting PIDs only | Lists the full PID registry (ECU-supported/stable filters apply); the profile PIDs and the current selection are always listed |
| Selected PID outside the profile lists | Not drawn | Drawn in the top grid after the profile's grid PIDs, in the dialog's drag order |
| Gauges | The profile's bottom list, not editable | New setting **Gauge PIDs**, max 5, chosen from the selected PIDs; unset = the profile's list; empty = no gauges. Selected PIDs not chosen as gauges go to the grid |
| More than 15 grid PIDs | Rows grow down and push the gauges off screen | Text shrinks and columns are added within the same 3-row height; up to 24 PIDs at 0.75 size; beyond that 23 are drawn and the last cell shows "+N" |
| `pref.query.performance.hidden` | Never drawn | Unchanged; also excluded from the gauge dialog |
| Vehicle status (status panel / disconnect-when-off on) | Never drawn (not in the profile lists) | Unchanged: listed in `PERFORMANCE_STATUS_PIDS` |
| Brake boosting | Unchanged | Unchanged |
| Phone Performance screen | Drew the profile lists | Same plan as AA: follows the AA selection, gauge choice and drag order (no phone-side gauge setting) |
| "Select all" in the Performance PID dialog | Selected the profile's PIDs | Selects the whole registry; a toast warns that the refresh rate drops and only what fits is shown (no cap) |
| More than 5 gauges checked | n/a | The dialog does not stop at 5; saving shows a toast with how many go to the grid. The first 5 in gauge order are drawn, the rest go to the grid |
| PID deselected in the Performance dialog | n/a | Also dropped from a stored gauge selection, so selecting it again later does not bring it back as a gauge. An unset gauge pref stays unset |
| Min/max stats next to a grid value | Hidden once wider than `width / 6` (Trip Info's column count) | Hidden once wider than the Performance column (`width / grid.columns`) |

## Settings

| Key | Type | Meaning |
| --- | --- | --- |
| `pref.aa.performance.pids.selected` | StringSet of PID ids | Unchanged. What Performance queries and can draw. Shared by AA and phone |
| `pref.aa.performance.pids.selected.view.settings` | JSON order | Unchanged key, written by the dialog's drag reorder. Now orders the PIDs outside the profile's grid list |
| `pref.query.performance.top` / `.bottom` / `.hidden` | StringSet | Unchanged, set by the profile. Top: grid PIDs drawn first. Bottom: default gauges. Hidden: never drawn |
| `pref.aa.performance.bottom.pids.selected` | StringSet of PID ids | **New.** Which selected PIDs are gauges. **No XML `defaultValue`**: it would be persisted when settings open and replace the profile's gauges |
| `pref.aa.performance.bottom.pids.selected.view.settings` | JSON order | **New.** Gauge order |

The setting is in `preferences.xml` under AA → Performance → displayed PIDs, dialog source
`performance_bottom`. Saving broadcasts `pref.aa.performance.bottom.pids.selected.event.changed`,
handled by `SurfaceRendererScreen` like a Performance selection change. New strings
`pref.aa.performance.bottom_pids`, `pref.aa.performance.bottom_pids_summary` and
`pref.aa.performance.bottom_pids_overflow`, EN and PL.

## Implementation

1. **`PerformanceQueryStrategy.kt` (`:datalogger`).** Public `PREF_QUERY_PERFORMANCE_SELECTED`,
   `PREF_QUERY_PERFORMANCE_HIDDEN` (moved from `ScreenSettings.kt`), `PREF_QUERY_PERFORMANCE_BOTTOM_SELECTED`,
   `PERFORMANCE_STATUS_PIDS`, and `performanceBottomDialogSelection(persisted, listed)`: the stored gauges,
   or the profile's when unset, limited to the PIDs the dialog lists — the same rule as
   `tripInfoBottomDialogSelection`, so saving the dialog unchanged persists nothing. The query is unchanged.
   Also `PERFORMANCE_MAX_GAUGES = 5`, `performanceGaugeOverflow(count)` and
   `prunedPerformanceBottomSelection(stored, selected)` (null = nothing to write: unset or nothing dropped).
2. **`PerformanceMetrics.kt` (new, `:screen_renderer`).** Pure functions:
   - `plan(available, profileTop, profileBottom, hidden, bottomSelection, sortOrder, bottomSortOrder)`.
     Only queried, non-hidden, non-status PIDs are placed. **Gauges:** the user's selection in gauge order
     (undragged gauges in the profile's bottom order, then by id), or the profile's bottom list when unset; at most `PERFORMANCE_MAX_GAUGES = 5` (bundled profiles define ≤ 5).
     **Grid:** the profile's top list in its order, then every other queried PID in dialog order
     (unordered last, by id); gauge PIDs are excluded.
   - `grid(itemCount)` reuses `TripInfoMetrics.grid`, now parameterised by base columns/rows
     (Performance: 5 × 3, so up to 15 items nothing changes).
3. **`MetricsCache`** keeps the plan and rebuilds it when the queried ids change or on `cacheReset()`.
   It reads the profile lists from `Prefs` at rebuild time; `PerformanceScreenSettings` lost its own
   copies of those lists and its preference listener, which only this cache used. The metrics are
   looked up from the collector every frame.
4. **`PerformanceSurfaceRenderer`** listens for the profile list, gauge and order prefs and resets the
   cache; `recycle()` unregisters it.
5. **`PerformanceDrawer`** draws `grid.shown` items with `textSize × grid.scale` over `grid.columns`
   columns, and the "+N" cell via `TripInfoDrawer.drawHiddenCount` (now non-private). The grid is
   recomputed only when the item count changes. It passes its column width to `TripInfoDrawer.drawMetric`
   (`maxWidth`): the default comes from the Trip Info layout cache, which Performance never lays out.
6. **`PidDefinitionViewModel` (`:app`).** The `Performance` source is the profile PIDs plus the current
   selection (unfiltered), plus the filtered registry. The new `PerformanceBottom` source is the
   selection minus status and hidden PIDs, listed in the profile's gauge order
   (`performanceBottomDialogItems`) because the dialog stores its listed order as the drag order on
   first open; its checked state and save check use
   `performanceBottomDialogSelection`. Saving the `Performance` selection prunes the stored gauges;
   saving the gauge dialog with more than 5 checked shows the overflow toast (`gaugeOverflow()`).

## Backward compatibility

- No pref keys renamed, no stored values reinterpreted, no migration.
- A selection the old dialog allowed contains only profile PIDs, so `plan()` gives the former grid and
  gauges (the old code took the profile lists filtered by queried and hidden). Tests pin this with the
  bundled profile 8 lists.
- No bundled profile lists a PID in both top and bottom, so dropping gauge PIDs from the grid changes nothing.
- Grids of more than 15 PIDs shrink instead of overflowing; that layout was broken before.
- The profile lists are StringSets, so their "order" is the set's iteration order, as before.

## Tests

`./gradlew :screen_renderer:testDebugUnitTest` — `PerformanceMetricsTest`:

| Test | Asserts |
| --- | --- |
| profile PIDs keep the former layout when no gauges were ever set | Shuffled bundled selection and a stale drag order give the former grid and gauges |
| every profile PID selected keeps the former order | Same grid and gauge order, with all top + bottom PIDs (21 grid PIDs now shrink, see below) |
| selected PIDs outside the profile follow its grid PIDs in the dialog order | Profile PIDs first, then by sort order, unordered last |
| hidden PIDs are drawn nowhere | Hidden in neither grid nor gauges, even if chosen as a gauge |
| vehicle status PID added to every query by the status panel is not drawn | Grid unchanged, never a gauge |
| selected gauges are ordered, capped and the rest moves to the grid | Gauge order honoured, max 5, overflow and former gauges go to the grid |
| undragged gauges keep the profile order after one is removed | A stored set without a drag order follows the profile's bottom order, not id order |
| dragged gauges follow the drag order, then the profile order | Dragged first, then profile order, then the rest by id |
| the order the gauge dialog stores on opening keeps the profile order | The drag order the dialog stores on first open does not reorder the gauges |
| empty gauge selection means no gauges | Former gauges move to the grid |
| gauges that are not queried are skipped | Only queried ids |
| grid is unchanged up to three full rows | 0–15 items: 5 columns, scale 1 |
| grid shrinks to fit every profile grid PID | 21 items fit, scale < 1, more columns |
| the hidden count marker always lands in the last column | For 0–200 items, `shown % columns == columns − 1` whenever items are hidden (`PerformanceDrawer` places "+N" there) |
| items that do not fit are counted in the last cell | 100 items: capacity − 1 shown, rest hidden |

`./gradlew :datalogger:testDebugUnitTest --tests '*PerformanceQueryStrategy*'` pins the pref key
literals, `performanceBottomDialogSelection` (unset → profile gauges ∩ listed; stored ∩ listed),
`prunedPerformanceBottomSelection` (drops deselected PIDs; null when unset or unchanged) and
`performanceGaugeOverflow`. The stats width fix is Canvas-only and has no unit test.

## Risks and verification

- **Dialog changes have no unit test** (`:app` has no JVM setup); verify manually.
- **Gauge height is not budgeted.** The grid is capped at 3 rows' height as before; on very small AA
  displays the gauges below may still be tight with a large font setting.
- **Phone shares the AA keys**, as for Trip Info. `MetricsCache` serves both screens and reads
  `pref.aa.performance.bottom.pids.selected` and both `.view.settings` order keys, so a gauge choice or
  drag order made in the AA section also changes the phone screen; the phone has no setting of its own.
- Profile switching loads the new profile's keys; a gauge selection saved under one profile follows the
  generic profile save/load like any other pref.

Verification:

- [x] `./gradlew :screen_renderer:testDebugUnitTest` passes (Performance 11, Trip Info 18)
- [x] `./gradlew :datalogger:testDebugUnitTest --tests '*Performance*' --tests '*TripInfo*'` passes
- [x] `./gradlew spotlessCheck assembleGiuliaDebug assembleGiuliaAADebug` builds
- [ ] Manual: with an existing selection, Performance on phone and AA looks identical to `master`
- [ ] Manual: open and close both PID dialogs without changes; nothing is persisted
- [ ] Manual: select a PID outside the profile; it appears in the grid after the profile PIDs; drag reorder applies
- [ ] Manual: set, reorder and clear the gauges on AA; the screen refreshes without reconnecting
- [ ] Manual: select more than 15 grid PIDs; the grid shrinks and the gauges stay on screen
- [ ] Manual: brake boosting still takes over the screen
- [ ] Manual: check 6+ gauges and save; the toast names the overflow
- [ ] Manual: choose a gauge, deselect it in the Performance dialog, reselect it; it is in the grid
- [ ] Manual: with ≤ 15 grid PIDs, min/max stats show where they fit a 5-column tile

## Out of scope / follow-ups

- Editing the profile's grid order (`pref.query.performance.top`) from the UI.
- Paging instead of the "+N" cell.
- Fitting gauge height to the remaining area.
