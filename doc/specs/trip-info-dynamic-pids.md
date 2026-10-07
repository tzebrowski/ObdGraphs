# Spec: Trip Info shows any selected PID

2026-10-04 · branch `feat/trip-info-dynamic-pids`

## Summary

The Trip Info screen (Android Auto and phone) can now show any PID the user selects, not only a fixed list of 22. The bottom row becomes configurable, and the top grid fits more PIDs by shrinking. Existing setups render exactly as before. 15 files, roughly +550 / −130 lines.

**Problem.** The PID list was hardcoded in four places that had to agree: the dialog's source list (`TripInfoQueryStrategy.defaults`), one field per PID in `TripInfoDetails`, one `getMetric(Pid.X)` per PID in `TripInfoSurfaceRenderer`, and two fixed descriptor lists in `TripInfoDrawer` (16 top, 3 bottom). A PID missing from any of them was silently not drawn, even when it was selected and queried.

**Scope.** `:screen_renderer` (planning, renderer, drawer, new JVM tests), `:datalogger` (pref constants), `:app` (PID dialog, preference, EN/PL strings), `:automotive` (refresh event). Performance screen and paging are out of scope.

**Constraint.** No breaking changes: same pref keys and stored values, no migration, and an unchanged layout for any existing selection.

## Behaviour changes

| Area | Before | After |
| --- | --- | --- |
| Trip Info PID dialog | Lists the 22 default PIDs only | Lists the full PID registry (ECU-supported/stable filters apply); the defaults and the current selection are always listed |
| Selected PID outside the defaults | Not drawn | Drawn in the top grid with generic formatting |
| Top grid order | Fixed order of 16 PIDs | Same 16 in the same fixed order, then any other PID in the dialog's drag order |
| Bottom row | Always intake pressure, oil pressure, torque (when selected) | New setting **Bottom row PIDs**, max 4, chosen from the selected PIDs; unset = the former row; empty = no bottom row |
| More than 18 grid PIDs | Rows grow down and overlap the bottom row | Text shrinks and columns are added within the same 3-row height; up to 32 PIDs at 0.75 size; beyond that 31 are drawn and the last cell shows "+N" for the rest |
| Ambient temp, atm pressure, dynamic selector | Queried, never drawn in the grid | Unchanged (status panel and theme only) |
| Vehicle status (status panel / disconnect-when-off on) | Queried, never drawn | Unchanged: listed in `TRIP_INFO_STATUS_PIDS` |

## Settings

| Key | Type | Meaning |
| --- | --- | --- |
| `pref.aa.trip_info.pids.selected` | StringSet of PID ids | Unchanged. What Trip Info queries and can draw. Shared by AA and phone |
| `pref.aa.trip_info.pids.selected.view.settings` | JSON order | Unchanged key, written by the dialog's drag reorder. Now orders the non-default PIDs |
| `pref.aa.trip_info.bottom.pids.selected` | StringSet of PID ids | **New.** Which selected PIDs go to the bottom row. Deliberately has **no XML `defaultValue`**: Android would persist it the first time settings open and wipe the default row |
| `pref.aa.trip_info.bottom.pids.selected.view.settings` | JSON order | **New.** Bottom row order |

The bottom setting is in `preferences.xml` under AA → Trip Info → displayed PIDs, with dialog source `trip_info_bottom`. Changing it broadcasts `pref.aa.trip_info.bottom.pids.selected.event.changed`, which `SurfaceRendererScreen` handles the same way as a Trip Info selection change. New strings: `pref.aa.trip_info.bottom_pids` and `pref.aa.trip_info.bottom_pids_summary`, in EN and PL.

## Implementation

