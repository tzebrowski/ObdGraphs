# Spec: DataLoggerService background-restart crash fix

2026-10-03 · branch `fix/fgs-background-restart`

## Summary

`DataLoggerService` no longer crashes when Android restarts it in the background; it stops itself instead of calling `startForeground()`. One commit (`0215c176`), 3 files, +80 / −5 lines.

**Problem.** The service returned `START_STICKY`, so after the process was killed the system re-created it with a `null` intent. `onStartCommand` then called `startForeground()` unconditionally. From the background, Android 12+ (API 31) refuses that with `ForegroundServiceStartNotAllowedException`, an `IllegalStateException`. The existing handler caught only `SecurityException`, so the exception escaped and crashed the app in production.

**Scope.** `:datalogger` only — `DataLoggerService.kt`, its Robolectric test, and a `CLAUDE.md` update. No UI, preference or string changes.

## Behaviour changes

Every path through `onStartCommand` now returns `START_NOT_STICKY`, and every failure to go foreground ends with the service stopped rather than half-started.

| Scenario | Before | After |
| --- | --- | --- |
| System restart (`intent == null`) | `startForeground()` called; crash on API 31+ in background | Logs a warning, `stopSelf()`, returns `START_NOT_STICKY`; no notification shown |
| `startForeground()` throws `ForegroundServiceStartNotAllowedException` | Uncaught, process crashes | Caught as `IllegalStateException`, service stopped, command not executed |
| `SecurityException`, fallback `CONNECTED_DEVICE` type succeeds | Continued | Unchanged: continues with the command |
| `SecurityException`, fallback also fails (API 29+) | Stopped, but `onStartCommand` still dispatched the command | Stopped and returns early; command not executed |
| `SecurityException` on API < 29 (no fallback) | Swallowed; command ran without foreground | Service stopped; command not executed |
| Normal command intent, foreground succeeds | Command dispatched, `START_STICKY` | Command dispatched, `START_NOT_STICKY` |

Consequence of `START_NOT_STICKY`: if the process is killed mid-session, the service is not revived. That revival never carried a command, so no running logging session is lost that was previously recovered.

## Implementation

All code changes are in `datalogger/src/main/java/org/obd/graphs/bl/datalogger/DataLoggerService.kt`.

1. **Null-intent guard** at the top of `onStartCommand`: logs `"Ignoring a restart without a command"`, calls `stopSelf()`, returns `START_NOT_STICKY`. Because `intent` is now smart-cast non-null, `intent?.action` became `intent.action`.
2. **`startForegroundServiceSafe()` returns `Boolean`** — `true` once `startForeground()` (primary or fallback type) succeeds, `false` when the service could not go foreground and has been stopped. `onStartCommand` returns `START_NOT_STICKY` immediately on `false`, before the permission check and action dispatch.
3. **New `catch (e: IllegalStateException)`** after the existing `SecurityException` handler, covering `ForegroundServiceStartNotAllowedException` (API 31+).
4. **Single exit for failures:** the `serviceStop()` call moved out of the inner fallback `catch` to the end of the function, so every failure path (fallback failed, no fallback below API 29, background refusal) runs `stopForeground(REMOVE)` + `stopSelf()` exactly once. `REQUEST_LOCATION_PERMISSIONS` is still broadcast only when the fallback fails.
5. **Return value** at the end of `onStartCommand` changed from `START_STICKY` to `START_NOT_STICKY`.

`CLAUDE.md` gains two entries: a `DataLoggerService` finding (null intent = system restart; stop, don't go foreground) and a project rule that every feature and bug fix ships with tests, with bug-fix tests verified to fail against the unfixed code.

## Tests

Three Robolectric tests were added to the existing `datalogger/src/test/java/org/obd/graphs/bl/DataLoggerServiceTest.kt`; run with `./gradlew :datalogger:testDebugUnitTest`.

| Test | Setup | Asserts |
| --- | --- | --- |Plugins
| `onStartCommand without an intent should stop instead of going foreground` | `onStartCommand(null, 0, 1)` | `START_NOT_STICKY`; `isStoppedBySelf`; no foreground notification |
| `onStartCommand should stop the service when going foreground is refused` | `spyk` service; `startForeground(id, n, type)` throws `ForegroundServiceStartNotAllowedException` | `START_NOT_STICKY`; `stopSelf()` called; orchestrator `start` never called |
| `onStartCommand should go foreground and not ask to be restarted after a kill` | `onStartCommand(Intent(), 0, 1)` | `START_NOT_STICKY`; foreground notification posted; not stopped |

The first two reproduce the bug: against the unfixed code the first goes foreground and returns `START_STICKY`, the second lets the exception escape. The third guards the happy path.

## Risks and verification

- **No auto-revival after a kill.** A logging session killed by the system stays stopped until the user (or Android Auto) sends a new command. Intended: the null restart could never resume a session anyway.
- **API < 29 `SecurityException` now stops the service.** Previously the command ran without foreground status. Low impact, but a behaviour change on old devices.
- **Background command intents are dropped, not deferred.** A real command arriving while the app cannot go foreground is discarded silently; no event is broadcast to the UI for the `IllegalStateException` path.

Verification:

- [ ] `./gradlew :datalogger:testDebugUnitTest` passes
- [ ] `./gradlew spotlessApply` leaves no diff
- [ ] Manual, API 31+ device: start logging, kill the process via `adb shell am kill`, confirm no crash in logcat and no orphan notification
- [ ] Manual: normal start/stop from phone and Android Auto still works

Open question: should the background-refusal path broadcast an error event so the UI stops showing "connecting"?
