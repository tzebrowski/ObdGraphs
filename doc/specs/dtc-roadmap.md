# DTC roadmap

2026-10-13 · Status: proposed, not implemented (index of planned DTC work; each item gets its own spec when started)

Planned improvements to trouble-code handling, phone and AA. They are ranked by value to the driver and come from
comparing with OBDeleven, FORScan, Car Scanner, Torque, Carly and AlfaOBD/MultiECUScan. When an item starts, it gets
its own `doc/specs/<feature>.md` and this list links to it.

## Done

| Item | Spec |
|---|---|
| Module grouping, colours, modules with codes first | `dtc-list-colors-order.md` |
| Freeze frames, extended data (2.0 GME) | `dtc-extended-data.md` |
| Last read time / VIN, clear outcome | `dtc-read-info-clear-outcome.md` |
| AA DTC screen | `aa-dtc-screen.md` |
| Show what state each code is in (initial #1): chips for active / stored / pending / history + MIL, sort by state | `dtc-status-badges.md` |
| Read/clear overlay no longer times out during a long scan | `dtc-status-badges.md` (fix in the same branch) |
| Readiness monitors (initial #3): phone screen, clear warning, DTC list row | `readiness-monitors.md` |
| Modules that do not answer: "no response" / "refused" instead of clean, left out of history markers | `dtc-module-response.md` |
| History per vehicle (initial #4) with VIN on every connect (added later #8): new / gone markers | `dtc-history.md` |
| New-code notification after the connect scan (initial #6): phone notification + AA toast | `dtc-new-code-notification.md` |

## Planned

Ranked; dependencies noted. **App** = ObdGraphs only. **ObdMetrics** = needs commands or decoders upstream first.
"Initial #N" is the item's number and name in the first proposal (2026-10-13). "Added later" items came from findings
made while building the state chips: the overlay timeout, and the VIN missing from the logcat.

### 1. Keep a history per vehicle instead of overwriting one list (App) — initial #4

**Done** (2026-10-14), spec `dtc-history.md` (together with 8). Kept here for item 4, which builds on it.


* **Now:** `VehicleCapabilitiesManager.updateDTC` overwrites the single stored list (`pref.datalogger.dtc`).
* **Plan:** keep scans keyed by VIN (time, modules, codes with status, freeze frames). Small local store, capped per
  vehicle.
  * Before a clear, the current list is saved automatically, so freeze frames are not lost.
  * Per code: first seen, last seen, number of scans it appeared in.
  * The list marks codes **new since last scan** and offers "gone since last scan" (healed or cleared).
* **Unblocks:** 4 (new-code notifications) and a history view.
* **Risk:** VIN is often missing (see 8); without it, history falls back to "unknown vehicle".

### 2. Readiness monitors (App) — initial #3

* **Readiness is a feature of its own:** spec `readiness-monitors.md` (phone screen, optional AA screen, needs 01 01 /
  01 41 in ObdMetrics, which has neither yet).
* **Left on the DTC side:**
  * **Clear confirmation (phone + AA):** "Readiness monitors will reset; the car shows not-ready until they run
    again." Done, shipped with the readiness phone screen.
  * **DTC list row:** done. The last stored result ("MIL on · complete 3 / 7 · time") links to the readiness dialog;
    an accepted clear drops it.

### 3. Read Mode 07 and Mode 0A codes (ObdMetrics) — initial #2

* UDS `19 02` with the status byte already covers pending for UDS modules (the "Pending" chip).
* The generic OBD-II legacy header needs Mode 07 (pending) and Mode 0A (permanent). ObdMetrics has no Mode 07 / 0A
  definitions yet (its resources stop at modes 01–05 and 09).
* Permanent codes cannot be cleared by a tool. Without them, a driver who clears and sees the MIL return does not
  know why. Show them as their own section with an explanation.

### 4. Tell the driver when the connect scan finds new codes (App, needs 1) — initial #6

**Done** (2026-10-14), spec `dtc-new-code-notification.md`.

* The connect scan already runs and raises `DATA_LOGGER_DTC_AVAILABLE`, but only the phone reacts, and it does not
  know which codes are new.
* **Plan:** a phone notification and an AA alert, only for codes not in the previous scan, opening the DTC list.

### 5. Clear safely, and clear one module at a time (App) — initial #5

* **Clear one module:** "Clear this module" from a module header. `scheduleDTCCleanup(selectedModules)` already takes
  a set.
* **Pre-check:** before sending, check ignition on / engine off (RPM = 0, or vehicle status). This avoids the
  `7F 14 22` refusal instead of only reporting it afterwards.
* **Confirmation:** mention that freeze frames are erased (saved in history per 1) and that readiness monitors reset.

### 6. Scan progress (ObdMetrics + App) — added later

* A 13-module scan with snapshots takes ~13 s behind an overlay that says only "please wait".
* **Plan:** report per-module progress ("Reading Engine ECU · 4 / 13"), which needs a progress callback from
  ObdMetrics' DTC handler.
* Optionally show the codes as each module finishes.

### 7. Better descriptions: failure type text (App / ObdMetrics) — initial #7

* The suffix in `P0120-14` (failure type byte, SAE J2012 / ISO 15031-6) is shown raw.
* Add its meaning ("14 = circuit short to ground or open") to the expanded row and the report.
* Check whether `DiagnosticTroubleCode.failureType.description` is already filled; otherwise add the generic table.

### 8. VIN on every read (App) — added later

**Done** (2026-10-14), spec `dtc-history.md` part A: the VIN is read alone on every connect while DTC reading is on.


* Every read in the 2026-10-13 log stamped `vin=null`, because `pref.adapter.init.fetchDeviceProperties` is off.
  Read info then cannot spot another vehicle, and history (1) has no key.
* **Plan:** when no VIN is known, read it once (the profile's VIN DID, e.g. `22 F190`) as part of the DTC action.

### 9. A better shared report (App) — initial #8

* Add to the shared report: the state line (done), odometer at read time, scan duration, modules that did not answer.
* Export history as JSON / CSV.

### 10. Filters and AA polish (App) — added later

* **Phone:** filter chips (All / Active / Stored / Pending), useful with 30+ codes.
* **AA:** a state-specific icon per row. Today AA uses one fault icon, tinted, because rows allow no chips.

## To investigate (added later)

* **Legacy header scanned with UDS:** the scan includes "OBD-II (Legacy Mode 01)" (header `DB33F1`) and sends it the
  UDS `19 02` read. A legacy/functional header may need Mode 03 instead, or should be skipped by the UDS scan.
* ~~**Modules that do not answer**~~: done, `dtc-module-response.md`.

## Out of scope

* Coding, adaptations, service resets (OBDeleven / AlfaOBD territory).
* Online code database lookups beyond the existing web search per code.