1. **`TripInfoQueryStrategy.kt` (`:datalogger`).** Adds public `PREF_QUERY_TRIP_INFO_SELECTED`, `PREF_QUERY_TRIP_INFO_BOTTOM` and `TRIP_INFO_DEFAULT_BOTTOM_PIDS`. The query itself is unchanged: `getPIDs()` returns the main selection only.
2. **`TripInfoMetrics.kt` (new, `:screen_renderer`).** Pure, Canvas-free functions:
   - `plan(available, bottomSelection, sortOrder, bottomSortOrder)` splits the queried ids into `top` and `bottom` descriptors.
     - **Bottom:** the user's selection in bottom-row order, or `TRIP_INFO_DEFAULT_BOTTOM_PIDS` when unset; only queried ids, never `TRIP_INFO_STATUS_PIDS`; at most `MAX_BOTTOM_ITEMS = 4`. A bottom PID keeps the `diff` flag of its top descriptor, so the odometer shows the trip distance there too (the drawer applies `buildDiff` in both rows).
     - **Top:** the 16 former grid PIDs in their fixed order with their former formatting, then every other queried id in dialog order (unordered ids last, by id). Bottom ids and `TRIP_INFO_STATUS_PIDS` (shared from `TripInfoQueryStrategy.kt`) are excluded.
   - `grid(itemCount)` returns columns, capacity and scale. At scale 1 the grid is 6 columns and the layout is unchanged; at most 18 items fit there. Above that, the scale steps down by 0.05 until `floor(6/s) × floor(3/s)` items fit, with a floor of 0.75 (8 × 4 = 32). `shown`/`hidden` give the tiles drawn and the count for the "+N" cell, which takes the last slot when items do not fit. The floor was 0.5 (12 × 6 = 72) until a DHU test with every PID selected showed about 4 px labels at 800 × 480.
3. **`TripMetricDescriptor`** moved to `TripInfoMetrics.kt` and became id-based, with a `diff` flag for the odometer (`MetricsBuilder.buildDiff`). `BottomMetricDescriptor` was removed.
4. **`TripInfoDetails`** is now two lists of `TripInfoItem` (descriptor plus the current `Metric?`) instead of 21 named fields.
5. **`TripInfoSurfaceRenderer`** caches the plan. It rebuilds the plan when the queried ids change (compared element-wise against a `LongArray`, with no per-frame allocation), when the bottom-row pref or either order pref changes (`OnSharedPreferenceChangeListener`), or on `invalidate()`. Each frame it only refreshes `item.metric` from the collector. `recycle()` unregisters the listener.
6. **`TripInfoDrawer`** iterates the items. `TripInfoLayoutCache` also tracks the `TripInfoGrid`; every input besides the area that changes label geometry (top and bottom counts, bottom PID ids, break-label flag) lives in `TripInfoLabelLayout`, which `requiresLayoutUpdate` consults. The bottom ids are needed because the bottom text size is fitted to the labels: swapping one bottom PID for another keeps the count but changes the widths. `maxItemWidth` uses `grid.columns`; `PerformanceDrawer` calls `drawMetric` without `drawScreen`, so it keeps 6 columns.
7. **`PidDefinitionViewModel` (`:app`).**
   - The `TripInfo` source is the defaults plus the current selection (unfiltered, so a selected PID hidden by a filter is not dropped on save), plus the filtered registry.
   - The new `TripInfoBottom` source is the PIDs in the main selection, minus `TRIP_INFO_STATUS_PIDS`.
   - For the bottom dialog, `persistedSelection()` uses `tripInfoBottomDialogSelection(persisted, listed)` (`:datalogger`): the stored row, or the default row when unset, **limited to the PIDs the dialog lists**. It drives both the checked state and the "did it change" check on save, so saving the dialog unchanged writes nothing — even when a default bottom PID is not selected for Trip Info, which would otherwise persist a shortened row and lose that PID for good.

## Backward compatibility

- No pref keys renamed, no stored values reinterpreted, no migration.
- Existing selections were limited by the old dialog to the 22 defaults. For those, `plan()` yields exactly the former top grid (order and formatting) and bottom row, and `grid()` stays at scale 1. A test pins this.
- A stale drag order saved for the main key cannot move the former grid PIDs.
- **Known exception:** bundled profiles can set the selection directly. `giulia_2_2_multijet.properties` (profile 15) includes 7046 (`EXT_VEHICLE_SPEED`), which was never drawn and now appears in the grid. Trip Info is disabled in that profile by default.

## Tests

