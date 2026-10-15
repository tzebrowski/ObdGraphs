# Spec: tell the driver when the connect scan finds new codes

2026-10-14 · branch `feat/dtc-new-code-notification` (ObdGraphs only) · Status: implemented, on-device checks pending

Roadmap item 4 (`dtc-roadmap.md`), built on the per-vehicle history (`dtc-history.md`) and the module response
statuses (`dtc-module-response.md`).

## Summary

**Problem.**
* The automatic scan on connect finds codes, but the driver learns about them only by opening the DTC dialog. On
  Android Auto nobody does that while driving.
* The only alert was a toast "DTC are available" (setting off by default). It fired on every connect while any code was
  stored, new or not, and only with the phone app in the foreground.

**Scope.** `:datalogger` (decision + notification), `:common` (strings, navigation extra), `:app` (setting, opening
the DTC list from the notification, old toast removed), `:automotive` (car toast), EN + PL.

## Behaviour changes

| Area | Before | After |
|---|---|---|
| Connect scan finds a code the vehicle never reported | Toast "DTC are available" if the setting was on, app in foreground only | Pop-up (heads-up) phone notification "New trouble codes (N)", one line per code (`P0123-1A · Engine ECU`) |
| Connect scan, only known codes | Same toast, every connect | Nothing |
| Tap on the notification | — | Opens the app on the DTC settings screen with the DTC list dialog open |
| Android Auto, main (surface) screen | Nothing | Car toast "New trouble codes (N): P0123-1A · Engine ECU, ..." |
| Manual read / clear | Nothing | Nothing (the result is on screen) |
| Vehicle's first scan (e.g. after a fresh install) | Toast (if on) | Notification "Trouble codes found (N)" with every code; AA "Trouble codes found" |
| Vehicle without a VIN reply | Toast (if on) | Nothing: its codes may come from any car (`NO_VIN`, a VIN not programmed, does announce) |
| Many codes | — | Notification lists 8, then "+N more"; AA toast lists 3 |
| Setting | "Enable DTC notifications", off | "Notify about new trouble codes", **on** by default |

"New" is the history's definition (`DtcHistoryDiff.newCodes`): in this scan, in no earlier scan of the vehicle that
looked at the code's module. An intermittent code coming back is not new; a module that did not answer a scan does
not count as having looked.

## Settings / keys touched

| Key | Type | Notes |
|---|---|---|
| `pref.dtc.notify_new_codes` | Boolean, default `true` | New. Gates both the phone notification and the AA toast. Depends on `pref.adapter.init.fetchDTC` |
| `pref.dtc.show_notification` | Boolean | Removed from the UI and code. A stored value is ignored. New key, so the feature is on for users whose old switch was persisted as `false` by merely opening the screen |

## Implementation

* `dtc/DtcNewCodes.kt` (`:datalogger`):
  * `dtcAnnouncement(kind, vin, dtc, scans, firstScan)`: null unless `kind == CONNECT` and the vehicle has a history
    key other than `UNKNOWN`; on the vehicle's first scan every code, otherwise the codes `diff.isNew`, in display
    order (`sortedForDisplay`). Result `DtcAnnouncement(lines, firstScan)`, also the broadcast's extras.
  * `firstScan` is read in `updateDTC` **before** the scan is appended. Wrong turn: "history holds one scan" after
    appending. History replaces a connect scan equal to the previous one, so a vehicle whose codes never change keeps
    one scan, and every connect announced all its codes again (caught by `VehicleCapabilitiesManagerTest`).
  * `announcementLine()`: display code + module name (none for the default module).
  * `DtcNewCodesAnnouncer.announce`: called at the end of `VehicleCapabilitiesManager.updateDTC` with
    the read's VIN, so both connect paths (module scan, ObdMetrics init read) are covered. When the
    setting is on: broadcast `DATA_LOGGER_DTC_NEW_CODES` (extras: `DtcAnnouncement`) and post the
    notification. The notification is posted from the service process, not the activity's receiver: the activity's
    receivers are gone once it is destroyed, the service runs while driving. A failure to notify is logged and never
    breaks storing the scan.
  * Notification: channel `dtc_alerts`, **high importance** so it pops up (heads-up); id 12347, small icon as the other
  notifications. Wrong turn: default importance (first channel `dtc_new_codes`) only put it in the shade, no pop-up.
  A channel's importance cannot be changed from code once created on a device, hence the new id.
    Its `PendingIntent` uses its own request code: launch intents differ only by extras, and the code-0 launch intent
    of the service / GPS notifications would otherwise replace the navigation extra.
* Opening the list: the launch intent carries `EXTRA_NAVIGATE_TO = "dtc"` (constant moved to `:common`).
  `MainActivity` (singleTop, `onNewIntent` / `onResume`) calls `navigateToPreferencesScreen("pref.dtc", openDialog = true)`;
  `PreferencesFragment` opens `DiagnosticTroubleCodePreferenceDialogFragment` only when `PREFERENCE_SCREEN_OPEN_DIALOG`
  is set, so the menu entry still opens the plain DTC screen.
* AA: `SurfaceRendererScreen` shows a `CarToast` on `DATA_LOGGER_DTC_NEW_CODES`. Other AA screens do not listen; the
  phone notification is still posted.
* `DATA_LOGGER_DTC_AVAILABLE` is still broadcast (instrumented test relies on it); the app no longer reacts to it.

## Backward compatibility

* History stored before this change works as is; the first scan after an update compares against it.
* Existing users get the notification on by default (new key). The Android 13+ notification permission is requested
  by the app at startup already; without it `Notifications.show` logs and skips.

## Tests

* `DtcNewCodesTest` (done): connect scan announces only new codes; read / clear announce nothing; first scan announces
  every code (none when empty); no VIN reply announces nothing, `NO_VIN` does; known codes are not announced again;
  display order; line format; line cap.
* `VehicleCapabilitiesManagerTest` (done), end to end through `updateDTC` and the history file: new code → broadcast
  with its line + notification; first scan → all codes, the same codes on the next connect (history replaces the
  scan) and a manual read → nothing; setting off → nothing; no VIN → nothing.
* [ ] Phone: connect with a new code on the car (or after deleting it from `files/dtc_history.json`): notification,
  tap opens the DTC list. Quick tap check without the car:
  `adb shell am start -n <pkg>/org.obd.graphs.activity.MainActivity --es org.obd.graphs.EXTRA_NAVIGATE_TO dtc`.
* [ ] AA (DHU): car toast on the main screen; PL text length.

## Risks

* A CarToast is short and easy to miss; a heads-up AA notification (`CarAppExtender`) is the alternative if drivers
  miss it.

## Out of scope

* Announcing codes found by the periodic / manual reads.
* An AA notification with an action opening the AA DTC screen.
* Per-severity filtering (e.g. only MIL codes).
