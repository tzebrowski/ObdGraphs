# Spec: DTC history per vehicle (with VIN on every read)

2026-10-13 · branch `feat/dtc-history` · Status: part A (VIN) implemented, B–D proposed

Roadmap items 1 and 8 (`dtc-roadmap.md`). Item 8 comes first: history is keyed by VIN.

## Summary

**Problem.**
* Every DTC store (`VehicleCapabilitiesManager.updateDTC`) overwrites the single list in `pref.datalogger.dtc`.
  The driver cannot see which codes are new since the last scan and which have gone (healed or cleared).
  A clear also erases the freeze frames of the codes it removes, and the app keeps no copy.
* The VIN that read info is stamped with comes from the connect metadata. With
  `pref.adapter.init.fetchDeviceProperties` off, metadata is `{}` and every read is stamped `vin=null`.
  Then "another vehicle" cannot be detected and history has no key.

**Scope.** ObdMetrics (`vehicleMetadataVinReadingEnabled`, part A). `:datalogger` (VIN source, history store, diff logic: pure and unit-tested). `:app` (phone DTC
dialog markers and the "gone" section, EN/PL). `:automotive` ("new" marker in AA rows, EN/PL).

## Behaviour changes

| Area | Before | After |
|---|---|---|
| VIN with device properties off | Not known; reads stamped `vin=null` | With DTC reading on, the connect init reads the VIN PID alone (one request) on every connect |
| Each DTC store (read, clear, connect scan) | Overwrites the one list | Also appends a scan record to the vehicle's history (capped) |
| Clear | Freeze frames of cleared codes are lost | The scan before the clear stays in history, with its freeze frames |
| Phone DTC list, code seen for the first time | No marker | "New" chip on the row (accent colour) |
| Phone DTC list, codes gone since the previous scan | Not shown | Collapsed section at the end: "No longer reported (N)", rows greyed out, with first / last seen |
| Phone expanded row | Code details | Adds "First seen …, in N of M scans" |
| AA DTC list | Codes only | Row text gets a "New" prefix for new codes. No "gone" section: AA lists are short |
| Vehicle unknown (no VIN even after A) | — | History goes under "unknown vehicle"; no new / gone markers (the codes might come from another car) |
| Codes stored before this change | — | No history; the first scan after the update starts it, and marks nothing as new |

## Settings / keys touched

| Key / file | Type | Notes |
|---|---|---|
| `files/dtc_history.json` (app private storage) | JSON `{vin → [scan]}` | New. Not a pref: freeze frames make it tens of kB, too big for `SharedPreferences`, which are loaded into memory at startup |
| `pref.datalogger.dtc*` | — | Unchanged. They stay the "current list" every existing screen reads |
| `pref.adapter.init.fetchDeviceProperties` | — | Unchanged for the other metadata; the VIN is read whenever DTC reading is on (summary text says so, EN/PL) |

## Implementation

### A. VIN on every read (roadmap 8) — implemented

**Decision (user):** a granular ObdMetrics adjustment reads the VIN alone. Full metadata reading still follows
`fetchDeviceProperties`.

* **ObdMetrics** (`feat/vin-reading`):
  * `Adjustments.vehicleMetadataVinReadingEnabled` (default false). It does **not** add METADATA to
    `getRequestedGroups()`: that would read the whole group. `WorkflowBufferInitializer` queues the VIN PIDs in a
    step of its own when `isVehicleMetadataVinReadingEnabled()` and METADATA is not among the requested groups (no second VIN read).
  * VIN PIDs are recognised by the package-private `api.VinPids.isVin`: mode `09` PID `02` (OBD-II) or `22` `F190` (UDS, ISO 14229). The
    match goes by request, not description, because descriptions differ per profile.
  * Replies go through `MetadataReader` as before, into `VehicleCapabilities.metadata`.
* **App:** `AdjustmentsStrategy` sets it from `dtcEnabled`, on every connect. One request per connect, so a change
  of car is seen, and `updateCapabilities` / `getCurrentVin()` need no change. Drag racing adjustments stay off.
* Rejected:
  * Reading the whole metadata group while no VIN is stored: a change of car would go unnoticed afterwards.
  * A one-shot query switch after connect: it delays live data.
  * Marking the other metadata PIDs `stable=false`: a user-editable flag.

