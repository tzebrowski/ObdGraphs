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

## Settings / keys touched

| Key | Type | Notes |
|---|---|---|
| `pref.datalogger.dtc.read_info` | String (JSON `{timestamp, vin}`) | New, written with every DTC store (`updateDTC`): user reads/clears and the automatic read on connect. Absent = never stamped. |

## Implementation

* `datalogger/.../dtc/DtcReadInfo.kt` (pure, unit-tested):
  * `DtcReadInfo(timestamp, vin)`, `isFromOtherVehicle(currentVin)` (both known and different).
  * `vinFrom(metadata)`: the metadata map is keyed by the metadata PID's label, which differs per
    profile, and the decoded value may carry padding. So it takes the first key containing "vin"
    whose value holds a 17-character ISO 3779 VIN (no I/O/Q).
  * `summary()` / `vehicleLine()` build the line from localized format strings (`DtcReadInfoLabels`);
    the parts are joined in code because string resources lose a leading space.
  * `DtcClearOutcome` + `dtcClearOutcome(status, codesAfterClear)`: `ERR` → REFUSED; otherwise any
    code read back → CODES_REMAIN; else CLEARED. The read that follows every clear is the verdict,
    so the outcome works with today's ObdMetrics (always `NO_DATA`) except for REFUSED.
* `VehicleCapabilitiesManager.updateDTC` stamps `DtcReadInfo(now, getCurrentVin())`. Metadata is
  rewritten on every connect before that connection's reads complete, so the VIN is the connected
  vehicle's. `getDtcReadInfo()` returns null when unset or malformed.
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

* `datalogger/src/test/.../dtc/DtcReadInfoTest.kt`: VIN parsing (label variants, padding, invalid
  values), other-vehicle rule, summary text, clear outcome for every status/read-back combination.
* `VehicleCapabilitiesManagerTest`: `updateDTC` stamps time + VIN, no VIN when none reported,
  `getDtcReadInfo` null when unset/malformed.
* ObdMetrics `DiagnosticTroubleCodeReadingTest`: `scheduledClearReportsTheClearStatus` (OK/ERR, with and
  without modules; fails on the unfixed handler) and `scheduledReadReportsNoClearStatus`.

## Risks and verification checklist

- [ ] Phone: after a read, the grey line shows date/time and the VIN; it's absent on a fresh install.
- [ ] Phone + AA: connect to a different vehicle (or edit the stored VIN): "another vehicle" appears.
- [ ] Clear with the engine running on an ECU that refuses: "refused" toast (after the ObdMetrics bump).
- [ ] Clear with an active fault: "still reported after clearing: N".
- [ ] Clear that succeeds: "Trouble codes cleared." and an empty list.
- [ ] AA (DHU): read info row first; with module sections, a separate "Last read" section.
- [ ] PL: line and toasts fit (phone dialog width, AA toast).
- Risk: on AA the read info row takes one of the rows the host allows while driving.
- Risk: `vinFrom` depends on the metadata label mentioning "VIN". A profile whose VIN PID is labelled
  otherwise gets no VIN (line without VIN, never a false "another vehicle").

## Out of scope

Per-module clear results in the UI (the API reports one aggregated status), keeping a history of past
reads, and a "time since codes cleared" PID display.
