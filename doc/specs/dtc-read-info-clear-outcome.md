# Spec: DTC read info and clear outcome

2026-10-12 · branch `feat/dtc-read-info-clear-status` · Status: implemented, manual checks pending

## Summary

**Problem.** The DTC list (phone dialog and AA screen) shows the last *stored* codes with no hint of
their age or origin: they may be weeks old or come from another vehicle. A clear gave no verdict
either. ObdMetrics reported `NO_DATA` for every user-triggered clear, so an ECU refusing it (typically
`7F 14 22`, conditions not correct, engine running) looked like success. The list simply reloaded, and
codes that came straight back from an active fault were not called out.

**Scope.** `:datalogger` (read info store, VIN parsing, clear outcome, all shared), `:app` (phone
dialog line + toasts, EN/PL), `:automotive` (AA row + toasts, EN/PL). ObdMetrics `feat/dtc-clear-status`
reports the real clear status (needs an `obdMetricVersion` bump here for the "refused" case).

## Behaviour changes

| Area | Before | After |
|---|---|---|
| Phone DTC dialog, top | Code / Description header only | Grey line "Last read: 12 Oct 2026, 09:30 · VIN ZAR…" above it |
| AA DTC list | Codes only | Leading row "Last read: …" with the VIN on its second line; with module sections, its own "Last read" section |
| Codes from another vehicle | Indistinguishable | "… · another vehicle (VIN …)" when the stored and current VIN are both known and differ |
| Codes stored before this change | — | No read info line (age unknown, not shown as "now") |
| Clear acknowledged, nothing read back | List reloads silently (phone) / "Trouble codes updated." (AA) | "Trouble codes cleared." |
| Clear, codes read back afterwards | Same as above | "Trouble codes still reported after clearing: N. The fault may still be present." |
| Clear refused by the ECU | Same as above | "The vehicle refused to clear the trouble codes. Turn the engine off, leave the ignition on and try again." (needs the ObdMetrics change) |
| Read | Unchanged | Unchanged (AA: "Trouble codes updated.") |
| Automatic read on connect (`pref.adapter.init.fetchDTC`), modules configured | ObdMetrics read the default header only; the flat, untagged result **replaced** the last grouped scan, and its codes leaked into the next scan as "Unknown module" | The app scans **all** configured modules (one per header) once connected; the list stays grouped. "DTC available" toast unchanged |
| Automatic read on connect, no modules configured | Default-header read | Unchanged |

## Settings / keys touched

| Key | Type | Notes |
|---|---|---|
| `pref.datalogger.dtc.read_info` | String (JSON `{timestamp, vin}`) | New, written with every DTC store (`updateDTC`): user reads/clears and the automatic read on connect. Absent = never stamped. |

## Implementation

* `datalogger/.../dtc/DtcReadInfo.kt` (pure, unit-tested):
  * `DtcReadInfo(timestamp, vin)`, `isFromOtherVehicle(currentVin)` (both known and different).
  * `vinFrom(metadata)`: the metadata map is keyed by the metadata PID's *label*, which differs per
    profile and is translated (`giulia_2.0_gme.json` 22F190 is "Vehicle Identification Number", in
    Polish "Numer identyfikacyjny pojazdu"). So a key containing "vin" wins (value may be padded),
    otherwise any value that is exactly a 17-character ISO 3779 VIN (no I/O/Q) mixing letters and digits.
  * The VIN exists only if the vehicle metadata is read on connect
    (`pref.adapter.init.fetchDeviceProperties`, "fetch device properties"). With it off, the
    metadata is `{}` and the line shows the time only.
  * `summary()` / `vehicleLine()` build the line from localized format strings (`DtcReadInfoLabels`);
    the parts are joined in code because string resources lose a leading space.
  * `DtcClearOutcome` + `dtcClearOutcome(status, codesAfterClear)`: `ERR` → REFUSED; otherwise any
    code read back → CODES_REMAIN; else CLEARED. The read that follows every clear is the verdict,
    so the outcome works with today's ObdMetrics (always `NO_DATA`) except for REFUSED.
* `VehicleCapabilitiesManager.updateDTC` stamps `DtcReadInfo(now, getCurrentVin())`. Metadata is
  rewritten on every connect before that connection's reads complete, so the VIN is the connected
  vehicle's. `getDtcReadInfo()` returns null when unset or malformed.
