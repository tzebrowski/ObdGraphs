# Spec: DTC history per vehicle (with VIN on every read)

2026-10-14 · branch `feat/dtc-history` · Status: implemented (A–D); on-device checks pending

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
| Phone DTC list, code seen for the first time | No marker | "New" chip on the row (green, own icon), first in the chip line |
| Phone DTC list, codes gone since the previous scan | Not shown | Collapsed section at the end. Its header has the module headers' layout (user's request: all sections alike): green "No longer reported" title, a green check chip with the count, arrow; tap expands. The meaning is in the legend. Rows greyed, code struck through, module named, "Last reported <date>" in place of the state chips |
| Clear / Refresh pressed while another DTC action runs (e.g. the connect scan) | ObdMetrics cut the running action short; its result was taken for the new one, whose read was then stored with no modules (list collapsed into one "ECU" group) | The new action waits and starts when the running one completes; each result is stored with its own kind and modules |
| Phone expanded row | Code details | "Code history" panel on top: first / last reported, in N of M scans |
| Phone code row | Expandable, nothing says so | Chevron at the row end, turned up while expanded |
| AA DTC list | Codes only | Status line starts with a green "New" for new codes. No "gone" section: AA lists are short |
| Vehicle unknown (no VIN reply even after A) | — | History goes under `UNKNOWN`; no new / gone markers (the codes might come from another car) |
| VIN not programmed (`NO_VIN`) | — | One shared history key, **with** markers: most such users have one car, and the 2.0 GME test car is one |
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
all cars with an empty ECM VIN share a bucket. Unlike "unknown vehicle" it gets markers (see D).

### B. History store (`dtc/DtcHistory.kt` pure, `dtc/DtcHistoryStore.kt` file)

```kotlin
data class DtcScan(
    val timestamp: Long,
    val kind: DtcScanKind,                  // READ, CLEAR (the read that follows a clear), CONNECT
    val scannedModules: List<String>,       // empty = default single-ECU read
    val codes: List<DiagnosticTroubleCode>  // as stored today, incl. snapshot / details
)
```

* `updateDTC` gains a `kind` and appends a `DtcScan` under `historyKey(vin)` (upper-cased VIN, or `UNKNOWN`), with
  the read info's timestamp. Kinds: `WorkflowOrchestrator.onDTCCompleted` → `CLEAR` after a clear, `CONNECT` for the
  connect scan, else `READ`; ObdMetrics' init read (`updateCapabilities`) → `CONNECT`.
* The file is written atomically (temp file, then rename) and cached in memory; `DtcHistoryStore` is `@Synchronized`.
  A failed write is logged and never blocks storing the current list.
* **Caps:** 30 scans per vehicle and 5 vehicles (least recently scanned dropped first). A connect scan that
  equals the previous scan, itself a connect scan (same modules, keys and status bytes), replaces it instead of appending. Otherwise every ignition-on
  fills the cap with identical records.
* **Saving before a clear needs no extra write.** The last scan before the clear is already the newest record.
  The roadmap's "save before clear" holds as long as the cap never drops the newest records.

### B2. One DTC action at a time (`dtc/DtcActionQueue.kt`)

Found on the car while testing: Clear pressed 6 s into the connect scan. ObdMetrics' `scheduleDTCAction` clears its
command buffer, so the scan was cut short; its completion arrived first and was stored as the clear's result
("codes left=12"), and the clear's own read then found the app's per-action fields reset and was stored with no
scanned modules. `WorkflowOrchestrator` used single `pending*` fields for actions that can overlap.

* `WorkflowOrchestrator` submits every request (`DtcRequest(kind, modules)`) to `DtcActionQueue`. Free → sent now;
  busy → it waits. Only the newest waiting request is kept, but a read never replaces a waiting clear.
* `onDTCCompleted` takes the finished request from the queue: its kind and module labels go to `updateDTC`. When a
  request waited, it is sent (off the callback thread) and `DATA_LOGGER_DTC_ACTION_COMPLETED` is held back, so the
  screen that asked for it keeps its overlay until its own result arrives.
* An action with no completion stops blocking after `dtcActionTimeoutMs(modules)`; a refused schedule (not
  `DTC_QUEUED`), a new connection and a disconnect reset the queue.
* ObdMetrics should still not cancel a running DTC action silently; that guard belongs upstream (not done).

### C. Diff (`dtc/DtcHistoryDiff.kt`, pure)

* Code identity = `DtcListItem.itemKey()` (module + display code), the same key the lists use.
* **Coverage** (`DtcScan.covers`): a module scan covers the modules it targeted. A code without a module of its
  own (default ECU, `ecu` or untagged) is covered only by scans with the same targets as the scan that reported it,
  so the default single-ECU read is compared only with other default reads.
