# Spec: Readiness monitors

2026-10-13 · branch `feat/readiness-monitors` (ObdGraphs + ObdMetrics) · Status: phone screen implemented, manual
checks pending; AA screen, stored status and DTC summary row not implemented

## Summary

**Problem.** The engine ECU runs emission self-tests ("readiness monitors"). Each is reported as complete,
incomplete or not supported, together with the MIL (check-engine lamp) state and the number of emission codes. The
app shows none of this.

That leaves several questions unanswered:

* Is the car ready for an inspection or emissions test?
* Has a repair been verified (the monitor ran again and no code came back)?
* What did a clear reset? A clear sets every monitor back to incomplete; drivers find out at the inspection.

Torque, Car Scanner and OBD Auto Doctor all have a readiness screen.

**Decision: a feature of its own, not part of the DTC list.** It answers a different question ("has the car finished
checking itself?"). It matters with zero codes. It is best watched while driving (the "this drive cycle" view changes
live). It is one generic OBD request to the engine ECU, not a module scan. DTC keeps only small links to it
(see *DTC touchpoints*). Roadmap: `dtc-roadmap.md` item 2.

**Scope.**

* **ObdMetrics:** PID definitions and a decoder for 01 01 / 01 41.
* **`:datalogger`:** query and readiness model.
* **`:app`:** phone screen, EN/PL.
* **`:automotive`:** optional AA screen, EN/PL.

## Behaviour changes

| Area | Before | After |
|---|---|---|
| Menu (drawer + overflow) | — | "Readiness monitors" item after DTC, opens the readiness dialog directly (always shown) |
| Settings | — | "Readiness monitors" category under DTC, a sub-screen `pref.readiness` with the entry opening the dialog (shown even without DTC PIDs) |
| Readiness dialog | — | "MIL off · Emission codes: 0", "Since codes cleared" / "This drive" toggle, "Monitors complete: 5 / 7" (green when all), one row per supported monitor (green check = complete, orange hourglass = not complete, incomplete first), counters since clear / with MIL on |
| While the dialog is open | — | The query switches to the readiness PIDs; the replaced query is restored on close |
| PID pickers | — | Readiness PIDs never listed (they decode to a status, not a number) |
| DTC clear confirmation (phone + AA) | Erases codes and freeze frames | + "Clearing also resets the readiness monitors…" |
| AA | — | Not yet: "Readiness" screen like the DTC screen (proposed) |
| Readiness dialog, help | — | Collapsed "How do monitors relate to trouble codes?" section explaining monitors vs codes, clear reset, repair check |
| DTC dialog | — | Link row under the read info: "Readiness monitors: MIL on · complete 3 / 7 · <time> ›" from the last stored result, or "check whether the car has finished its self-tests ›"; tap opens the readiness dialog, the row refreshes when it closes |
| After an accepted DTC clear | — | The stored readiness result is dropped (the clear reset the monitors); kept after a refused clear |

## Data

**01 01, monitor status since DTCs cleared** (SAE J1979; 4 data bytes A–D):

| Byte / bits | Meaning |
|---|---|
| A7 | MIL on |
| A0–A6 | number of emission-related DTCs |
| B3 | 0 = spark ignition (petrol), 1 = compression ignition (diesel) — selects the C/D meaning |
| B0–B2 | supported: misfire, fuel system, components (continuous) |
| B4–B6 | incomplete: misfire, fuel system, components |
| C0–C7 | supported, non-continuous monitors |
| D0–D7 | incomplete, same bit order as C (1 = not complete) |

Non-continuous monitors, bits 0–7:

* **Petrol:** catalyst, heated catalyst, evaporative system, secondary air, A/C refrigerant, O2 sensor,
  O2 sensor heater, EGR/VVT.
* **Diesel:** NMHC catalyst, NOx/SCR aftertreatment, (reserved), boost pressure, (reserved), exhaust gas sensor,
  PM filter, EGR/VVT.

**01 41, monitor status this drive cycle:** same layout, A not used; "enabled this cycle" instead of "supported".
**Verify the exact 01 41 bit meanings against J1979 before implementing**; the 01 01 table above is the well-known layout.

**Already in ObdMetrics (`mode01_2.json`):** these show how long ago the last clear was, and how long the MIL has been on:

* 01 31 distance since codes cleared (id 50)
* 01 4E time since codes cleared (id 79)
* 01 21 distance with MIL on (id 34)
* 01 4D time with MIL on (id 78)

**Missing in ObdMetrics:** 01 01 and 01 41 (no definition, no decoder).

## Settings / keys touched

* `pref.readiness.last`: the last since-clear result (`ReadinessSnapshot.encode()`: time;MIL;codes;complete;supported),
  written by the readiness dialog, removed by an accepted clear. Not per vehicle yet.
* `pref.readiness.category` / `pref.readiness` (sub-screen) / `pref.readiness.list` (`preferences.xml`): the entry,
  no stored value. The menu item `ctx_menu_readiness` navigates to `pref.readiness`, and
  `PreferencesFragment.openPreferenceDialogFor` opens the dialog for it.
* Proposed with the AA screen: `pref.aa.readiness.enabled` (AA "Available screens", default `false`).

## Implementation

