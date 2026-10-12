# Spec: DTC extended data, full freeze frames, GME clear answers

2026-10-12 · branch `feat/dtc-extended-data` · Status: implemented, manual checks pending

## Summary

**Problem.** The 2.0 GME ECM UDS spec (`~/github/UDS/ECM-ECM_FGA-96-0001505917(006_005).xlsx`) showed three gaps:

1. **Freeze frames showed at most 4 values.** ObdMetrics' snapshot parser stops at the first unknown DID, and the
   ECM's snapshot (`19 04`, 33 DIDs) has `1921` fifth, which no loaded PID file defined. In multi-module scans the
   snapshot requests also went to the last scanned module's header, so they timed out (the 5 s wait in the logs).
2. **No extended data.** The ECM keeps, per code, how often the fault occurred (DID `6080`) and how many cycles
   remain before the warning lamp switches off (`6081`), via `19 06`. Nothing read it.
3. **A delayed clear looked refused.** The ECM may answer the clear with `7F 14 78` (response pending) before `54`;
   the generic codec reported ERR, so the app's "vehicle refused" toast would be wrong.

**Scope.** All 2.0 GME knowledge lives in ObdMetricsGiuliaQV, as requested; ObdMetrics core only gains generic
extension points. ObdGraphs shows the extended data. Branches:

| Repo | Branch | Change |
|---|---|---|
| ObdMetrics | `feat/dtc-follow-up-requests` | `dtcFollowUp` PID group, `DiagnosticTroubleCode.details`, per-module context requests, GME clear PID removed from `giulia_2.0_gme.json` |
| ObdMetricsGiuliaQV | `feat/gme-snapshot-dids` | 10 snapshot DIDs, `GmeDiagnosticTroubleCodeClearCodec` + `dtcClear`, `GmeDtcExtendedDataCodec` + `dtcFollowUp`, DIDs 6080/6081 |
| ObdGraphs | `feat/dtc-extended-data` | "Extended data" box in the phone DTC row, clipboard and share report |

## Behaviour changes

| Area | Before | After |
|---|---|---|
| Phone DTC row, expanded (snapshots on) | "UDS Freeze Frame Data" with ≤ 4 values; box shown even when empty | All 33 values (2.0 GME); box only when values exist; labels on one line |
| Phone DTC row, expanded | — | "Extended data" box: "DTC Event Counter: 7", "Cycles To Warning Lamp Off: 3" (when the ECU answered) |
| Copy / share report | Snapshot only | Snapshot + extended data |
| Multi-module scan with snapshots | Requests sent to the last module; timed out | Each code's requests go to its own module (`ATSH` per module) |
| Clear answered `7F 14 78` … `54` (2.0 GME) | ERR → "vehicle refused" | OK; `7F 14 22/21/31/13/7F` stay ERR (reason logged) |
| AA DTC screen | — | Unchanged (no detail view on AA) |

## Settings / keys touched

None new. Extended data is read together with snapshots, i.e. only with "Read DTC snapshots"
(`pref.dtc.read_snapshots`, default off → `DtcAction.READ_SNAPSHPOTS`). It costs one extra request per code.

## Implementation

* **ObdMetrics (generic):**
  * `dtcFollowUp` JSON group (`PIDsGroup.DTC_FOLLOW_UP`): request templates with a `{dtc}` placeholder (e.g. `pid:
    "06{dtc}FF"`) and a `codecClass`. The codec returns a `DtcFollowUpResponse`; its values land in
    `DiagnosticTroubleCode.details`.
  * `DiagnosticTroubleCodeHandler.processFollowUps`: snapshot + templates per code, grouped by module with `ATSH`
    from the scheduled headers, results keyed by module + code. The timeout scales with the number of requests.
  * `SnapshotFormulaEvaluator` is public so follow-up codecs decode DIDs like snapshots.
  * The 2.0 GME `dtcClear` moved out of `giulia_2.0_gme.json`. A query must have one clear PID across the loaded
    files: the registry keeps all of them and the decoder runs each, so the winner would depend on file order.
* **ObdMetricsGiuliaQV:** see its CLAUDE.md ("DTC behaviour lives here").
* **ObdGraphs:** `toDidLines` (`:datalogger`, shared) formats snapshot and extended-data DIDs. Every 2.0 GME
  profile loads `2.0_gme_ext.json`, so no profile loses the clear.

## Backward compatibility

Stored codes without `details` deserialize with an empty list. Profiles without `dtcFollowUp` entries behave as
before (snapshot only, now sent to the right module). The app needs the new ObdMetrics and GiuliaQV snapshots
together: core without the GiuliaQV clear PID leaves 2.0 GME profiles with no clear at all.

## Tests

* ObdMetrics `DiagnosticTroubleCodeReadingTest.followUpRequestsGoToEachCodesModule`: templates sent per code with
  `{dtc}` replaced, details attached, snapshot and follow-up requests under each code's header (the snapshot part
  fails on `v11.x`: all went to `ATSH760`). Clear tests use a generic `dtc_clear.json` test resource.
* GiuliaQV: `SnapshotDataSetTest` (all 33 DIDs; fails on master with "Unknown DID encountered: 1921"), 10 codec
  tests, `GmeDiagnosticTroubleCodeClearCodecTest` (pending cases fail with the generic codec),
  `GmeDtcExtendedDataCodecTest` (decoding, unknown DID, one clear PID + follow-up across the GME files).
* ObdGraphs: `DtcListItemTest` (`toDidLines`), `VehicleCapabilitiesManagerTest` (details survive storage).

## Risks and verification checklist

- [ ] Release order: ObdMetrics + GiuliaQV snapshots, then the app (see backward compatibility).
- [ ] Car, snapshots on, multi-module scan: freeze frames show engine speed, coolant temperature, gear, …
- [ ] Extended data box shows event counter / cycles for Engine ECU codes.
- [ ] Clear with engine running: "refused" toast; clear with engine off: "cleared" (no false refusal).
- [ ] PL: "Dane rozszerzone" box and labels fit.
- Risk: the 10 new GME DIDs and 6080/6081 are unverified on a car; their bitfields show only the headline bit.
- Risk: one extra request per code lengthens a scan with snapshots (≈ 0.1–0.3 s per code).

## Out of scope

Extended data on AA, the refusal reason in the app toast (needs a reason field in ObdMetrics' clear status),
decoding every flag of the bitfield DIDs, moving the GME DTC read (`dtcRead`) out of `giulia_2.0_gme.json`.
