# Spec: "Read freeze frame data" in the module picker

2026-10-20 · branch `feat/dtc-picker-snapshots` · Status: implemented, on-device check pending

## Summary

**Problem.** Freeze frames (snapshots) are read only when the "Read freeze frame data" setting
(`pref.dtc.read_snapshots`, default off) is on. To get them for one read, the user had to go to settings, turn it on,
read, and turn it off again.

**Scope.** Phone DTC dialog, Refresh flow only. A checkbox in the module picker chooses freeze frames for that read.
ObdMetrics is unchanged: the read sends `READ_SNAPSHPOTS` or `READ`, as the setting did.

## Behaviour changes

| Area | Before | After |
|---|---|---|
| Module picker, Refresh | Modules only | Also a "Read freeze frame data" checkbox, ticked as the setting is. Applies to this read only; not saved |
| Module picker, Clear | Modules only | Unchanged (no checkbox; the clear's read follows the setting) |
| Refresh with no modules configured (no picker) | Setting decides | Unchanged |
| Connect scan, AA read / clear | Setting decides | Unchanged |
| Freeze frames in the expanded row and the shared report | Shown only with the setting on | Shown whenever the code has them (they exist only if they were read) |

## Settings / keys touched

None. `pref.dtc.read_snapshots` stays the default. New service intent extra `org.obd.graphs.logger.DTC.read_snapshots`
(absent = the setting decides).

## Implementation

* `DtcRequest.snapshots: Boolean?` (null = setting) and `DtcRequest.readAction(snapshotsSetting)`
  (`dtc/DtcActionQueue.kt`). `WorkflowOrchestrator.send()` uses it instead of the old `getReadDtcAction()`.
* `DataLoggerService.scheduleDTCRead(modules, snapshots)` carries the choice as an intent extra.
* `pickDtcModules(snapshotsOption = true)` shows `dtc_picker_snapshots` (`dialog_dtc_module_picker.xml`) and passes
  its state to `onPicked`. The checkbox reuses the setting's label and summary (`pref.dtc.read_snapshots[_summary]`), set
  off from the list by a divider. The list height cap
  (`MaxHeightListView`) measures siblings, so the checkbox is accounted for.
* The adapter and the share report no longer check the setting before showing a snapshot. Otherwise a read with the
  box ticked and the setting off read the freeze frames and hid them.

## Backward compatibility

The request queue, the stored list and the history are unchanged. With the checkbox left as it starts, a read behaves
as before. Snapshots stored by an earlier read now show even if the setting was turned off later.

## Tests

`DtcActionQueueTest`: the picker's choice beats the setting either way; without a choice the setting decides.

## Risks and verification checklist

* [ ] Phone, setting off: Refresh, tick the box, read: freeze frames in a code's expanded row and in the report.
* [ ] Phone, setting on: untick the box: no `19 04` in the logcat.
* [ ] Clear picker shows no checkbox; PL picker (box, divider, hint line) fits a phone screen.

## Out of scope

* A checkbox on AA (it has no picker).
* Freeze frames for one code on demand.
