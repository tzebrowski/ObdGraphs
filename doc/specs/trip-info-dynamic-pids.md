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
| More than 18 grid PIDs | Rows grow down and overlap the bottom row | Text shrinks and columns are added within the same 3-row height; up to 72 PIDs at half size; the rest is cut |
| Ambient temp, atm pressure, dynamic selector | Queried, never drawn in the grid | Unchanged (status panel and theme only) |

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
     - **Bottom:** the user's selection in bottom-row order, or `TRIP_INFO_DEFAULT_BOTTOM_PIDS` when unset; only queried ids; at most `MAX_BOTTOM_ITEMS = 4`.
     - **Top:** the 16 former grid PIDs in their fixed order with their former formatting, then every other queried id in dialog order (unordered ids last, by id). Bottom ids and the not-drawn set are excluded.
   - `grid(itemCount)` returns columns, capacity and scale. At scale 1 the grid is 6 columns and the layout is unchanged; at most 18 items fit there. Above that, the scale steps down by 0.05 until `floor(6/s) × floor(3/s)` items fit, with a floor of 0.5 (12 × 6 = 72).
3. **`TripMetricDescriptor`** moved to `TripInfoMetrics.kt` and became id-based, with a `diff` flag for the odometer (`MetricsBuilder.buildDiff`). `BottomMetricDescriptor` was removed.
4. **`TripInfoDetails`** is now two lists of `TripInfoItem` (descriptor plus the current `Metric?`) instead of 21 named fields.
5. **`TripInfoSurfaceRenderer`** caches the plan. It rebuilds the plan when the queried ids change (compared element-wise against a `LongArray`, with no per-frame allocation), when the bottom-row pref or either order pref changes (`OnSharedPreferenceChangeListener`), or on `invalidate()`. Each frame it only refreshes `item.metric` from the collector.
6. **`TripInfoDrawer`** iterates the items. `TripInfoLayoutCache` also tracks the top item count and the `TripInfoGrid`, and `requiresLayoutUpdate` includes the top count. `maxItemWidth` uses `grid.columns`; `PerformanceDrawer` calls `drawMetric` without `drawScreen`, so it keeps 6 columns.
7. **`PidDefinitionViewModel` (`:app`).**
   - The `TripInfo` source is the defaults plus the current selection (unfiltered, so a selected PID hidden by a filter is not dropped on save), plus the filtered registry.
   - The new `TripInfoBottom` source is the PIDs in the main selection.
   - A shared `persistedSelection()` treats an unset bottom key as the default row, both for the checked state and for the "did it change" check on save. Opening and closing the dialog without changes therefore writes nothing.

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
| bottom PIDs that are not queried are skipped | Only queried ids are placed |
| grid is unchanged up to three full rows | 0–18 items: 6 columns, scale 1 |
| grid shrinks and adds columns to fit more items | 19–72 items fit, scale < 1, more than 6 columns |
| grid stops shrinking at the minimum scale | 500 items: scale 0.5, 12 columns, capacity 72 |

The existing `TripInfoQueryStrategyTest` (`:datalogger`) still passes. It pins the main pref key literal.

## Risks and verification

- **Dialog changes have no unit test.** `:app` has no JVM test setup, so `PidDefinitionViewModel` is verified manually.
- **The grid cuts PIDs beyond 72** with no "+N" indicator.
- **Small text at high counts.** At 0.5 scale on a small AA display, labels may be hard to read. The user's font-size setting still applies on top.
- **Phone shares the AA keys.** The bottom-row setting sits in the AA section but also affects phone Trip Info, the same as the existing PID selection.

Verification:

- [x] `./gradlew :screen_renderer:testDebugUnitTest` passes (11 tests)
- [x] `./gradlew :datalogger:testDebugUnitTest --tests '*TripInfo*'` passes
- [x] `./gradlew assembleGiuliaDebug` builds; `spotlessApply` applied
- [ ] Manual: with an existing selection, Trip Info on phone and AA looks identical to `master`
- [ ] Manual: open and close both PID dialogs without changes; nothing is persisted, layout unchanged
- [ ] Manual: select a PID outside the defaults; it appears after the default PIDs; drag reorder applies
- [ ] Manual: set, reorder and clear the bottom row on AA; the screen refreshes without reconnecting
- [ ] Manual: select more than 18 PIDs; the grid shrinks and does not overlap the bottom row

## Out of scope / follow-ups

- **Performance screen:** its layout is already driven by the profile's `pref.query.performance.top`/`bottom` lists. Only its dialog is still limited to those lists.
- **Paging** the grid via the AA virtual-screen actions instead of cutting beyond 72.
- Allowing ambient temp and atm pressure in the grid as an explicit opt-in.
