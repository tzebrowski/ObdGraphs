# CLAUDE.md (org.obd.graphs)

Project-specific knowledge for agents. **Maintained incrementally — when you
learn something non-obvious (a finding, a pitfall, an architecture decision
you made), add it here as part of the same change/PR.** Keep entries terse:
a rule, its reason, and the file it lives in. Cut narrative history; keep the
lesson. Update or delete entries that turn out to be wrong.

## 🤖 AI Assistant Directives (Token & Context Management)
* **Aggressive Context Management:**
    * You MUST monitor context size. Prompt the user to use `/compact` mid-task if the conversation history grows too long (to prevent >150k token context bloat and expensive cache reads).
    * Remind the user to use `/clear` when switching to a completely new task or a different module. Do not carry stale context.
* **Subagent & Fork Efficiency:** When spawning subagents or using "forks", keep instructions strictly scoped to prevent runaway loops. If performing simple file-system reads, prefer cheaper models (like Haiku) if the environment allows it.
* **Update this file after every feature or fix — it is part of "done".** Before handing the
  change over, add what the next session would otherwise have to rediscover: the architecture
  decision and why, pitfalls hit, wrong turns and what proved them wrong (lessons learned).
  Correct or delete entries the work disproved. Stage the `CLAUDE.md` edit with the change itself.
* **Brevity is required:** Provide code solutions directly. Omit preamble, conversational filler, and lengthy explanations unless explicitly requested.
* **Progressive Disclosure:** Do not assume the entire architecture up front. If deep context is needed for a specific module (e.g., `:datalogger`), read its local `README.md` before writing code.
* **Targeted Fixes:** When fixing existing files, output only the modified blocks or reference specific line numbers rather than re-writing the entire file.
* **Ignored Paths:** Do not ingest, search, or read files in any `build/`, `.gradle/`, or generated code directories (e.g., KAPT/KSP outputs). Avoid reading large binary assets in `res/drawable-*` or `.apk`/`.aab` files.
* **Tooling Reliance:** Do not act as a syntax linter or formatter. Rely on the user running Spotless and Android Studio's native linting.

---

## 🛠 Build & Command Line Cheat Sheet

### Key Gradle Commands
* **Clean Project:** `./gradlew clean`
* **Assemble Debug APK:** `./gradlew assembleDebug`
* **Run Lint Check:** `./gradlew lint`
* **Code Formatting (Spotless):** `./gradlew spotlessApply`

### Testing Commands
* **Run All Instrumented Tests:** `./gradlew connectedAndroidTest`
* **Run Unit Tests (if added):** `./gradlew test`
* **Run Tests for a Specific Flavor:** e.g., `./gradlew connectedGiuliaAADebugAndroidTest`

### Build Variants & Target Flavors
The application uses the `version` flavor dimension. Combine the flavor and the build type (e.g., `giuliaAADebug`, `giuliaRelease`, etc.).
* **Giulia Android Auto:** `./gradlew assembleGiuliaAADebug`
* **Giulia Performance Monitor:** `./gradlew assembleGiuliaPerformanceMonitorDebug`
* **Standard Giulia:** `./gradlew assembleGiuliaDebug`

---

## 🧪 Testing & Quality Assurance

### Test Configuration
The project is set up to use the standard AndroidX test runner, with some specific configuration flags disabled:
* **Test Runner:** `androidx.test.runner.AndroidJUnitRunner`
* **Functional Testing Flag:** Disabled (`testFunctionalTest false`)
* **Profiling Flag:** Disabled (`testHandleProfiling false`)

### Included Testing Frameworks
When writing new tests, use the natively provided libraries within the `androidTest` source set:
* **UI Testing:** Espresso Core (v3.5.1)
* **Test Execution & Rules:** AndroidX Test Runner (v1.5.2) and Rules (v1.5.0)
* **Kotlin Extensions:** AndroidX Core KTX (v1.5.0) and JUnit KTX (v1.1.5)

> **Note on Unit Tests:** `:app` has a plain JUnit suite for Android-free logic (e.g.
> `PidDefinitionDialogModeTest`, `./gradlew :app:testGiuliaDebugUnitTest`). `:datalogger` does have a local JVM suite under `src/test` running on
> Robolectric (`./gradlew :datalogger:testDebugUnitTest`). `:screen_renderer` has plain JUnit
> tests for pure layout logic (`./gradlew :screen_renderer:testDebugUnitTest`) — keep Canvas-free
> logic in testable functions such as `TripInfoMetrics`. Other modules define only
> `androidTestImplementation`, so a new local suite there needs `testImplementation` added first.