* **ObdMetrics `feat/readiness-monitors`:**
  * 01 01 (id 21010) and 01 41 (id 21011) in `mode01.json`, `"category": "readiness"`, length 4.
  * `ReadinessMonitorsCodec` decodes into `ReadinessStatus(milOn, emissionDtcCount, compressionIgnition,
    monitors)`. Each entry is a `ReadinessMonitor(id, supported, complete)`, where `ReadinessMonitorId` is a
    stable key and display names come from the app. Reserved diesel bits are left out. Only the first reply's
    4 bytes are used (a functional request can be answered by several ECUs). A short reply decodes to null.
  * `BatchCodecTestRunner` now skips a query's first token: it is the mode, and "01" began resolving to PID 01.
  * `ConnectorResponseDecoder`: the reply to UDS `10 01` (queued before every query update, no PID) threw an NPE
    that ended the decoder thread. Opening the dialog therefore stopped all decoding (readiness, then DTC reads)
    until a reconnect. PID-less replies are skipped and a failing reply no longer ends the thread
    (`ConnectorResponseDecoderTest`).
  * `CachedBatchMessageDecoder`: a PID missing from a batch reply is searched for again from after the answer
    code, not from 0. The 2.0 GME answers `01 01 41 31` with `410188077171` (01 01 only); PID 41 matched the
    reply's `41` header and decoded a bogus drive-cycle status (diesel, wrong monitors).
* **`:datalogger`:**
  * `Pid` ids for the six PIDs.
  * `QueryStrategyType.READINESS_QUERY` → `ReadinessQueryStrategy`, a fixed `READINESS_PIDS`.
  * `bl/readiness/Readiness.kt` (Android-free): `isReadinessPid()` (by category), `summary()`, `shownMonitors()`,
    and `ReadinessState.update(pidId, value, units)`, which folds replies in.
  * `DataLoggerRepository.runningQuery()`, so the dialog can put back the query it replaced.
  * `DataLoggerRepository.observeEach()`: every metric, posted to the main thread one by one. The dialog first used
    `observe()` (LiveData `postValue`, keeps only the latest): 01 01 and 01 41 are decoded from one reply and
    published back to back, so 01 01 never arrived and the dialog stayed at "waiting" on the 2.0 GME.
* **`:app`:**
  * `ReadinessListPreferences` + `ReadinessDialogFragment` (`dialog_readiness.xml`, rows built in code).
  * The PID pickers drop readiness PIDs (`PidDefinitionViewModel`).
  * Menu item next to DTC (`left_nav_menu.xml`, `bottom_nav_menu.xml`, `NavigationRouter`). Wrong turn: an entry in
    the root settings only; drivers reach DTC through the menu, which opens just the `pref.dtc` sub-screen, so the
    entry was never seen.
  * Clear-confirmation strings on phone and AA mention the reset.
* DTC link: `ReadinessSnapshot` / `resetsReadiness()` in `Readiness.kt`; row `dtc_readiness_link` in `dialog_dtc.xml`.
  The DTC dialog shows the readiness dialog from its `childFragmentManager` and refreshes the row in
  `onFragmentViewDestroyed`; `WorkflowOrchestrator` drops the stored result after an accepted clear.
* **Not done:** the AA screen, the stored result per vehicle (VIN).

## Backward compatibility

New PIDs and screens only. Profiles without 01 01 support (the ECU's supported-PIDs bitmap) hide the entry or show
"not supported by this vehicle".

## Tests

* ObdMetrics `ReadinessMonitorsTest` covers:
  * registration and category;
  * a petrol reply, all complete;
  * MIL on with a code count and incomplete monitors;
  * an incomplete continuous monitor;
  * a diesel reply, with the reserved bits skipped;
  * 01 41 ignoring byte A;
  * a second ECU's reply being ignored;
  * short or missing replies decoding to null.

  The full ObdMetrics suite passes (1145 tests).
* ObdMetrics `UnansweredPidBatchCodecTest`: the car's replies to `01 01 41 31` / `01 4E 21 4D`; fails without the
  decoder fix.
* `:datalogger` `ReadinessTest` also covers snapshot encoding (and malformed values) and which clear outcomes drop it.
* `:datalogger` `ReadinessTest`: summary, shown monitors, state folding, undecoded replies, PID category.
* `:datalogger` `MetricsObserverTest`: `observeEach` delivers both metrics of a batch, and stops after destroy.

## Risks and verification checklist

* [x] 2.0 GME: 01 01 answered (`41 01 88 07 71 71`: MIL on, 8 emission codes; catalyst, A/C, O2 sensor and O2
  heater incomplete). 01 41, 01 31, 01 4E and 01 4D are not answered; 01 21 is.
* [ ] Monitor set matches what other tools show.
* Drive-cycle view on an ECU without 01 41 (2.0 GME): once 01 01 has been answered, the dialog says the vehicle does
  not report it (`ReadinessState.isNotReported`) instead of waiting for good. 01 01 and 01 41 share one batch, so a
  missing 01 41 after a 01 01 reply is final.
* The dialog window has a 16 dp side inset (`readiness_dialog_side_inset`); without it the card touched the screen edges.
* [ ] After a clear, all supported non-continuous monitors read incomplete; they complete after a drive cycle.
* [ ] 01 41 bit meanings checked against J1979 (see *Data*).
* [ ] Polling 01 01 / 01 41 does not slow the other live screens (only active while the dialog is open).
* [ ] Closing the dialog puts back the previous query; leaving the settings returns screens to their own PIDs.
* [ ] The dialog in light and dark theme; the counters show sensible units (km, min).
* Needs the ObdMetrics snapshot with the codec (`mvn install` of `feat/readiness-monitors`) until it is released.
* Some ECUs report "not supported" for monitors they do run, or report the A/C bit (obsolete); show what the ECU says.

## Out of scope

* Mode 06 (on-board monitoring test results, the values behind each monitor).
* Drive-cycle guidance ("drive at 80 km/h for 10 min to complete the catalyst monitor"): it is vehicle-specific.
