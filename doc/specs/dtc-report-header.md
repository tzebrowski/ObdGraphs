# Spec: shared report header

2026-10-19 · branch `feat/dtc-report-header` (ObdGraphs only) · Status: implemented, on-device checks pending

Roadmap item 9, first part (`dtc-roadmap.md`).

## Summary

**Problem.** The shared DTC report started with the codes. A mechanic receiving it could not tell when the list was
read, from which car, or whether the scan was complete. Only the modules that did not answer were listed.

**Scope.** `:datalogger` (action duration, stored with the read info), `:app` (report header, EN + PL strings).
Odometer and history export are not part of it (see Out of scope).

## Behaviour changes

| Area | Before | After |
|---|---|---|
| Shared report, top | "Modules without an answer: …" (when any) | "Last read: <date, time> · VIN …" (the dialog's read info line), "Scan duration: 13.4 s", then the modules without an answer |
| List stored before this change | — | No duration line; read info line as before |
| No read info at all | — | No header lines (no "N/A") |

## Settings / keys touched

`pref.datalogger.dtc.read_info` (`DtcReadInfo`) gains `durationMs` (nullable). No new keys.

## Implementation

* `DtcActionQueue.complete()` returns `DtcCompletion.durationMs`: now minus the time the finished request was *sent*
  (`inFlightSince`), so a request waiting behind another does not count its wait. Null for a completion nobody asked for.
* `WorkflowOrchestrator.onDTCCompleted` passes it to `VehicleCapabilitiesManager.updateDTC(durationMs)`, which stores it
  in `DtcReadInfo`. Logged as `duration=…ms` on the "DTC action completed" line.
* `dtcReportHeader()` (`app/.../preferences/dtc/DtcReport.kt`) builds the header lines, leaving out unknown parts.
  The duration is formatted with the locale (`13,4 s` in PL).
* A read / clear of some modules keeps the other modules' codes (`DtcStoredList.mergedOver`), but the duration is that
  of the last action only.

## Backward compatibility

Read info stored before has no `durationMs`; Jackson reads it as null and the line is left out. History
(`dtc_history.json`) is unchanged.

## Tests

* `DtcActionQueueTest`: duration from send to completion, a waiting request's time starts when it is sent, null for an
  unrequested completion.
* `VehicleCapabilitiesManagerTest`: `updateDTC` stores the duration; old read info reads as null.
* `DtcReportTest` (`:app`): header lines, unknown parts left out, PL decimal separator, `dtc.share.duration` takes a
  decimal in EN and PL.

## Risks and verification checklist

* [ ] Share after a module scan: header shows read time, VIN and a plausible duration (~13 s for 13 modules).
* [ ] Share after a clear: duration covers the clear plus its read.
* [ ] Share a list stored before the update: no duration line, no crash.
* [ ] PL locale: "Czas skanowania: 13,4 s".

## Out of scope

* Odometer at read time: the 2.0 GME's snapshot (`1008` = operating time) and extended data (`6080` / `6081`) carry
  none; it would need live data (PID 7076) or an extra read per DTC action in ObdMetrics. Left out by the user's choice.
* History export as JSON / CSV: roadmap item 9, follow-up.