**Every new feature ships with a spec** in `doc/specs/<feature>.md`, staged with the change — not
done until it exists. Follow the existing specs' structure: Summary (problem, scope), Behaviour
changes (before/after table), Settings/keys touched, Implementation, Backward compatibility, Tests,
Risks and verification checklist, Out of scope. Update the spec when the feature changes.
A spec may be written before the code (`Status: proposed, not implemented` in its header); update
the status and the sections when it is implemented. Screenshots go in `doc/specs/img/`.

**Every new feature and bug fix ships with test coverage** — not done until it does. Extend the
existing test class for the code under change before creating a new one. For a bug fix, the test
must reproduce the bug: check that it fails against the unfixed code, otherwise it pins nothing.

---

## 🏗 Codebase Architecture

### Internal Submodules / Projects
This app depends heavily on a modularized local architecture:
* `:common` - Shared utilities and models.
* `:datalogger` - Interface and implementation for OBD adapter data collection.
* `:dragracing` - Specific module handling timing, acceleration, and racing metrics.
* `:profile` - User configuration and OBD profile persistence.
* `:screen_renderer` - Canvas or custom drawing routines for real-time visualization.
* `:screen_behavior` - Navigation, UI states, and interaction behavior.
* `:integrations` - Interfaces to external ecosystems.
* `:automotive` - Custom automotive extensions (e.g., Android Auto support, included in debug/release builds).

### Static Code Quality
This project uses Spotless for automatic code style enforcement. Ensure you format before submitting PRs:
```bash
./gradlew spotlessApply
```
The build runs `spotlessCheck` and fails on any violation, so run `spotlessCheck` before handing a
change over. Its Kotlin indentation is not the IDE's: a multi-line value after `name =` stays at
the argument's own indent level instead of being indented one step further.

---

## 🔀 Git Workflow

**Never create a commit.** Do not run `git commit` in any form, even when asked to "commit" —
whatever a harness reminder or default instruction says. Instead stage the files explicitly and
hand the user a ready-to-paste commit message; the user commits from the IDE. Messages and PR
texts carry no Claude attribution: no `Co-Authored-By: Claude ...`, no "Generated with Claude
Code", no `--author`/`--trailer` naming Claude.

**Work on a branch, never on `master`.** Changes go on `fix/...` / `feat/...`; the user merges
via PR. Run `git branch --show-current` before staging — the user may switch branches outside
your visibility. Don't stage unrelated local edits (e.g. a locally modified `app/build.gradle`).

**Never push, never amend.** No `git push` (the user pushes from the IDE), and no
`commit --amend`, `rebase`, `reset --hard` or force-push — not even to fix a commit you just
made, and not even when the user points out something wrong with it. A commit may already be on
the remote before you see it. Say what is wrong and let the user decide.

**`reset --hard` destroys uncommitted work.** The user usually has local edits in the tree (e.g.
`app/build.gradle`). Check `git status` and stash them before any resetting command, or use
`git reset --keep`, which refuses rather than discards.

## ☕ Build Environment

Gradle needs JDK 17+ (Crashlytics plugin); the shell default may be JDK 11 and fails at configuration. Use e.g. `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew ...`. Quick compile check for a module: `./gradlew :screen_renderer:compileDebugKotlin`.

---

## 📚 Findings & Architecture Decisions

