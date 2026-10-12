# Spec: DTC extended data, full freeze frames, GME clear answers

2026-10-12 · branch `feat/dtc-extended-data` · Status: implemented, manual checks pending

## Summary

**Problem.** The 2.0 GME ECM showed three gaps:

1. **Freeze frames showed at most 5 values, and a read with snapshots hung.** ObdMetrics' snapshot parser stops at
   the first DID it cannot resolve. The car's freeze frame (`19 04`) holds 11 DIDs; the 6th, `181D`, is not defined
   in the 2.0 GME files, and `1812` / `1004` are 1 byte there but 2 in their live definitions. A frame that failed
   to parse was dropped (`null`), so the DTC handler waited for it until its timeout (23 s for 30 codes). In
   multi-module scans the snapshot requests also went to the last scanned module's header and timed out.
2. **No extended data.** The ECM keeps, per code, how often the fault occurred (DID `6080`) and how many cycles
   remain before the warning lamp switches off (`6081`), via `19 06`. Nothing read it.
3. **A delayed clear looked refused.** The ECM may answer the clear with `7F 14 78` (response pending) before `54`;
   the generic codec reported ERR, so the app's "vehicle refused" toast would be wrong.

**Scope.** All 2.0 GME knowledge lives in ObdMetricsGiuliaQV, as requested; ObdMetrics core only gains generic
extension points. ObdGraphs shows the extended data. Branches:

| Repo | Branch | Change |
|---|---|---|
| ObdMetrics | `feat/dtc-follow-up-requests` | `dtcFollowUp` and `dtcSnapshot` PID groups, `DtcDidLookup`, partial snapshots, `DiagnosticTroubleCode.details`, per-module context requests, GME clear PID removed from `giulia_2.0_gme.json` |
| ObdMetricsGiuliaQV | `feat/gme-snapshot-dids` | `dtcSnapshot` DIDs (181D, 1862, 1-byte 1812/1004, 6080/6081), `GmeDiagnosticTroubleCodeClearCodec` + `dtcClear`, `GmeDtcExtendedDataCodec` + `dtcFollowUp` |
| ObdGraphs | `feat/dtc-extended-data` | "Extended data" box in the phone DTC row, clipboard and share report |

## Behaviour changes

| Area | Before | After |
|---|---|---|
| Phone DTC row, expanded (snapshots on) | "UDS Freeze Frame Data" with ≤ 5 values; box shown even when empty | All 11 values (2.0 GME); box only when values exist; labels on one line |
| Read with snapshots, a frame that fails to parse | Scan waits ~23 s, then lists codes without that snapshot | Partial snapshot shown, no wait |
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
  * `dtcSnapshot` JSON group (`PIDsGroup.DTC_SNAPSHOT`): DID definitions for freeze frames and follow-up data,
    resolved by `DtcDidLookup` before livedata (then livedata, mode 22 first, lowest id). Never queried live; mode
    `19` keeps their query clear of live PIDs. Needed because a freeze frame may store a DID in fewer bytes than its
    live reply.
  * `DiagnosticTroubleCodeSnapshotCodec` returns the DIDs decoded before an error (and a response for NO DATA)
    instead of `null`: a null is never published, so the handler waited for it until its timeout.
  * Wrong turn: the first GiuliaQV version defined 10 DIDs for a 33-DID layout taken from the spec document. The
    car's frame has 11 DIDs and none of them; they were removed.
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
* ObdMetrics `UdsSnapshotParserTest`: the car's frame decodes all 11 DIDs with `dtcSnapshot` definitions over
  livedata; `dtcSnapshot` stays out of `findAll()`; the codec keeps the 5 DIDs decoded before an unknown DID (all
  three fail without the change).
* GiuliaQV: `SnapshotDataSetTest` (the car's frames, incl. a two-record one; fails on the previous JSON at `181D`),
  `GmeDiagnosticTroubleCodeClearCodecTest` (pending cases fail with the generic codec),
  `GmeDtcExtendedDataCodecTest` (the car's reply, unknown DID, negative reply reported, one clear PID + follow-up).
* ObdGraphs: `DtcListItemTest` (`toDidLines`), `VehicleCapabilitiesManagerTest` (details survive storage).

## Risks and verification checklist

- [ ] Release order: ObdMetrics + GiuliaQV snapshots, then the app (see backward compatibility).
- [ ] Car, snapshots on, multi-module scan: freeze frames show engine speed, engine load, battery voltage, …
- [ ] Extended data box shows event counter / cycles for Engine ECU codes.
- [ ] Clear with engine running: "refused" toast; clear with engine off: "cleared" (no false refusal).
- [ ] PL: "Dane rozszerzone" box and labels fit.
- [ ] Car, snapshots on: the scan finishes without the ~23 s wait; freeze frames show 11 values.
- Risk: the scaling of 1003 / 1935 / 1862 in the freeze frame is unverified (copied from `alfa.json`, values look
  implausible); only the framing is pinned by tests.
- Risk: one extra request per code lengthens a scan with snapshots (≈ 0.1–0.3 s per code).

## Out of scope

Extended data on AA, the refusal reason in the app toast (needs a reason field in ObdMetrics' clear status),
freeze-frame records after the first (the ECM may send records 00 and 01; only 00 is shown), moving the GME DTC read (`dtcRead`) out of `giulia_2.0_gme.json`.
