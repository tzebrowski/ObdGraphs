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

> **Note on Unit Tests:** The current `build.gradle` configuration strictly defines `androidTestImplementation` dependencies. If local JVM unit tests are required, standard `testImplementation` dependencies (like JUnit 4/5) must be added first.

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

---

## 🔀 Git Workflow

**Never commit directly to `master`.** **Do not add Claude attribution (`Co-Authored-By: Claude ...`, "Generated with Claude Code") to commits or PRs.** Always branch (`fix/...`, `feat/...`); the user merges via PR. Run `git branch --show-current` before committing — the user may switch branches outside your visibility. Stage files explicitly; don't sweep in unrelated local edits (e.g. a locally modified `app/build.gradle`).

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
* Top grid: labels drawn by `AbstractDrawer.drawTitle`, fixed 6 columns (`MAX_ITEM_IN_THE_ROW`), fixed row height `1.8 × textSizeBase` — labels have no width clamp, and a 3+ line label would overlap the next row.
* Bottom row: drawn via `GiuliaDrawer.drawMetric`; text size is computed once in `calculateLayout` and cached. Any input that changes label geometry (area, visible metric count, break-label flag) must be part of `TripInfoLayoutCache.requiresLayoutUpdate`, and label width must be measured the same way it is drawn (split on `\n` only when breaking is enabled).
* AA label splitting is controlled by `pref.aa.trip_info.break_label` (default `true`), independent of Giulia virtual screens. Phone Trip Info always splits (`TripInfoSettings`).

### Performance screen (AA + phone)
* `renderer/performance/PerformanceSurfaceRenderer.kt` wraps settings in the internal `PerformanceScreenSettings` delegate (same name as the `api.PerformanceScreenSettings` data class — mind the imports). Its top grid reuses `TripInfoDrawer.drawMetric`; gauges use the gauge drawer.
* AA label splitting is controlled by `pref.aa.performance.break_label` (default `true`) via that delegate. Phone Performance always splits (`PerformanceSettings`).

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