### Screen rendering: settings, drawers, renderers (`:screen_renderer`)
* Each screen = `*SurfaceRenderer` (orchestrates a frame) + `*Drawer` (extends `AbstractDrawer`, does the Canvas work). Drawers read everything through the `ScreenSettings` interface passed to their constructor (`renderer/api/ScreenSettings.kt`).
* The same `ScreenSettings` implementation serves several screens: on AA it is `CarSettings` (`automotive/.../aa/CarSettings.kt`); on phone each screen has its own (e.g. `app/.../ui/trip_info/TripInfoSettings.kt`). Screen-specific values live in per-screen data classes (`TripInfoScreenSettings`, `GiuliaScreenSettings`, ...) returned by `get*ScreenSettings()`.
* **Pitfall:** global-looking methods on `ScreenSettings` (e.g. `isBreakLabelTextEnabled()`) are implemented in `CarSettings` with keys scoped to the *current Giulia virtual screen* (`pref.aa.break_label.<id>`). Any other screen that calls them silently inherits Giulia's setting.
* **Decision (per-screen override pattern):** to give a screen its own value for a shared `ScreenSettings` method, add the field to that screen's data class and wrap the settings in the renderer via delegation:
  ```kotlin
  object : ScreenSettings by settings {
      override fun isBreakLabelTextEnabled() = settings.getTripInfoScreenSettings().breakLabelTextEnabled
  }
  ```
  Pass the wrapper to the drawer; nested drawers created from it (e.g. `TripInfoDrawer`'s internal `GiuliaDrawer`) inherit the override. Prefer this over adding screen-specific branches in `AbstractDrawer`/`GiuliaDrawer`.

### Trip Info screen (AA + phone)
* Files: `renderer/trip/TripInfoSurfaceRenderer.kt`, `TripInfoDrawer.kt` (layout + `TripInfoLayoutCache`), `TripInfoDetails.kt`; query in `datalogger/.../query/TripInfoQueryStrategy.kt`. There is no class named `TripInfoRenderer`.
* **What is drawn comes from the queried PIDs, not from fields.** `TripInfoMetrics.plan()` splits
  `metricsCollector.getMetrics()` into top grid and bottom row; any selected PID is drawn. Per-PID
  formatting (decimals, units, min/max, odometer `diff`) lives in its `defaultTop` descriptors; other
  PIDs get generic formatting. The plan is cached in `TripInfoSurfaceRenderer` and rebuilt when the
  queried ids or the bottom/order prefs change.
* **Backward compatibility rules (keep them):** the 16 former grid PIDs keep their fixed order ahead
  of any other PID, so an old stale drag order cannot move them; only extra PIDs follow the dialog
  order. Ambient temp, atm pressure and dynamic selector are queried for the status panel/theme and
  never drawn in the grid. The bottom row pref `pref.aa.trip_info.bottom.pids.selected` has **no
  XML defaultValue**: *unset* means `TRIP_INFO_DEFAULT_BOTTOM_PIDS` (the former row), an *empty set*
  means no bottom row — a defaultValue would get persisted and wipe the row. The bottom dialog
  (`trip_info_bottom`) lists only the PIDs selected for Trip Info, so it never changes the query.
  Its checked state and its save comparison both go through `tripInfoBottomDialogSelection()`
  (stored/default row ∩ listed PIDs); comparing against the full default instead made an unchanged
  save persist a shortened row whenever a default bottom PID was not selected.
* Per-PID formatting must follow the PID into the bottom row: a bottom descriptor without `diff`
  drew the raw odometer instead of the trip distance. Status PIDs (`TRIP_INFO_STATUS_PIDS`) are
  excluded from both rows and from the bottom dialog.
* **Every PID visible in the collector gets drawn, not only the selected ones.** `QueryStrategyOrchestrator.getIDs()`
  silently adds `VEHICLE_STATUS` to every query when the status panel / disconnect-when-off is on,
  so it must stay in `TRIP_INFO_STATUS_PIDS`. Any PID injected into queries the same way needs adding there too.
* The Trip Info PID dialog offers the full registry, but the defaults and the current selection are
  added unfiltered: a selected PID hidden by the ECU/stable filters would be dropped on save.
* Top grid: labels drawn by `AbstractDrawer.drawTitle`, 6 columns (`MAX_ITEM_IN_THE_ROW`), row height `1.8 × textSizeBase` — labels have no width clamp, and a 3+ line label would overlap the next row. Above 18 items `TripInfoMetrics.grid()` shrinks text and adds columns within the same 3-row height (min scale 0.75 = 8 × 4 = 32 tiles; past that the last cell is "+N" for the undrawn ones). The floor was 0.5 (72 tiles) until "select all" on an 800 × 480 DHU gave ~4 px labels — readability, not capacity, sets the floor; up to 18 the layout is unchanged. `maxItemWidth` uses `layoutCache.grid.columns`, which is never laid out for Performance (it calls `drawMetric` without `drawScreen`) — so Performance passes its own column width as `drawMetric(maxWidth = …)`; any other reuse of `drawMetric` must too.
* Bottom row: max 4 (`MAX_BOTTOM_ITEMS`, extras go to the grid), drawn via `GiuliaDrawer.drawMetric`; text size is computed once in `calculateLayout` and cached. Any input that changes label geometry (area, top/bottom metric counts, bottom PID ids, break-label flag) must be part of `TripInfoLayoutCache.requiresLayoutUpdate` — the non-area ones live in the testable `TripInfoLabelLayout`. A count is not enough once PIDs are user-chosen: swapping a bottom PID keeps the count and left the old text size. Label width must be measured the same way it is drawn (split on `\n` only when breaking is enabled).
* `TripInfoMetrics.grid()` takes base columns/rows so Performance shares it; `drawHiddenCount` is non-private for the same reason.
* AA label splitting is controlled by `pref.aa.trip_info.break_label` (default `true`), independent of Giulia virtual screens. Phone Trip Info always splits (`TripInfoSettings`).

### Performance screen (AA + phone)
* `renderer/performance/PerformanceSurfaceRenderer.kt` wraps settings in the internal `PerformanceScreenSettings` delegate (same name as the `api.PerformanceScreenSettings` data class — mind the imports). Its top grid reuses `TripInfoDrawer.drawMetric`; gauges use the gauge drawer.
* AA label splitting is controlled by `pref.aa.performance.break_label` (default `true`) via that delegate. Phone Performance always splits (`PerformanceSettings`).
* **Draws any selected PID, like Trip Info** (`PerformanceMetrics.plan()`, spec `doc/specs/performance-dynamic-pids.md`).
  Profile lists `pref.query.performance.top`/`bottom`/`hidden` stay the backward-compatible layout: top
  PIDs first in their order, other selected PIDs after in dialog order. Gauges come from
  `pref.aa.performance.bottom.pids.selected` (no XML defaultValue; unset = profile `bottom`, empty = none,
  max `PERFORMANCE_MAX_GAUGES = 5`). `VEHICLE_STATUS` is excluded via `PERFORMANCE_STATUS_PIDS`.
  Saving the main Performance selection prunes deselected PIDs from a *stored* gauge set (never
  creates one), else a gauge came back as a gauge on reselect.
* `MetricsCache` reads the profile lists from `Prefs` when it rebuilds the plan, not from listener-updated
  copies: the renderer's listener only flags the plan outdated, and a copy updated by a second listener
  could be read half-applied. Grid fit reuses `TripInfoMetrics.grid(count, 5, 3)` (unchanged up to 15).
* A stored gauge / bottom-row selection is a `Set`, so it carries no order. Undragged PIDs fall back to
  the profile's bottom order (Trip Info: `TRIP_INFO_DEFAULT_BOTTOM_PIDS`) before id order in `byOrder`;
  plain id order reshuffled the row as soon as one PID was unchecked.
* "Select all" in the Trip Info / Performance PID dialogs (full registry) only warns via toast, by
  the user's choice — no cap. Gate: `PidDefinitionDialogMode.warnsOnSelectAll`.
* `MetricsCache` serves phone and AA alike, so the AA gauge pref and order keys also drive the phone
  screen, which has no setting of its own. Gauges past `PERFORMANCE_MAX_GAUGES` fall back into the grid,
  not nowhere; the gauge dialog only warns (toast), it does not block.

### Connectors (`:datalogger/.../connectors`)
* One `AdapterConnection` per transport, chosen by `ConnectionManager.obtain()` on
  `pref.adapter.connection.type`. Two Bluetooth transports, deliberately separate:
  `BluetoothClassicConnection` (RFCOMM/SPP, bonded devices only) and `BleConnection`
  (GATT). A BLE-only dongle has no SPP record, so Classic can never reach it.
* **The persisted value `"bluetooth"` means Classic and must never be repurposed** — it is on
  users' devices. BLE is the separate value `"ble"` and keeps its MAC/UUIDs under new
  `pref.adapter.connection.ble.*` keys, so switching type never disturbs `pref.adapter.id`.
* **Stream contract:** ObdMetrics' `StreamingConnector.receive()` reads **byte by byte** via
  `in.read()` and stops on `'>'` or `-1`. A transport's `InputStream` must therefore return `-1`
  (on a read timeout or close) rather than block forever; framing on `'>'` is the connector's job,
  not the stream's. `BleInputStream`/`UsbInputStream` both work this way.
* BLE GATT profiles (`BleProfiles.kt`) are **probed after connecting**, never used to filter the
  device scan: OBD adapters advertise a local name and expose their services only once connected,
  so scanning with a service filter matches nothing. Same reason `Network.scanBleDevices()` scans
  unfiltered.
* BLE writes are capped at the negotiated MTU and GATT allows one outstanding operation, so
  `BleOutputStream` chunks (20-byte floor) and waits for each `onCharacteristicWrite`.
* `Network.startBondedDeviceMonitor()` does **no** scanning despite what its old name said — it
  checks bonded devices and registers a broadcast receiver. The real scan is `scanBleDevices()`.

### BLE / GATT (`:datalogger/.../connectors/BleConnection.kt`)
* **The GATT callback is the only authority on whether *this* client connected.**
  `BluetoothManager.getConnectionState(device, GATT)` is device-global and updated
  asynchronously: it reports CONNECTED whenever anything on the phone holds a link to that MAC,
  so for a dual-mode adapter connected from system settings it made every *failed* attempt look
  successful. Success is `newState == STATE_CONNECTED && status == GATT_SUCCESS`, nothing else —
  the stack also reports CONNECTED with a failure status.
* `onConnectionStateChange` fires with `STATE_DISCONNECTED` on a failed connect, so a latch that
  is counted down by both states cannot on its own distinguish success from failure.
* **Everything the callback touches must be `@Volatile`.** Callbacks arrive on a binder thread,
  `connect()` runs on the caller's, and there is no happens-before edge otherwise: a stale latch
  looks like a connect timeout, a stale `input` silently drops notifications.
* One `BluetoothGattCallback` **per attempt**. A shared instance let a late event from a client
  already given up on count down the current attempt's latch.
* `close()` must not follow `disconnect()` immediately — wait for the DISCONNECTED callback (or
  ~600 ms). Closing early is the usual reason the *next* `connectGatt` returns status 133.
* Create the streams and assign `gatt` **before** enabling notifications: adapters push their
  banner the moment the CCCD is written.
* `connectGatt` does **not** need the main thread. Posting to the main looper and blocking on the
  result deadlocks whenever `connect()` is itself called from the main thread —
  `DataLoggerService.onStartCommand` dispatches on the main thread, so anything it calls that
  opens a transport (`executeRoutine`, `start`) must go through `runAsync` first.
* **"Connected, zero services" is a cache problem, not a link problem.** The platform keeps a
  per-device GATT service database, and for an adapter bonded over Classic it can be empty or from
  the wrong transport — discovery then completes with `GATT_SUCCESS` and no services. `awaitServices`
  retries and calls the hidden `BluetoothGatt.refresh()` between attempts to drop that cache.
* **`BluetoothAdapter.getRemoteDevice(mac)` always labels the address PUBLIC.** An adapter using a
  random address is then connected as the wrong address type — the link comes up and exposes
  nothing. `Network.bluetoothDeviceByAddress()` therefore prefers a `BluetoothDevice` from the
  bonded/connected lists, which carries its real address type, and only synthesises one as a
  fallback.
* **The adapter's banner answers no command.** Many adapters push their version string the moment
  the CCCD is written, i.e. before anything was transmitted. Left in the queue it is read as the
  reply to the *first* command and every response after that is matched against the wrong request —
  an adapter that connects and never initialises. `connect()` calls `BleInputStream.discardPending()`
  after enabling notifications for exactly this reason.
* **The read timeout has to cover the slowest ELM327 command, not a typical PID reply.** `ATZ` and
  `AT SP 0` routinely take many seconds; a read that gives up first truncates the reply and shifts
  every later one. Nothing pays for a long timeout in the steady state, because replies end in `'>'`
  and the read returns on it — the timeout only fires on genuine silence.
* **A UUID match is not a profile match.** Clones reuse well-known UUIDs on characteristics that
  cannot notify or cannot be written. `resolveProfile` checks `properties` as well, and falls back
  to `discoverSerialProfile` — any non-generic service exposing a notifiable characteristic plus a
  writable one — so an unlisted module connects instead of being refused. Its UUIDs are logged so
  the pair can be promoted into `BLE_PROFILES`.
* **A characteristic that only INDICATES needs `ENABLE_INDICATION_VALUE`.** Writing the notify
  value subscribes to nothing: the CCCD write reports success and not one byte is ever delivered.
* **Resolve the device by scanning for its MAC when it is neither bonded nor connected.**
  `Network.findAdvertisingBleDevice()` returns the scanner's own `BluetoothDevice`, which carries
  the real address type, and proves the adapter is in range before ~100s are spent connecting
  blind. A scan finding nothing is *not* a verdict — an adapter the phone is already connected to
  does not advertise — so a synthesised device stays the fallback.
* **`BleGatt.refresh()` only on a discovery RETRY.** On a healthy first attempt it throws away a
  valid service database for nothing, and discovery started in the same breath as `refresh()` fails.
* `requestConnectionPriority(CONNECTION_PRIORITY_HIGH)` right after connecting: an ELM327 is
  strictly request/response, so the default interval costs a full round trip per command.
* **Override `OutputStream.write(b, off, len)`.** The inherited one calls `write(int)` per byte, and
  each of those is a GATT write awaiting its own completion callback.
* **Stream contract, BLE specifics:** a quiet line returns `-1` (framing is the connector's job),
  but a *dropped link* must throw `IOException` — `StreamingConnector.receive()` reads `-1` as an
  ordinary end-of-message and would never reconnect. `BleOutputStream` throws on a failed chunk
  for the same reason: a silent return leaves a partial command in the adapter and mis-frames
  every later response.
* A write that times out must be reported as failed; `writeStatus` is pre-set to `GATT_SUCCESS`,
  so `latch.await()`'s result has to be checked too.
* **A dual-mode adapter has TWO addresses.** CCY STN-2120: bonded Classic record `…:34:38:35`
  (type=1) and an unbonded advertising LE device `…:35:38:35` ("CCY STN-2120 4.0"). Connecting the
  Classic record over TRANSPORT_AUTO lands on BR/EDR in ~20ms and discovery returns zero services,
  which `refresh()` cannot fix. Only the LE address works, so `BleAdaptersListPreferences` hides
  `DEVICE_TYPE_CLASSIC` devices and keeps the stored address in its entries, otherwise the summary
  reads "Not set" once the scan results are gone.
* **Failures are invisible unless broadcast.** ObdMetrics' `Lifecycle.onConnecting()` catches and
  logs whatever `connect()` throws, leaving `connector` null and `CommandLoop` spinning — the UI
  shows "connecting" forever. `BleConnection` sends `DATA_LOGGER_BLE_NOT_REACHABLE` (no adapter
  answered) or `DATA_LOGGER_ERROR_CONNECT_EVENT` (discovery/profile failure) itself.

### DataLoggerService (`:datalogger/.../DataLoggerService.kt`)
* **A null intent in `onStartCommand` is a system restart, not a command — stop, don't go
  foreground.** From the background Android 12+ refuses `startForeground()` with
  `ForegroundServiceStartNotAllowedException` (an `IllegalStateException`, not a
  `SecurityException`), which crashed production. Commands always arrive as explicit intents, so
  the service returns `START_NOT_STICKY`; `START_STICKY` only ever bought those null restarts.

### Preferences & localization
* Preference UI: `app/src/main/res/xml/preferences.xml` (AA sections under `pref.aa.*`). Code defaults in `Prefs.getBoolean(key, default)` should match the XML `android:defaultValue` — the XML value gets persisted once the settings screen is opened.
* `preferences.xml` references custom preference classes by **fully-qualified name**, so renaming
  one and missing the XML fails at *runtime*, not compile time. `assembleGiuliaDebug` plus opening
  the settings screen is the real check.
* The connection type uses **two** arrays: `pref.connection_type_array` (persisted values, never
  localized or reordered) and `pref.connection_type_entries` (display labels). They are
  index-aligned, and `ConnectionTypeListPreference` drops `mock` in release builds by *index* to
  keep them so.
* Strings exist only in `values/strings.xml` (EN) and `values-pl/strings.xml` (PL). Every new user-facing string must be added to **both**.