`screen_renderer` gains `testImplementation "junit:junit:4.13.2"` and plain JUnit tests in `screen_renderer/src/test/java/org/obd/graphs/renderer/trip/TripInfoMetricsTest.kt`. Run them with `./gradlew :screen_renderer:testDebugUnitTest`.

| Test | Asserts |
| --- | --- |
| default PIDs keep the former layout when no bottom row was ever set | Shuffled input plus a reversed stale order still give the former 16-item top order and 3-item bottom |
| default PIDs keep their former formatting | Distance `diff` with no stats; misfires int, no unit or stats; fuel level 1 decimal; bottom cast-to-int per PID |
| status panel and theme PIDs are not drawn in the grid | Ambient temp, atm pressure and dynamic selector are absent |
| other PIDs follow the default ones in the dialog order | Defaults first, then by sort order, unordered last |
| other PIDs get the generic formatting | Generic descriptor defaults |
| selected bottom row is ordered, capped and the rest moves to the grid | Order honoured, max 4, overflow and unchosen default bottom PIDs appear in the grid |
| empty bottom selection means no bottom row | Empty set means no bottom row; former bottom PIDs move to the grid |
| odometer in the bottom row still shows the trip distance | Distance chosen for the bottom row keeps `diff` |
| vehicle status PID added to every query by the status panel is not drawn | `VEHICLE_STATUS` (added by `QueryStrategyOrchestrator` when the status panel or disconnect-when-off is on) leaves both rows unchanged |
| status panel and theme PIDs chosen for the bottom row are not drawn | Status PIDs in the bottom selection are dropped from both rows |
| bottom PIDs that are not queried are skipped | Only queried ids are placed |
| grid is unchanged up to three full rows | 0–18 items: 6 columns, scale 1 |
| grid shrinks and adds columns to fit more items | 19–32 items fit, scale < 1, more than 6 columns |
| grid stops shrinking at a readable scale | 500 items: scale 0.75, 8 columns, capacity 32 |
| every item that fits is drawn and nothing is hidden | 0–32 items: all shown, hidden 0 |
| items that do not fit are counted in the last cell | 100 items: 31 shown, 69 hidden |
| swapping a bottom PID at the same count requires a layout update | Same count, another bottom PID: the layout is recalculated (failed with the count-only check) |
| unchanged label inputs require no layout update | Same inputs: no update; any changed count, break flag or a reset: update |

`TripInfoQueryStrategyTest` (`:datalogger`) pins the main pref key literal and `tripInfoBottomDialogSelection`: unset means the default row limited to the listed PIDs; a stored row is limited the same way.

## Risks and verification

- **Dialog changes have no unit test.** `:app` has no JVM test setup, so `PidDefinitionViewModel` is verified manually.
- **The grid shows at most 31 PIDs once it overflows**; the rest are still queried (adapter bandwidth) and only counted in the "+N" cell.
- **Small text at high counts.** At 0.75 scale on a small AA display, labels are smaller than before. The user's font-size setting still applies on top.
- **Phone shares the AA keys.** The bottom-row setting sits in the AA section but also affects phone Trip Info, the same as the existing PID selection.

Verification:

- [x] `./gradlew :screen_renderer:testDebugUnitTest` passes (15 tests)
- [x] `./gradlew :datalogger:testDebugUnitTest --tests '*TripInfo*'` passes
- [x] `./gradlew assembleGiuliaDebug` builds; `spotlessApply` applied
- [ ] Manual: with an existing selection, Trip Info on phone and AA looks identical to `master`
- [ ] Manual: open and close both PID dialogs without changes; nothing is persisted, layout unchanged
- [ ] Manual: select a PID outside the defaults; it appears after the default PIDs; drag reorder applies
- [ ] Manual: set, reorder and clear the bottom row on AA; the screen refreshes without reconnecting
- [ ] Manual: select more than 18 PIDs; the grid shrinks and does not overlap the bottom row

## Out of scope / follow-ups

- **Performance screen:** done in `performance-dynamic-pids.md`.
- **Paging** the grid via the AA virtual-screen actions instead of the "+N" cell beyond 32.
- Allowing ambient temp and atm pressure in the grid as an explicit opt-in.
