# Spec: VIN read with every DTC action

2026-10-17 · Status: implemented 2026-10-18 (ObdMetrics + app, branch `feat/dtc-vin-on-read`)

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
| Connect scan | VIN from the connect init | VIN from the action (read moments after the init, same value); falls back to the connect VIN |
| ECM without a programmed VIN | `NO_VIN` when the connect init read it | `NO_VIN` from the action too |
| Profile without a VIN PID | `vin=null` | Unchanged, no extra request |
| Duration of a read / clear | — | One request longer (~100 ms) |

## Settings / keys touched

None. `pref.adapter.init.fetchDTC` keeps controlling the automatic read only; its summary needs no change.

## Implementation

* **ObdMetrics.**
  * `ExecutionContext.queueVin`, called by `scheduleDTCAction` right after `DiagnosticTroubleCodeStartCommand`, before
    the modules: METADATA group, `getStable`, `VinPids.isVin` (one per query, `distinctByQuery`). Each request gets an
    explicit `ATSH` from the init's header table (`CommandProducer.headerFor` → `CANMessageHeaderMultiplexer.headerFor`,
    keyed by `canMode`, else mode: `22` → the ECM). Not `switchHeader`: it skips a header it believes is set, and in
    single-header mode sends one only once. The existing `commandProducer.resetHeader()` after the action covers the
    extra `ATSH`. No header in the table: the request goes on the header in use, as at init.
  * **VIN PIDs are marked in the resource files**, `"category": "vin"` (`mode01.json` 11000 = 09 02,
    `giulia_2.0_gme.json` 17001 = 22 F190). `VinPids.isVin` checks the category; the hard-coded 09 02 / 22 F190 match
    is gone, in ObdMetrics and in the app (`isVinRequest` removed, the app calls `VinPids.isVin`). Descriptions are
    no use: they are translated. `mode09.json`'s 09 02 is livedata and stays unmarked.
  * `DiagnosticTroubleCodeHandler` captures the VIN reply during the action (`METADATA` + `VinPids.isVin`, now public
    for the `executor` package and the app), trimmed; an empty VIN stays empty, the app maps it to `NO_VIN`. With both 09 02 and
    22 F190 defined, a real VIN wins over an empty one. Reset by the start marker, like the codes.
  * `Lifecycle.onDTCCompleted(dtc, status, moduleStatus, vin)`: new default method calling the three-argument one,
    forwarded by `Subscription`. `vin` is null when no VIN PID answered or none is defined.
  * Open: whether a VIN that does not answer (NO DATA, `7F 22 31`) delays the action by its timeout. Check the logcat
    on a car whose ECM refuses `22 F190`.
* **App.**
  * `WorkflowOrchestrator.onDTCCompleted` overrides the four-argument callback and passes `vin` to
    `VehicleCapabilitiesManager.updateDTC`.
  * `updateDTC(..., vin)` stamps `DtcReadInfo` and the history scan with `vinFromReply(vin) ?: getCurrentVin()`, for
    every kind of action. `vinFromReply` (`DtcReadInfo.kt`) shares `isEmptyVin` and the VIN pattern with `vinFrom`:
    null → null, blank / zero filled → `NO_VIN`, a value holding no VIN → null (falls back).
  * The connect-init VIN read (`vehicleMetadataVinReadingEnabled`) stays as it is.

## Backward compatibility

* A VIN PID without `"category": "vin"` (a profile file outside ObdMetrics' bundled ones) is no longer recognised:
  no VIN read at connect or with a DTC action, reads stamped `UNKNOWN`. Bundled profiles are marked.
* ObdMetrics: new default method; existing `Lifecycle` implementers keep getting the three-argument call.
* App: scans stored under `UNKNOWN` before the fix stay there; new scans go under the VIN. The first scan after the fix
  has no earlier scan of that VIN to compare with, so its markers start with the second one.

## Tests

* ObdMetrics `DiagnosticTroubleCodeReadingTest`: `dtcActionReadsTheVinBeforeTheModules` (read and clear: one
  `22F190`, right after `ATSHDA10F1`, before the first module's `ATSH`); `dtcActionReportsTheVin` (VIN / empty → `""`
  / no reply → null); `profileWithoutVinPidSendsNoVinRequest`. The existing module tests pass unchanged. `VinPidsTest`: the marked PIDs are
  VIN PIDs; 22 F18C and the unmarked 09 02 of `mode09.json` are not.
* App `VehicleCapabilitiesManagerTest`: `updateDTC` with a VIN and no connect metadata stamps read info and history
  with it; null falls back to the connect VIN; empty → `NO_VIN`; a read then a clear without the connect VIN lists the
  cleared code as gone. `DtcReadInfoTest`: `vinFromReply`.

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
