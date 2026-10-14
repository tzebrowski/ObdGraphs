# Spec: modules that do not answer a DTC scan

2026-10-14 · branch `feat/dtc-module-response` (ObdGraphs + ObdMetrics) · Status: implemented, on-device checks pending

Roadmap "To investigate: modules that do not answer" (`dtc-roadmap.md`).

## Summary

**Problem.**
* A scanned module that does not answer (asleep, wrong header, timeout) looks the same as a clean one: header with a
  green check, "No trouble codes reported". The driver is reassured about a module nobody looked at.
* Since DTC history (`dtc-history.md`), it also produces false markers: every code the module had last time is listed
  under "No longer reported", and its codes come back as not new once it answers again.

**Why ObdMetrics.** The app gets only the merged code set. Inside ObdMetrics a module that sends `NO DATA` (or another
adapter error) produces no reply at all - `ObdCommandHandler` drops it - while `59 02 FF` (no codes) and `7F 19 xx`
(refused) both decode to an empty list. Only the DTC handler sees which module answered and how.

**Scope.** ObdMetrics (`DiagnosticTroubleCodeHandler`, `Lifecycle`), `:datalogger` (storage, history coverage, list
logic), `:app` (header chip, message, legend, share report), `:automotive` (message row), EN + PL.

## Behaviour changes

| Area | Before | After |
|---|---|---|
| Module that did not answer, phone header | Green check, collapsed like a clean module | Grey "no response" chip; collapsed |
| Its expanded line | "No trouble codes reported" (green) | "No response from the module. Codes unknown." (grey) |
| Module that refused the read (`7F 19 xx`) | Same as clean | "Read refused by the module" chip + line |
| Module order | Codes, then clean | Codes, then not answered / refused, then clean |
| History | Its previous codes listed as "no longer reported" | Not answered = not scanned: no gone / new markers from it |
| AA | Green check row "No trouble codes reported" | Row "No response from the module" with a grey icon |
| Shared report | — | "Modules without response: ..." line |
| Legend | — | Line for the "no response" chip |
| Lists stored before this change | — | No statuses stored: every module counts as answered, as before |

## Settings / keys touched

| Key | Type | Notes |
|---|---|---|
| `pref.datalogger.dtc.module_status` | JSON `{module label → RESPONDED / REFUSED / NO_RESPONSE}` | New, written with the list in `updateDTC`; missing = all answered |
| `files/dtc_history.json` | `DtcScan.moduleStatus` | New field, default empty (older scans unchanged) |

## Implementation

### ObdMetrics

* `DtcModuleStatus` enum (`command.dtc`): `RESPONDED`, `REFUSED`, `NO_RESPONSE`.
* `DiagnosticTroubleCodeReadCodec`: a negative response to service 19 (`7F 19`, NRC 78 "pending" skipped,
  `DtcModuleStatus.of`) decodes to `DtcReadReply(REFUSED)`, an empty `List` carrying the status, instead of a plain
  empty list. Every consumer still sees a list.
* `DiagnosticTroubleCodeHandler.onNext`: any decoded `DTC_READ` reply marks its module ("" untagged) as answered -
  `RESPONDED`, or the `DtcReadReply` status. Reset with the other per-action state at `DiagnosticTroubleCodeStartCommand`.
  Wrong turn: classifying `reply.getRaw()`. The decoder publishes the raw response only with
  `collectRawConnectorResponseEnabled`, so every module read as `NO_RESPONSE`.
* At the schedule command, every targeted module gets its status; a module without a reply is `NO_RESPONSE`.
* `Lifecycle.onDTCCompleted(dtc, status, moduleStatus)`: new default method calling the two-argument one, so existing
  implementers keep working; the handler and `Lifecycle.Subscription` call the three-argument one. A default-ECU read
  (no modules) passes an empty map.

### App

* `WorkflowOrchestrator` overrides the three-argument callback and passes the map to `updateDTC`, which stores it
  (`pref.datalogger.dtc.module_status`) and puts it into the `DtcScan`.
* `DtcScan.covers`: a module whose status is not `RESPONDED` is not covered, so the history never compares it.
* `toDtcListItems(…, moduleStatus)`: header and message per status; `ModuleHeader.response` (not in `itemKey()`).
  `collapsedBy` keeps such modules collapsed by default (they have no codes).
* Phone: header chip `NO_RESPONSE_BADGE` / `REFUSED_BADGE` (grey / amber, "?" / "block" icons) in place of the
  check; legend row; the share report lists them. AA: message row with a grey icon.

## Backward compatibility

* ObdMetrics: the new callback is a default method; the old one is still called for implementers that only override it.
* App: a list or scan without statuses counts every module as answered (old behaviour). An app built against an older
  ObdMetrics is not possible: the override needs the new method (same version, `11.26.1-SNAPSHOT`).

## Tests

* ObdMetrics `DtcModuleStatusTest` (done): reply classification (positive, multi-frame, `7F 19 xx`, NRC 78 ahead of the
  answer, empty).
* ObdMetrics `DiagnosticTroubleCodeReadingTest.scheduleDtcReadReportsHowEachModuleAnswered` (done): two modules answering
  `59 02 FF` / `7F 19 11` / nothing → `RESPONDED` / `REFUSED` / `NO_RESPONSE` per module (the mock replies by request,
  so one status per case). The existing tests use the two-argument callback and still pass.
* `DtcHistoryDiffTest` (done): a silent / refusing module produces no gone codes and is not counted as covered; once it
  answers again it is compared again.
* `DtcListItemTest` (done): header / message per status, order (codes, silent, clean), silent modules start collapsed,
  statuses missing = answered, unanswered modules in scan order.
* `VehicleCapabilitiesManagerTest` (done): statuses round trip through the pref and into the history scan.
* `DtcLegendTest` (done): the legend covers the two new chips.
* [ ] Car: unplug / skip a module (e.g. a header no ECU answers), scan: grey "no response", its codes not "gone".
* [ ] PL strings at phone width.

## Risks

* A module that answers slower than the adapter timeout reads as `NO_RESPONSE` while it may have codes. That is the
  truth as far as the scan knows, and now visible instead of looking clean.
* `REFUSED` relies on the `7F 19` prefix; another negative-reply shape would count as answered with no codes, as today.

## Out of scope

* "Retry this module": a single-module read replaces the whole stored list today; it needs a merge of partial scans.
* Retrying silently inside ObdMetrics.