* Read on connect: `AdjustmentsStrategy` turns ObdMetrics' `vehicleDtcReadingEnabled` off whenever
  `dtcConnectScanModules(mappings)` is non-empty (every module with a header; the phone picker's
  deselection applies to manual scans only, by the user's choice). `WorkflowOrchestrator.onRunning`
  then calls `scheduleConnectScan`. ObdMetrics only counts the workflow as running once all its threads
  are up (after `onRunning` returns), so it waits on `workflow.isRunning` (max 10 s) off the callback
  thread before `scheduleDTCRead(modules, connectScan = true)`. Its completion raises
  `DATA_LOGGER_DTC_AVAILABLE` like the old read. `updateCapabilities(moduleScanOnConnect = true)` no
  longer stores the (empty) init codes.
* Leak in ObdMetrics (all versions): `DiagnosticTroubleCodeHandler` accumulates *every* DTC read reply,
  including the init read's, and hands them out with the next scheduled read. With the init read off
  whenever modules are configured, the app no longer triggers it. Fixed upstream on
  `feat/dtc-clear-status` too: every scheduled DTC action starts with a `DiagnosticTroubleCodeStartCommand`
  that drops whatever the handler accumulated before it (init read, a timed-out earlier action).
  Test: `scheduledReadDoesNotReturnCodesFromTheInitRead` (fails on `v11.x`).
* `WorkflowOrchestrator`: `pendingDtcClear` marks a scheduled clear; `onDTCCompleted` then sends
  `DATA_LOGGER_DTC_ACTION_COMPLETED` with the `DtcClearOutcome` as the broadcast extra. A read sends it
  without one.
* Phone `DiagnosticTroubleCodePreferenceDialogFragment`: `dtc_read_info` TextView (gone when null),
  toast per outcome. AA `DiagnosticTroubleCodesScreen`: read info row, toast per outcome.
  Time is formatted with `DateUtils.formatDateTime` (locale-aware).

### ObdMetrics (`feat/dtc-clear-status`, from `v11.x`)

* `DiagnosticTroubleCodeHandler` keeps the latest clear reply per module and reports, for the modules
  the clear targeted: `ERR` if any refused, `OK` if the rest acknowledged it, `NO_DATA` if none answered
  or no clear was scheduled.
* Keyed by module because the handler also sees the automatic clear during init. Without the key,
  that stale reply could stand in for the user's clear.
* `ExecutionContext` tags per-module clear commands with the module (as reads already were) and passes
  the module labels to `DiagnosticTroubleCodeScheduleCommand` (new constructor; the old one stays).

## Backward compatibility

Existing stored codes keep working; without `read_info` the line is simply not shown. Older ObdMetrics
(`NO_DATA`) degrade to CLEARED / CODES_REMAIN, never to a false "refused". The broadcast action is
unchanged; receivers that ignore the extra behave as before.

## Tests

* `DtcListItemTest`: `dtcConnectScanModules` takes every module with a header, ignoring the picker.
* `AdjustmentsStrategyTest`: no ObdMetrics DTC read on connect when modules are configured (fails on
  the old strategy); the automatic clear is unaffected.
* `VehicleCapabilitiesManagerTest`: `updateCapabilities(moduleScanOnConnect = true)` stores nothing and
  raises no "DTC available".

* `datalogger/src/test/.../dtc/DtcReadInfoTest.kt`: VIN parsing (label variants, padding, invalid
  values), other-vehicle rule, summary text, clear outcome for every status/read-back combination.
* `VehicleCapabilitiesManagerTest`: `updateDTC` stamps time + VIN, no VIN when none reported,
  `getDtcReadInfo` null when unset/malformed.
* ObdMetrics `DiagnosticTroubleCodeReadingTest`: `scheduledClearReportsTheClearStatus` (OK/ERR, with and
  without modules; fails on the unfixed handler) and `scheduledReadReportsNoClearStatus`.

## Risks and verification checklist

- [ ] Phone: after a read, the grey line shows date/time and the VIN (metadata reading on); it's absent on a fresh install.
- [x] Phone, metadata reading off (`fetchDeviceProperties=false`): time only, no VIN (seen on device 2026-10-12).
- [ ] Phone + AA: connect to a different vehicle (or edit the stored VIN): "another vehicle" appears.
- [ ] Clear with the engine running on an ECU that refuses: "refused" toast (after the ObdMetrics bump).
- [ ] Clear with an active fault: "still reported after clearing: N".
- [ ] Clear that succeeds: "Trouble codes cleared." and an empty list.
- [ ] AA (DHU): read info row first; with module sections, a separate "Last read" section.
- [ ] PL: line and toasts fit (phone dialog width, AA toast).
- [ ] Automatic DTC reading on, modules configured: after connecting, the list is grouped by module
      (no "Unknown module", no flat list); "Last read" shows the connect time.
- [ ] Automatic DTC reading on, no modules configured: flat default-ECU list as before.
- Risk: on AA the read info row takes one of the rows the host allows while driving.
- Risk: the value fallback could take another 17-character letters+digits metadata value for a VIN.
  None of the bundled profiles has such a field. A wrong VIN could cause a false "another vehicle".

## Out of scope

Per-module clear results in the UI (the API reports one aggregated status), keeping a history of past
reads, and a "time since codes cleared" PID display.