**Empty VIN (`NO_VIN`).** An unprogrammed ECM answers `22 F190` with 17 spaces (seen on a 2.0 GME; `09 02` is
refused with `7F 09 11`). `vinFrom` returns `NO_VIN` (`"00000000000000000"`) when a VIN PID answered with an empty
value (blank, zeros, 0x00 / 0xFF), and still null when no VIN PID answered. The VIN PIDs' labels come from the
registry (`isVinRequest`: 09 02 / 22 F190), because metadata is keyed by translated labels. `NO_VIN` shows as
"VIN not programmed" (EN/PL, phone + AA) and never counts as "another vehicle". In history it is one shared key:
all cars with an empty ECM VIN share a bucket, like "unknown vehicle".

### B. History store (`dtc/DtcHistory.kt`, pure + a thin file wrapper)

```kotlin
data class DtcScan(
    val timestamp: Long,
    val kind: DtcScanKind,                  // READ, CLEAR (the read that follows a clear), CONNECT
    val scannedModules: List<String>,       // empty = default single-ECU read
    val codes: List<DiagnosticTroubleCode>  // as stored today, incl. snapshot / details
)
```

* `updateDTC` gains a `kind` and appends a `DtcScan` under `vin ?: UNKNOWN_VEHICLE`. One writer, on the
  DTC handler's thread; the file is written atomically (temp file, then rename).
* **Caps:** 30 scans per vehicle and 5 vehicles (least recently scanned dropped first). Connect scans that
  equal the previous scan (same keys + status) replace it instead of appending. Otherwise every ignition-on
  fills the cap with identical records.
* **Saving before a clear needs no extra write.** The last scan before the clear is already the newest record.
  The roadmap's "save before clear" holds as long as the cap never drops the newest records.

### C. Diff (`dtc/DtcHistoryDiff.kt`, pure)

* Code identity = `DtcListItem.itemKey()` (module + display code), the same key the lists use.
* **New** = in the current scan, absent from every earlier scan of this vehicle.
  "New since the last scan" is not enough: an intermittent code would flash "new" every other drive.
* **Gone** = in the previous scan, not in the current one, **and its module was scanned this time**.
  A code from a module the user unchecked in the picker has not gone, it was just not looked at.
  The default single-ECU read only compares against other default reads.
* Per code: first seen, last seen, number of scans it appeared in out of the scans that covered its module.

### D. UI

* Phone: `item_dtc.xml` gets a "New" chip in the existing chip row, before the state chips, so the layout does
  not change. The "gone" section is a new `DtcListItem` subtype (header + rows) placed after the clean modules.
  It is collapsed by default.
* AA: prefix in the row's text line (colour spans are allowed there, see CLAUDE.md DTC notes).

## Backward compatibility

* Existing prefs untouched; screens that read `getDiagnosticTroubleCodes()` behave as before.
* A missing or corrupt history file = empty history (log and start over). It never blocks storing the current list.
* The first scan after updating has nothing to compare against, so it marks nothing as new.

## Tests

* `DtcHistoryDiffTest`: new versus intermittent; gone only for scanned modules; default read only
  compared with default reads; unknown vehicle gets no markers.
* `DtcHistoryTest`: caps (per vehicle, vehicle eviction), identical connect scan replaced, corrupt file → empty.
* `VehicleCapabilitiesManagerTest`: `updateDTC` appends with the right kind and VIN key.
* `AdjustmentsStrategyTest` (done): properties off + DTC on → METADATA not requested, VIN reading requested; both off → neither.
* ObdMetrics `VinReadingTest` (done): VIN-only reads only `22 F190`; flags off → no metadata; full reading unchanged.
  Fails without the init filter.
* [ ] Car with `fetchDeviceProperties` off: read info shows the VIN after a connect.
* [ ] Two reads, clear one code in between: no "New", the cleared code is under "No longer reported".
* [ ] Uncheck a module, scan: its codes are not reported as gone.
* [ ] Clear: the pre-clear scan, with its freeze frames, is still in `dtc_history.json` (`run-as`).
* [ ] Ten connects without a code change: one connect record, not ten.
* [ ] PL strings at phone width (chip and section header).
* [ ] Properties off: logcat init shows `22 F190` (or `09 02`) and no other metadata request; DTC read info shows the VIN.
* Risk: a profile with neither 09 02 nor F190 gets no VIN; history then uses "unknown vehicle".
* Risk: profiles with neither 09 02 nor F190 in their metadata get no VIN; history then uses "unknown vehicle".

## Out of scope

* A history browser (list of past scans, opening one read-only). The store supports it; the UI comes later.
* Notifications for new codes (roadmap 4). They build on `DtcHistoryDiff`.
* Export of history as JSON / CSV (roadmap 9).
* Clearing one module (roadmap 5).
