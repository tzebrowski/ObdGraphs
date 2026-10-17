# Spec: OBD-II trouble code services per module

2026-10-17 · branches `feat/dtc-obd-modes` (ObdGraphs + ObdMetrics) · Status: implemented, on-device checks pending

Roadmap item 3 (`dtc-roadmap.md`).

## Summary

**Problem.** A car without UDS answers trouble-code requests only with the OBD-II services: 03 (stored), 07 (pending),
0A (permanent), 04 (clear). The scan sent UDS `19 02` / `14` to every module, so such a car showed no codes, and
permanent codes (mandatory in the US since 2010) were never shown.

**Scope.** ObdMetrics (requests as PID data, protocol per module), `:datalogger` (state, clear outcome, scan
modules), `:app` (chip, setting), `:automotive` (status line, clear outcome), EN + PL.

**Which modules get the OBD-II services is configuration, never inferred.** The first version picked them by header
(`7DF` / `DB33F1` / `18DB33F1`, hard-coded). Rejected by the user: an ECU may answer OBD-II on a physical address
(`7E0`), a non-CAN car has no such header, and a manufacturer may answer UDS on the broadcast address.

## Behaviour changes

| Area | Before | After |
|---|---|---|
| Module set to OBD-II in the module picker (UDS / OBD-II per row) | UDS `19 02` / `14` | 03 + 07 + 0A, clear 04; a code reported by two services is one code with both states |
| Any other module, whatever its header | UDS | UDS (unchanged) |
| Code from 0A | — | "Permanent" chip beside the state (phone), "Permanent" in the AA status line, counted in the module header |
| Clear leaves only permanent codes | "Codes remain" | "Cleared. Permanent codes stay: N" (phone + AA) |
| Read on connect (automatic scan) | Every configured module, OBD-II ones included: an OBD-II entry reaching the ECM listed its codes again as a second ECM section | OBD-II modules skipped while any UDS module is configured; a table of OBD-II modules only is still scanned |
| Row switched to OBD-II in the picker | Check box unchanged | Check box cleared (user can tick it again for a manual read / clear) |
| Bundled profiles | — | No module marked: unchanged. On the 2.0 GME, `DB33F1` answered 03 / 07 with the ECM's codes (each listed twice) and 0A with NO DATA (car test, 2026-10-16) |

## Settings / keys touched

* `pref.dtc.obd_modules` (string set of module-table `requestKey`s, no default = none): set by the UDS / OBD-II buttons on
  each row of the DTC dialog's module picker ("Select Modules to Scan", shown for Read and Clear), saved on confirm.
  Saved per profile like every preference. No entry in the settings screen.
* **Where the protocol choice lives (user's choice):** first a separate "OBD-II modules" list in the DTC settings. Rejected: two
  module lists in two places, one choosing *which* modules are scanned, the other *how*; the user asked what the
  settings one was for. The protocol now sits on the picker row next to the scan check box, as a UDS / OBD-II
  toggle group (user's choice: a lone "OBD-II" switch did not say that the alternative is UDS).

## Implementation

* **ObdMetrics.** `Init.Header.dtcProtocol` (`Init.DtcProtocol.UDS` default / `OBD`). The requests are data:
  `dtcObdRead` (03 / 07 / 0A, ids 21020–21022) and `dtcObdClear` (04, id 21023) in `mode01.json`, groups
  `PIDsGroup.DTC_OBD_READ` / `DTC_OBD_CLEAR`, codec `ObdDtcReadCodec` (answer code = the PID's `successCode`).
  `ExecutionContext.scheduleDTCAction` sends a module the OBD or the UDS groups by its `dtcProtocol`; the DTC handler
  accepts both groups and asks no UDS follow-ups for codes of OBD modules. A profile without `mode01.json` has no
  OBD-II requests (the module then gets none).
* **App.** `dtcScanHeaders` (`DtcListItem.kt`) builds the scan modules with the protocol from `PREF_DTC_OBD_MODULES`;
  the picker (`DtcModulePickerAdapter`, row `item_dtc_module_pick.xml`: check box mirrored from the ListView's
  choice via `CheckableModuleRow`, `MaterialButtonToggleGroup` UDS / OBD-II, row `blocksDescendants`) saves the marks with `dtcObdModulesAfterPick`
  (listed entries take their row's choice, unlisted keep theirs). `DtcStatus.permanent`, `isPermanentOnly()`,
  `DtcClearOutcome.PERMANENT_REMAIN`, `remainingAfterClear`, `DtcModuleSummary.permanentCount`; `PERMANENT_BADGE`
  chip, in the legend too.
* **Read on connect.** `dtcConnectScanModules(mappings, obdModules)` drops the OBD-II entries unless nothing else is left
  (user's choice: skip them rather than merge codes across sections; the app cannot tell two entries reach one ECU).
  `AdjustmentsStrategy` decides ObdMetrics' own init read on "no module configured" (`dtcScanModules`), unaffected by
  the protocol. A connect scan replaces the stored list, so OBD-II codes from a manual read are gone after the next
  connect; they get no "no longer reported" marker (the scan did not cover them).
* **Picker.** `DtcModulePickerAdapter.onObdMarked` fires when a row is switched to OBD-II; the dialog unticks it.
  Switching back to UDS leaves the check box alone.

## Backward compatibility

* ObdMetrics: `dtcProtocol` defaults to UDS, so existing callers send the same requests as before.
* App: no module marked by default, so every profile scans as before.

## Tests

* ObdMetrics `DiagnosticTroubleCodeReadingTest`: OBD-II reads for a marked module, merged states, module status from
  any answered service, no follow-ups, clear with 04; `dtcProtocolOfTheModuleDecidesTheRequests` (UDS for an unmarked
  module on `DB33F1`, OBD-II for a marked one on `7E0`). `ObdDtcReadCodecTest` (decoding; requests come from
  `mode01.json`).
* App: `DtcListItemTest` (`dtcScanHeaders`, `dtcObdModulesAfterPick`, permanent count, connect scan skips OBD-II
  modules unless only OBD-II ones are configured), `DtcStatusTest`, `DtcReadInfoTest`
  (`PERMANENT_REMAIN`), `DtcLegendTest`.

## Risks and verification checklist

* [ ] Picker: tapping the row toggles the scan check box only, the buttons pick UDS / OBD-II only; marks survive Cancel
  (not saved), confirm (saved), a profile switch and back. PL row text at phone width.
* [ ] A marked module on a non-UDS car: codes from 03 / 07 listed under it, states right.
* [ ] A car with permanent codes: chip shown, clear reports "Permanent codes stay".
* [ ] Unmarked modules: logcat shows `19020D` only.
* [ ] Read on connect with the ECM on UDS and a second ECM entry on OBD-II: one ECM section, no 03 / 07 in the logcat.
* [ ] Picker: switching a row to OBD-II unticks it; "Select all" / confirm state follows.
* [ ] PL texts at phone width.
* Not verified on any car that answers 0A.

## Out of scope

* Code names for OBD-II codes on profiles without GiuliaQV's generic table.
* The default-header read without modules (ObdMetrics' init read) stays UDS.
* Skipping the legacy entry in a UDS scan (roadmap "To investigate").
