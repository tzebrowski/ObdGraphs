# Spec: connect from the DTC dialog, legacy entry unticked by default

2026-10-21 · branch `feat/dtc-connect-from-dialog` · Status: implemented, on-device check pending

## Summary

**Problem.**
1. The first time the module picker opens (Refresh / Clear), every table entry is ticked, "OBD-II (Legacy Mode 01)"
   (`DB33F1`) included. Scanned with UDS, its answer only repeats the Engine ECU's codes.
2. With no connection, Refresh / Clear only toast "Please connect first": the user has to leave the dialog, go back to a
   metrics screen, tap connect and come back.

**Scope.** Phone DTC dialog and the bundled profiles. AA already connects from its DTC screen. ObdMetrics unchanged.

## Behaviour changes

| Area | Before | After |
|---|---|---|
| Module picker, first open | All entries ticked | Legacy Mode 01 unticked (profile default); the user's later picks are kept as before |
| AA read / clear | Scans every entry the picker left ticked | Same; with the new default that leaves out Legacy Mode 01 |
| Refresh / Clear / "Clear this module" with no connection | Toast "Please connect first" | Connects (Refresh shows "Connecting…"), then goes on with the action |
| … with automatic reading on connect on | Toast | Connects; Refresh stops there (the connect reads the codes), Clear goes on |
| Connect fails / adapter not set / stopped | – | Refresh button back to normal, nothing runs |

## Settings / keys touched

* Every bundled profile with the legacy entry: `profile_N.pref.dtc.module_picker.deselected=[01]` (its request key).
* New string `dtc.action_connecting` (EN / PL).

## Implementation

* Profiles preset the existing picker key `PREF_DTC_DESELECTED_MODULES`; no code. The profile loader adds keys missing
  from the stored prefs on update, and copies an array key of the current profile to the unprefixed key.
* `DiagnosticTroubleCodePreferenceDialogFragment.whenConnected(isRead, action)`: runs the action when connected, else
  stores it in `afterConnect` and starts the data logger with `ROUTINES_QUERY` (the idle query AA's DTC screen uses; a
  live screen switches the query when it opens). Not started again while the status is already `Connecting`.
* `DATA_LOGGER_CONNECTED_EVENT` refreshes the list and runs `afterConnect`. The connect / stop / error events drop it
  (`resetActionState`).
* `dtcRunsAfterConnect(isRead, readsOnConnect)` (`dtc/DtcActionQueue.kt`): a read is skipped when automatic reading on
  connect is on. A clear opened during the connect scan waits in `DtcActionQueue` (the scan's overlay covers the picker).

## Backward compatibility

* Existing installs: the new profile key is added on the next profile update. For the current profile it also
  overwrites a picker selection the user stored before, once (the picker then starts with all but the legacy entry).
* A user who wants the legacy entry ticks it once; the choice is stored as before.

## Tests

* `DtcProfileDefaultsTest` (`:app`): every bundled profile with a "Legacy Mode 01" entry presets its request key as
  deselected. Fails without the profile change.
* `DtcActionQueueTest`: `dtcRunsAfterConnect` for read / clear, with and without the read on connect.

## Risks and verification checklist

* [ ] Fresh install: Refresh picker shows Legacy Mode 01 unticked; AA read skips `DB33F1` (logcat `ATSH`).
* [ ] Disconnected: Refresh → "Connecting…" → connects → picker opens → read works.
* [ ] Disconnected, automatic reading on: Refresh connects, the connect scan fills the list, no second picker.
* [ ] Disconnected: Clear and "Clear this module" connect, then show their confirmation.
* [ ] Adapter off: Refresh comes back from "Connecting…" after the connect error.
* [ ] After a connect from the dialog, a gauge screen shows live data (query switched back).

## Out of scope

* Removing the legacy entry from the table (used by Mode 01 live PIDs).
* A connect button of its own in the dialog.
