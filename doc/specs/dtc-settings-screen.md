# Spec: DTC settings screen and dialog buttons

2026-10-19 · branch `feat/dtc-settings-screen` (ObdGraphs only) · Status: implemented, on-device check pending

## Summary

**Problem.** The DTC settings screen (`pref.dtc`) was one flat list: the row opening the code list next to four
switches. The row's summary was static ("Tap to view active DTCs retrieved from the ECU"), though the list also holds
stored, pending and permanent codes from several modules. "Enable Automatic DTC readings" said *while connected*; the
codes are read once, on connect. "Enable Automatic DTC cleanup" erases codes on every connect without asking and resets
the readiness monitors, and said none of it.

The DTC dialog closed with four equal filled buttons in two rows (Refresh, Clear Codes / Share, Close): ~100 dp of a
phone screen taken from the list, and the destructive Clear Codes as prominent as Refresh.

**Scope.** `:app` (preference XML, list row, DTC dialog buttons, strings EN + PL); one constant made public in
`:datalogger`.

## Behaviour changes

| Area | Before | After |
|---|---|---|
| Layout | One category, five rows | Trouble codes (list row) · On connect (read, notify) · Reading (freeze frames) · Clearing (auto clear, last) |
| List row title | "View Current DTCs" | "Trouble codes" |
| List row summary | Static "Tap to view active DTCs …" | "3 codes · Last read: <date time> · VIN …"; "No codes · …"; "Not read yet. Tap to open the list and read the codes." before the first read. Follows each read while the screen is open |
| Read on connect | "Enable Automatic DTC readings" / "… while connected to the ECU" | "Read codes on connect" / "Scans the configured modules once the adapter connects." |
| Notification | "Notify about new trouble codes", 5-line summary | "Notify about new codes", shorter summary |
| Snapshots | "Read DTC snapshots" / "Reads additional context data when DTC was taken." | "Read freeze frame data" / "… Reads take longer." (matches the dialog's "Freeze Frame Data") |
| Auto clear | "Enable Automatic DTC cleanup" / "Automatically clears DTCs from the ECU memory." | "Clear codes on connect" / ⚠ warning: every connect, without asking, resets readiness monitors; points to Clear Codes |
| DTC dialog buttons | 2 × 2 filled buttons, caps | One row: Share (icon only), Close (text) · Clear Codes (outlined red, like "Clear this module") · Refresh (the only filled button). Share and Clear Codes dim when there are no codes |
| `pref.dtc` entry in the root settings | "Tap to view active DTCs …" | "Read, view and clear the trouble codes stored in the vehicle's modules." |

## Settings / keys touched

No key, default or dependency changes. Strings changed: `pref.dtc.settings`, `.settings_summary`, `.read_snapshots*`,
`.enable_dtc_reading*`, `.enable_automatic_cleanup*`, `.notify_new_codes*`. New: `pref.dtc.list_not_read`,
`.list_no_codes`, plurals `pref.dtc.list_codes`, `pref.dtc.category.on_connect` / `.reading` / `.clearing` (EN + PL).

## Implementation

* `preferences.xml`: the five rows split into four categories; the list row has no XML summary.
* `DiagnosticTroubleCodeListPreferences`: sets its summary in `onAttached` and again whenever
  `PREF_DTC_READ_INFO` changes (a `SharedPreferences` listener held in a field: listeners are kept weakly). Every read
  and clear goes through `updateDTC`, which rewrites the read info, so that key covers them all.
* `dtcListSummary` (pure, tested) picks the text; `readInfoSummary(context, info)` builds the "Last read … VIN …" line
  and is now shared with the DTC dialog (`refreshReadInfo`), so both read alike.
* `dialog_dtc.xml` `button_container`: one horizontal row of `MaterialButton`s, ids unchanged (`CoreDialogFragment`
  finds `action_close_window` as a `Button`). Labels are not in caps, so the PL ones ("Zamknij", "Skasuj kody",
  "Odśwież") fit one row; the dialog's `minWidth` is 350 dp. Share's icon is `dtc_share`. Fixed text / icon colours
  ignore the disabled state, so `setEnabledDimmed` sets alpha 0.38 too.
* `PREF_DTC_READ_INFO` (`VehicleCapabilitiesManager`) is public for the listener.
* Auto clear stays (user's choice: warn, not remove). It is ObdMetrics' init clear on the default header (the engine
  ECU), so the warning names that module, not all of them.

## Backward compatibility

Keys and defaults unchanged, so stored settings carry over. Only labels and grouping move.

## Tests

`DtcListSummaryTest` (`:app`, plain JUnit): count + read line; "No codes"; "Not read yet" before the first read, even
with codes stored; PL plurals have one / few / many / other; the auto-clear warning names the clear button
(`dtc.action_clear_codes`) in EN and PL. `DtcDialogButtonsTest` (parses `dialog_dtc.xml`): one row in the order
Share, Close, Clear Codes, Refresh; Refresh filled, Clear Codes outlined; Share has a content description.

## Risks and verification checklist

- [ ] Phone, EN and PL: the four categories show; the auto-clear warning fits without clipping.
- [ ] List row: "Not read yet" on a fresh install; after a read "N codes · Last read …"; with the screen open, the
  summary updates when the connect scan finishes.
- [ ] Turning "Read codes on connect" off still greys out notification and auto clear.
- [ ] DTC dialog, EN and PL at phone width: the four buttons fit one row, no label wraps or is cut
  (`adb shell uiautomator dump`); Share and Clear Codes dimmed with no codes; "Kasowanie…" during a clear still fits.
- [ ] Root settings `pref.dtc` entry shows the new summary.

## Out of scope

* Asking for the notification permission when the notification switch is turned on.
* Removing the automatic clear, or making it clear the configured modules instead of the default header.
