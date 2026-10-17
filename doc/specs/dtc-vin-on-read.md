# Spec: VIN read with every DTC action

2026-10-17 · Status: proposed, not implemented

Roadmap item 11 (`dtc-roadmap.md`). Follows `dtc-history.md` part A.

## Summary

**Problem.** The VIN that stamps a DTC read comes only from the connect init, and the init reads it only while
"Enable Automatic DTC readings" (`pref.adapter.init.fetchDTC`) is on (`AdjustmentsStrategy`:
`vehicleMetadataVinReadingEnabled(dtcEnabled)`). A read or clear started by the user works with that setting off, but is
stamped `vin=null`. History then files it under `UNKNOWN`, and `DtcHistoryStore.diff` gives `UNKNOWN` no markers: no
"New", no "No longer reported". The setting defaults to `false`, so a fresh install (or cleared app data) loses the
markers without any visible reason.

Seen on the car (2026-10-17, fresh install): `Found Vehicle metadata: {}` at connect. Read (17 codes) and clear (5 left)
were both stamped `vin=null`, and the 12 cleared codes were not shown under "No longer reported".

**Scope.** ObdMetrics (VIN request in the DTC action, VIN in the completion callback), `:datalogger` (stamp and
history use it). No UI change.

## Behaviour changes

| Area | Before | After |
|---|---|---|
| Manual read / clear, automatic DTC reading off | Stamped `vin=null`: no history markers | VIN read as the action's first request; stamped with it, markers shown |
| Manual read / clear, automatic DTC reading on | VIN from the connect init | VIN from the action (same car, same value); falls back to the connect VIN if the action got no VIN reply |
| Connect scan | VIN from the connect init | Unchanged (the new-code notification needs it) |
| ECM without a programmed VIN | `NO_VIN` when the connect init read it | `NO_VIN` from the action too |
| Profile without a VIN PID | `vin=null` | Unchanged, no extra request |
| Duration of a read / clear | — | One request longer (~100 ms) |

## Settings / keys touched

None. `pref.adapter.init.fetchDTC` keeps controlling the automatic read only; its summary needs no change.

## Implementation

* **ObdMetrics.**
  * `ExecutionContext.scheduleDTCAction` queues the VIN PIDs right after `DiagnosticTroubleCodeStartCommand`, before
    the modules: METADATA group, `getStable`, `VinPids.isVin` (one per query, `distinctByQuery`). Each with its
    own CAN header, through `CANMessageHeaderMultiplexer` as in `WorkflowBufferInitializer.queue`. The existing
    `commandProducer.resetHeader()` after the action covers the extra `ATSH`.
  * `DiagnosticTroubleCodeHandler` captures the VIN reply during the action, decoded as `MetadataReader` does at init
    (an empty VIN stays empty, the app maps it to `NO_VIN`). Reset by the start marker, like the codes.
  * `Lifecycle.onDTCCompleted(dtc, status, moduleStatus, vin)`: new default method calling the three-argument one,
    forwarded by `Subscription`. `vin` is null when no VIN PID answered or none is defined.
  * Open: whether a VIN that does not answer (NO DATA, `7F 22 31`) delays the action by its timeout. Check the logcat
    on a car whose ECM refuses `22 F190`.
* **App.**
  * `WorkflowOrchestrator.onDTCCompleted` overrides the four-argument callback and passes `vin` to
    `VehicleCapabilitiesManager.updateDTC`.
  * `updateDTC(..., vin)` stamps `DtcReadInfo` and the history scan with it, falling back to `getCurrentVin()` when
    null. Map an empty value to `NO_VIN` the same way `vinFrom` does (one helper, not a copy).
  * The connect-init VIN read (`vehicleMetadataVinReadingEnabled`) stays as it is.

## Backward compatibility

* ObdMetrics: new default method; existing `Lifecycle` implementers keep getting the three-argument call.
* App: scans stored under `UNKNOWN` before the fix stay there; new scans go under the VIN. The first scan after the fix
  has no earlier scan of that VIN to compare with, so its markers start with the second one.

## Tests

* ObdMetrics `DiagnosticTroubleCodeReadingTest`: the VIN request is queued before the modules for a read and for a
  clear; the VIN reaches `onDTCCompleted`; no VIN PID in the profile → no extra request, `vin` null; an empty VIN
  reply → empty value; module scan results are unchanged.
* App `VehicleCapabilitiesManagerTest`: `updateDTC` with a VIN and no connect metadata stamps read info and history
  with it; null falls back to the connect VIN; empty → `NO_VIN`. A read then a clear with the automatic read off gives
  "No longer reported" for the cleared codes (`DtcHistoryDiff`).

## Risks and verification checklist

* A DTC action sent while the car is off answers nothing; the VIN request adds one timeout to an already failing
  action.
* [ ] Automatic DTC reading off, fresh install: read, clear; read info shows the VIN (or "VIN not programmed"), the
  cleared codes appear under "No longer reported".
* [ ] Logcat: one `22 F190` per action, before the first module's `ATSH`.
* [ ] Live data resumes on the right header after the action.

## Out of scope

* Reading the VIN when automatic DTC reading is off for the connect scan (there is no connect scan then).
* Moving `UNKNOWN` scans under the VIN once it is known.
