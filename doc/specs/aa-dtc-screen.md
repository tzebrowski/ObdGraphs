# Spec: Android Auto trouble codes (DTC) screen

2026-10-10 · branch `feat/aa-dtc-screen` · Status: implemented, manual checks pending

## Summary

Android Auto gets a "Diagnostic Trouble Codes" screen that lists the stored diagnostic trouble codes and lets
the driver read them again or clear them, so the head unit offers what the phone's DTC dialog does.

**Problem.** DTC were reachable only from the phone (`DiagnosticTroubleCodePreferenceDialogFragment`).
In the car, a check-engine light meant picking up the phone.

**Scope.** `:automotive` (new screen, navigation, settings, EN/PL strings), `:datalogger` (DTC list
logic moved here from `:app` so both UIs share it, JVM tests), `:screen_renderer` (settings data
class), `:app` (preference, EN/PL strings, dialog now uses the shared logic).

## Behaviour changes

| Area | Before | After |
|---|---|---|
| AA "Available screens" list | Surface screens + Routines | + "Diagnostic Trouble Codes" (when enabled) |
| AA DTC list | — | Last stored DTC (same store as the phone dialog), grouped by module like the phone |
| AA read | — | Strip action (sync icon) while connected; loading list until `DATA_LOGGER_DTC_ACTION_COMPLETED` |
| AA clear | — | Strip action (red trash) while connected and codes exist → confirmation `MessageTemplate` → clear |
| AA read info / clear result | — | Added later: see `dtc-read-info-clear-outcome.md` ("Last read" row, toast per clear outcome) |
| AA while disconnected | — | Strip shows Connect only (idle `ROUTINES_QUERY`, no live PIDs) |
| Phone DTC dialog | — | Unchanged output; grouping/sorting/formatting now come from `:datalogger` |
| Untagged codes after a multi-module scan (phone + AA) | Section "ECU", next to real modules such as "Engine ECU" | Section "Unknown module" (`pref.dtc.unknown_module` / `dtc.unknown_module`); single-module scans still file them under that module, a plain default-ECU read stays flat |

Row: title = code with failure type (`P0123-1A`), first line = description (or the
system → category path when unknown), second line = active statuses when present.

## Settings / keys touched

| Key | Type | Default | Notes |
|---|---|---|---|
| `pref.aa.dtc.enabled` | Boolean | `true` | New. Code default matches the XML default. |
| `pref.dtc.module_picker.deselected` | String set | — | Existing, read-only here: AA scans the modules the phone picker last left checked. |

## Implementation

* `datalogger/.../bl/datalogger/dtc/DtcListItem.kt` (moved from `app/.../preferences/dtc`, now public):
  `toDtcListItems`, `sortedForDisplay`, `displayCode`, `displayDescription`, `isDescriptionUnknown`,
  `dtcScanModules` (mappings with a header minus deselected), `toDtcSections` (header/row stream →
  sections for `ListTemplate.addSectionedList`).
* `automotive/.../screen/nav/DiagnosticTroubleCodesScreen.kt`: `ListTemplate` (single list for the
  default-ECU case, sectioned lists per module otherwise), `DtcClearConfirmationScreen`.
  Identity `DtcScreenIdentity.DTC` (223); icon `drawable/action_dtc.xml` (check-engine symbol with "!", vector, white like the other strip icons).
* `NavTemplateCarScreen.gotoScreen` pushes the screen for that identity; `availableFeatures()` lists it.
* `ScreenSettings.getDtcScreenSettings()` / `DtcScreenSettings`, implemented in `CarSettings`.
* `ListTemplate`'s action strip allows **2** actions (`ACTIONS_CONSTRAINTS_SIMPLE`), hence
  connect-only when disconnected and read + clear when connected; disconnect stays on the main screen.
* The receiver lives from `onCreate` to `onDestroy` of the screen's own lifecycle, so a result
  arriving while the confirmation is on top still ends the loading state. Error/stop/connect-error
  events also end it — the DTC action never reports back once the link is gone.

### Module tags come from ObdMetrics

A code's section is its `DiagnosticTroubleCode.module`, set by the codec from the PID it decodes with.
Two DTC read PIDs sharing a query (`giulia_2.0_gme.json` + `2.0_gme_ext.json` in the GME profiles) made
ObdMetrics decode with the registry's copies (`module=ecu`), so every code of a multi-module scan landed
in "ECU" and each scanned module read "No codes reported". Fixed upstream on branch
`fix/dtc-module-tagging` in ObdMetrics (decoder keeps the command's module; one read per query) and
ObdMetricsGiuliaQV (`ExtendedDiagnosticTroubleCodeCodec` tags the module); needs an `obdMetricVersion`
bump here. "Unknown module" is the safety net for any codec that still forgets the tag.

### One scan per header

CAN header table entries can share a header (GME profiles: "Engine ECU" / "Engine ECU (2.0 GME)" on
`DA10F1`, "Service Interval" / "Instrument Panel" on `DA60F1`). With module tags fixed, both entries
listed the same codes. `dtcScanTargets` (used by `WorkflowOrchestrator.getDtcModules`, so phone and AA,
read and clear) keeps one entry per header, the first selected one in table order, which names the section.

## Backward compatibility

No pref changes beyond the new key; stored DTC and module selection are shared with the phone, not
copied. The phone dialog's rendering is unchanged (same functions, moved).

## Tests

`datalogger/src/test/.../dtc/DtcListItemTest.kt` (plain JUnit, `./gradlew :datalogger:testDebugUnitTest`):
display code, description fallback, sort order, scan module selection, section folding for the
default-ECU, empty and multi-module cases. `:automotive` has no local test suite; the screen itself
is checked manually.

## Risks and verification checklist

- [ ] DHU: "Diagnostic Trouble Codes" appears in Available screens; hidden when `pref.aa.dtc.enabled` is off.
- [ ] Disconnected: only Connect in the strip; connecting shows the loading list, then read/clear.
- [ ] Read: loading list, then refreshed codes + "Trouble codes updated." toast.
- [ ] Clear: confirmation shown; Cancel/Back does nothing; Clear clears and reloads the list.
- [ ] With DRI modules configured: one section per scanned module, "No codes reported." for empty ones.
- [ ] Disconnect during a read (adapter off): loading state ends.
- [ ] Phone DTC dialog: list, share and clear unchanged.
- Risk: long code lists are truncated by the host's list limit while driving (car API behaviour).

## Out of scope

Module picking on AA (uses the phone's selection), freeze-frame/snapshot details, share/search.
