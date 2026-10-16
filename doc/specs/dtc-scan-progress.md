# Spec: DTC scan progress

2026-10-16 · branches `feat/dtc-scan-progress` (ObdGraphs + ObdMetrics) · Status: implemented, on-device checks pending

Roadmap item 6 (`dtc-roadmap.md`).

## Summary

**Problem.** A read or clear of 13 modules (with freeze frames) takes ~13 s behind an overlay that only says "This
might take a while. Please wait…". Nothing shows that the scan is moving or how far it is.

**Scope.** ObdMetrics (progress callback), `:datalogger` (broadcast, text), `:app` (overlay), `:common` (EN + PL).

## Behaviour changes

| Area | Before | After |
|---|---|---|
| Phone overlay during a DTC read / clear of modules | "This might take a while. Please wait…" | Same, plus a second line: "Engine ECU · 4 / 13", updated as each module starts |
| Same, once all modules are read and freeze frames are requested | — | "Reading code details · 12" (number of codes) |
| Default-ECU read (no modules configured) | Overlay text | Unchanged: no per-module step to report |
| Any other overlay (connect, trips) | — | Unchanged: updates apply only to the overlay the DTC dialog put up |
| AA | "Reading trouble codes, please wait…" | Unchanged (see Out of scope) |

## Settings / keys touched

None.

## Implementation

* **ObdMetrics.** `DtcProgress` (`command.dtc`: phase `MODULE` / `FOLLOW_UPS`, module, 1-based index, total) and
  `Lifecycle.onDTCProgress(DtcProgress)` (default no-op, forwarded by `Subscription`).
  `ExecutionContext.scheduleDTCAction` queues a `DiagnosticTroubleCodeProgressCommand` ahead of each module's `ATSH`.
  Commands run one at a time, so the marker executes when the previous module is done; this also covers a module
  that never answers, which no reply would report. `DiagnosticTroubleCodeHandler` executes the marker (calls
  `onDTCProgress`, sends nothing to the adapter) and reports `FOLLOW_UPS` with the code count before the per-code
  requests.
* **App.** `WorkflowOrchestrator.onDTCProgress` broadcasts `DATA_LOGGER_DTC_PROGRESS` with a `DtcScanProgress`
  (`dtc/DtcScanProgress.kt`, Serializable, with `text(getString)`). The DTC dialog's `ScreenLock` carries
  `DTC_SCREEN_LOCK_CONTEXT`; `ScreenLockManager.updateProgress(context, text)` sets the second line only while an
  overlay with that context is showing. `MainActivity`'s receiver handles the event.
* The overlay timeout (`dtcActionTimeoutMs`) is unchanged.

## Backward compatibility

* ObdMetrics: new default method, existing `Lifecycle` implementers are unaffected. The marker command never reaches
  the adapter.
* App: an older ObdMetrics without `onDTCProgress` does not compile against this; the dependency is the snapshot.

## Tests

* ObdMetrics `DiagnosticTroubleCodeReadingTest`: progress per module in order for a module that answers and for one
  that never answers (NO DATA); `FOLLOW_UPS` with the code count after the modules when snapshots are read; no
  progress for a default-header read.
* App `DtcScanProgressTest`: conversion from `DtcProgress`, the texts, EN / PL strings with their arguments.

## Risks and verification checklist

* Progress is broadcast for every DTC action, the connect scan included. A read queued behind the connect scan
  (`DtcActionQueue`) therefore shows the connect scan's modules first, then starts again at 1 for its own.
* [ ] Phone: read all modules on the car; the second line counts up to N / N, then "Reading code details".
* [ ] Clear: same counter.
* [ ] PL text.
* [ ] A module that does not answer still advances the counter (the next module shows after its timeout).

## Out of scope

* AA: the loading `ListTemplate` has only its title for text; changing the title on every module may count as a new
  template step against the host's quota. Needs a DHU check first.
* Showing codes as each module finishes.
* A progress bar (the overlay keeps its spinner).