* **New** = in the current scan, absent from every earlier scan covering it, **and at least one earlier scan
  covered it**. "New since the last scan" is not enough: an intermittent code would flash "new" every other drive.
  The first scan of a module (newly picked) or of a vehicle marks nothing.
* **Gone** = present the last time an earlier scan covered it, absent now, **and the current scan covers it**.
  A code from a module the user unchecked in the picker has not gone, it was just not looked at. A code gone
  before the previous covering scan is not listed again.
* Per code: first seen, last seen, scans it appeared in out of the scans that covered it.
* `DtcHistoryStore.diff(readInfo)` diffs the vehicle the stored list was stamped with, and only when that
  vehicle's newest scan has the list's timestamp (otherwise the list predates history, or the write failed): no markers.

### D. UI

* Shared list logic (`DtcListItem.kt`): `DtcRow` gains `isNew`, `isGone`, `history` (not part of `itemKey()`);
  `markedWith(diff)` marks rows; `withGoneSection(diff, expanded)` appends `GoneHeader(count, expanded)` and,
  expanded, the gone rows.
* Phone: `item_dtc.xml` gets a "New" chip first in the chip row (chips now spaced by end margin, so any can be
  gone), and a "Code history" panel first in the expanded details (green start edge and title, the "New" colour):
  first reported, last reported, "reported in N of M scans of this module". Every code row has a chevron at its
  end (rotated while expanded): without it, users did not find the details or the history. The "New" explanation
  joins the state explanations. Gone rows: alpha 0.6, code in secondary colour, description prefixed with the
  module, no state chips (they would describe an old scan). The section header (`item_dtc_gone_header.xml`) toggles
  `goneExpanded` in the dialog, which starts collapsed each time the dialog opens.
* AA: the row's status line starts with "New" in green (colour spans are allowed in row text, see CLAUDE.md DTC
  notes). Rows have two text lines, already description + status, so it shares the status line.

## Backward compatibility

* Existing prefs untouched; screens that read `getDiagnosticTroubleCodes()` behave as before.
* A missing or corrupt history file = empty history (log and start over). It never blocks storing the current list.
* The first scan after updating has nothing to compare against, so it marks nothing as new.

## Tests

* `DtcHistoryDiffTest` (done): first scan marks nothing; new versus intermittent; gone and not re-listed; gone only for
  scanned modules; a newly scanned module marks nothing new; same code from two modules; default read compared only with
  default reads; status change keeps identity; `markedWith` / `withGoneSection`.
* `DtcHistoryTest` (done): caps (per vehicle, vehicle eviction), identical connect scan replaced (and not replaced when
  state or kind differ), unknown key, round trip with freeze frames, corrupt file → empty, diff only for the newest
  scan, unknown vehicle without markers / `NO_VIN` with, unwritable file never throws.
* `DtcActionQueueTest` (done): the logged sequence (connect scan, then clear) keeps each completion with its own
  request and modules; a read does not replace a waiting clear; newest waiting read wins; timeout; reset.
* `VehicleCapabilitiesManagerTest` (done): `updateDTC` appends with kind, modules, time and VIN key; the init read on
  connect is a `CONNECT` scan of the unknown vehicle.
* `AdjustmentsStrategyTest` (done): properties off + DTC on → METADATA not requested, VIN reading requested; both off → neither.
* ObdMetrics `VinReadingTest` (done): VIN-only reads only `22 F190`; flags off → no metadata; full reading unchanged.
  Fails without the init filter.
* [ ] Car with `fetchDeviceProperties` off: read info shows the VIN after a connect.
* [ ] Two reads, clear one code in between: no "New", the cleared code is under "No longer reported".
* [ ] Uncheck a module, scan: its codes are not reported as gone.
* [ ] Clear: the pre-clear scan, with its freeze frames, is still in `dtc_history.json` (`run-as`).
* [ ] Ten connects without a code change: one connect record, not ten.
* [ ] PL strings at phone width (chip and section header / banner).
* [ ] Connect, press Clear within ~10 s (during the connect scan): the overlay stays until the clear's result, and
  the list keeps its module groups.
* [ ] Properties off: logcat init shows `22 F190` (or `09 02`) and no other metadata request; DTC read info shows the VIN.
* Risk: a profile with neither 09 02 nor F190 gets no VIN; history then uses "unknown vehicle" (no markers).
* Risk: a module that does not answer looks clean (see the roadmap's "To investigate"), so its codes show as gone.
* Risk: all `NO_VIN` cars share one history; a second car with an unprogrammed VIN gets wrong markers.

## Out of scope

* A history browser (list of past scans, opening one read-only). The store supports it; the UI comes later.
* Notifications for new codes (roadmap 4). They build on `DtcHistoryDiff`.
* Export of history as JSON / CSV (roadmap 9).
* Clearing one module (roadmap 5).
