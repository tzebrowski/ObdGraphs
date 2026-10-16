# Spec: DTC scan progress

2026-10-16 · branches `feat/dtc-scan-progress` (ObdGraphs + ObdMetrics) · Status: implemented, on-device checks pending

Roadmap item 6 (`dtc-roadmap.md`).

## Summary

**Problem.** A read or clear of 13 modules (with freeze frames) takes ~13 s behind an overlay that only says "This
might take a while. Please wait…". Nothing shows that the scan is moving or how far it is. The scan on connect is
worse: it ran with no overlay at all, while ObdMetrics holds back live data until it is done, so the gauges froze (and
went stale after 10 s) with no explanation.

**Scope.** ObdMetrics (progress callback), `:datalogger` (broadcast, text), `:app` (overlay), `:common` (EN + PL).

## Behaviour changes

| Area | Before | After |
|---|---|---|
| Phone overlay during a DTC read / clear of modules | "This might take a while. Please wait…" | Same, plus a second line: "Engine ECU · 4 / 13", updated as each module starts |
| Same, once all modules are read and freeze frames are requested | — | "Reading code details · 12" (number of codes) |
| Scan on connect (any screen, e.g. gauges) | No overlay; gauges freeze for the scan | Overlay "Reading trouble codes. Live data is paused until the scan finishes…" with the same module counter; closed when the scan completes. Cancel only hides it, the scan goes on |
| Manual read pressed during the connect scan | — | The connect scan's completion leaves the overlay up: the waiting read's own overlay continues |
| Default-ECU read (no modules configured) | Overlay text | Unchanged: no per-module step to report |
| Any other overlay (connect, trips) | — | Unchanged: updates apply only to a DTC overlay (DTC dialog or scan on connect) |
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
* Scan on connect: `WorkflowOrchestrator.send` broadcasts `SCREEN_LOCK_PROGRESS_EVENT` with `connectScanScreenLock`
  (same context and timeout as a manual read, cancel shown) once a `CONNECT` request is queued; `onDTCCompleted`
  sends `SCREEN_UNLOCK_PROGRESS_EVENT` for a finished `CONNECT` when no request waits behind it. Error / stop paths
  already unlock.

## Backward compatibility

* ObdMetrics: new default method, existing `Lifecycle` implementers are unaffected. The marker command never reaches
  the adapter.
* App: an older ObdMetrics without `onDTCProgress` does not compile against this; the dependency is the snapshot.

## Tests

* ObdMetrics `DiagnosticTroubleCodeReadingTest`: progress per module in order for a module that answers and for one
  that never answers (NO DATA); `FOLLOW_UPS` with the code count after the modules when snapshots are read; no
  progress for a default-header read.
* App `DtcScanProgressTest`: conversion from `DtcProgress`, the texts, the connect-scan overlay (context, timeout,
  cancel), EN / PL strings with their arguments.

## Risks and verification checklist

* Progress is broadcast for every DTC action, the connect scan included. A read queued behind the connect scan
  (`DtcActionQueue`) therefore shows the connect scan's modules first, then starts again at 1 for its own.
* The connect overlay blocks the phone UI for the scan (~13 s, longer with freeze frames), on every connect with
  modules configured. Wanted by the user: the driver must know metrics are not updated. Cancel hides it.
* [ ] Connect on the gauge screen: overlay with counter appears after connecting, closes when the scan ends, gauges
  resume.
* [ ] Cancel on the connect overlay: it closes, the scan still completes (list updated, notification if new codes).
* [ ] Phone: read all modules on the car; the second line counts up to N / N, then "Reading code details".
* [ ] Clear: same counter.
* [ ] PL text.
* [ ] A module that does not answer still advances the counter (the next module shows after its timeout).

## Out of scope

* Keeping live data flowing between modules (ObdMetrics queues the whole scan ahead of live PIDs).
* AA: the loading `ListTemplate` has only its title for text; changing the title on every module may count as a new
  template step against the host's quota. Needs a DHU check first.
* Showing codes as each module finishes.
* A progress bar (the overlay keeps its spinner).
