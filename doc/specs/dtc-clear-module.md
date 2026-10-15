# Spec: clear one module, warn when the engine runs

2026-10-15 · branch `feat/dtc-clear-module` (ObdGraphs only) · Status: implemented, on-device checks pending

Roadmap item 5 (`dtc-roadmap.md`).

## Summary

**Problem.**
* Clearing one module meant opening the Clear picker and unticking every other module. Afterwards the list held that
  module alone: `updateDTC` replaced the stored list with the clear's follow-up read, which covers only the cleared
  module, so the other modules' codes vanished from the screen although they were still in the car.
* Most modules refuse a clear with the engine running (`7F 14 22`). The app reported the refusal only afterwards.

**Scope.** `:datalogger` (list merge, engine state, list item), `:app` (module clear row, dialogs), `:automotive`
(warning in the clear confirmation), EN + PL.

## Behaviour changes

| Area | Before | After |
|---|---|---|
| Expanded module with codes (phone) | Header + codes | Also a small outlined "Clear this module" button right under its header, above the codes (at the bottom it went off screen with many codes). Red only in its outline and icon: codes are red text |
| Only one module has codes | — | The button shows too. Hiding it then (tried) made the user think it was gone |
| DTC dialog Close button | Label one line higher than the other buttons (`dialog_close_window_action` is "Close\n ", two lines for the trip dialog) | Own one-line label `dtc.action_close`, also on the "How it works" dialog |
| Tap on that row | — | Confirmation: only this module's codes and freeze frames are erased, the app keeps a copy in history, engine off / ignition on, readiness monitors reset if it is the engine module. "Clear" clears that module alone, no picker |
| Clear / read of some modules | The list shows only those modules | Those modules are refreshed; the other modules keep their last codes and statuses |
| Connect scan, default-ECU read, another vehicle, no VIN | List replaced | Unchanged: list replaced |
| Clear (button or module row) with the engine running | Sent; refusal toast afterwards | Phone: "The engine is running" dialog (Clear anyway / Cancel) before the picker; module row: the warning heads its confirmation. AA: the warning heads the clear confirmation |
| Engine state unknown (no engine PID in the running query, reading older than 10 s) | — | No warning; nothing is blocked |
| Default ECU, "Unknown module", a label no table entry scans | — | No clear row |

## Settings / keys touched

None new. `pref.datalogger.dtc`, `.scanned_modules`, `.module_status` now hold the merged list after a targeted
read / clear; `pref.datalogger.dtc.read_info` is the time of the latest read, also for kept modules.

## Implementation

* `dtc/DtcStoredList.kt`: `DtcStoredList.mergedOver(previous, kind, sameVehicle)`. Merges only for `READ` / `CLEAR`
  of named modules over a module list of the same known vehicle (`isSameVehicle`: same `historyKey`, never
  `UNKNOWN`). Kept = previous scanned modules not targeted now; their codes and statuses are copied. A `CONNECT` scan
  covers every configured module and replaces the list, so a module removed from the table does not linger.
* `VehicleCapabilitiesManager.updateDTC` stores the merged list; the history scan (`DtcHistoryStore.append`) records
  the read alone, so "new" / "no longer reported" still compare what was actually scanned.
* `dtc/EngineState.kt`: `EngineStateTracker`, a `MetricsProcessor` registered on `WorkflowOrchestrator`'s
  `MetricsObserver`. It keeps the last engine speed (`ENGINE_SPEED_PID_ID` 13 or GME `EXT_ENGINE_SPEED_PID_ID` 7008,
  running above 300 rpm) or vehicle status (`engine.running`) reading. `engineRunning()` is true / false only for a
  reading younger than `ENGINE_STATE_MAX_AGE_MS` (10 s), else null. Reset on connect and stop.
* `DtcListItem.ModuleClear(module)`, added by `withModuleClearActions(clearable)` after `collapsedBy` (phone only; AA
  ignores it). `dtcModuleRequestKey(mappings, module)` maps a code's module label back to the table entry
  `dtcScanTargets` scans it with; null means no row.
* Phone: `item_dtc_module_clear.xml` (start-aligned under the header name, outlined pill `dtc_module_clear_background.xml`
  inset to ~32 dp inside a 48 dp touch target, red outline and bin icon, primary text colour; content description adds
  the module name), `ModuleClearViewHolder`;
  `DiagnosticTroubleCodePreferenceDialogFragment.clearModule` (confirmation, then `scheduleDTCCleanup(setOf(key))`
  through the same `startClear` as the Clear button) and `confirmEngineOff` in front of the Clear picker.
* AA: `DtcClearConfirmationScreen` puts `dtc.engine_running_warning` above the confirmation text.

## Backward compatibility

* Lists stored before this change merge normally (they carry scanned modules and read info).
* A list without read info, a default-ECU list or another vehicle's list is replaced as before.

## Tests

* `DtcStoredListTest`: one-module clear keeps the others' codes and statuses; the targeted module comes from the new
  read only; connect / other vehicle / no previous / default read replace; same-vehicle rule.
* `VehicleCapabilitiesManagerTest`: end to end through prefs: connect with two modules, clear one → the other's code
  stays, history records the clear alone; the next connect replaces the list. Fails without the merge (list empty).
* `EngineStateTest`: reading age limit; RPM (both PIDs) and vehicle status set the state, other PIDs do not, stop
  resets it.
* `DtcListItemTest`: clear row right under the header of expanded modules with codes that map to a table entry; also
  when it is the only module with codes; `dtcModuleRequestKey` for shared headers, header-less and unknown labels.

## Risks and verification checklist

* The read info says "last read <time>" for the whole list; kept modules were read earlier. The connect scan
  refreshes everything on the next connect.
* The warning trusts the running query: with no engine PID in it, there is no warning (the confirmation text still
  says engine off / ignition on).
* [ ] Phone: the button under each expanded header with codes; Close label centred; PL text at phone width.
* [ ] Clear one module on the car: only its codes go (to "No longer reported"), other modules keep theirs.
* [ ] Engine running: warning dialog before the picker, and in the module confirmation; Clear anyway still clears.
* [ ] AA (DHU): warning above the clear confirmation with the engine running.

## Out of scope

* Blocking the clear with the engine running (the reading may be stale or missing).
* A per-module "last read" time.
* Module clear on AA.
