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

**Never reference private files in a repo.** The ECU UDS spreadsheets and other private documents
stay out of code, comments, specs and CLAUDE.md: no file names, paths or sheet names. Say "the ECM"
instead. The repos are public.

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
* Top-grid min / max are drawn only if `TripInfoMetrics.statsFit` says they fit the column. #222 added
  a `maxWidth` (column) parameter to `drawValue`, which already had a local `maxWidth` (max text width):
  the local shadowed it and no top-grid tile (Trip Info and Performance) showed min / max. Kotlin only
  warns on shadowing — read the compiler warnings when renaming parameters.
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
  the profile's bottom order (Trip Info: `TRIP_INFO_DEFAULT_BOTTOM_PIDS`) before id order in
  `bl/query/PidOrder.kt` (`:datalogger`, shared by both screens and their dialogs — keep one copy);
  plain id order reshuffled the row as soon as one PID was unchecked.
* **Opening a PID dialog stores a drag order.** `PidDefinitionViewModel.sortItems()` saves the checked
  rows' listed order whenever none is stored, and a stored order beats `PidOrder`'s default. So the
  bottom/gauge dialogs must *list* in the default order (`tripInfoBottomDialogItems`,
  `performanceBottomDialogItems`); listed in a `Set`'s order, merely opening the dialog fixed a random
  order. Orders stored before that fix stay as stored — they cannot be told apart from a real drag.
* "Select all" in the Trip Info / Performance PID dialogs (full registry) only warns via toast, by
  the user's choice — no cap. Gate: `PidDefinitionDialogMode.warnsOnSelectAll`.
* **Gauges keep their size; the top grid gives way** (user's choice). `PerformanceDrawer` sizes the
  gauges first (`PerformanceMetrics.gaugeRow`, width / count; `GaugeGeometry.dialBottomRatio` = how far
  below its top the arc reaches), then `gridRows()` turns the height left into a fractional row budget
  (1..3) for `TripInfoMetrics.grid(count, 5, rows)`, which shrinks text / adds columns to fit. On an
  800 × 480 DHU three grid rows plus full-size gauges do not fit. Wrong turn: capping the gauges by
  the height left under the grid gave ~90 px unreadable dials. That cap now only bites below a 1-row grid.
  Performance's grid font floor is 0.6 (`PERFORMANCE_MIN_GRID_SCALE`, passed as `grid(minScale)`), not
  Trip Info's 0.75: at 0.75 the ~1.4 rows left on a DHU held one row of six ("+7" for 12 PIDs).
* `MetricsCache` serves phone and AA alike, so the AA gauge pref and order keys also drive the phone
  screen, which has no setting of its own. Gauges past `PERFORMANCE_MAX_GAUGES` fall back into the grid,
  not nowhere; the gauge dialog only warns (toast), it does not block.

### Gauge drawer (`renderer/gauge/`)
* `GaugeDrawer` is shared by the Gauge, Performance, Drag Racing and Brake Boosting screens (phone + AA):
  any dial change shows on all four. Spec: `doc/specs/gauge-drawing-improvements.md`.
* **The dial maps values through `GaugeScale`, not `pid.min`/`pid.max`.** It extends the PID range to
  "nice" steps; progress, labels and ticks must all use `scale.fraction()` or they drift apart.
  Pure geometry lives in `GaugeGeometry.kt` (unit-tested); keep Canvas out of it.
* The scale bitmap is cached, so anything that decides *what* goes into it (numbers on/off, scale,
  zones) must be in its key. Numbers skipped for a still-`null` value were cached numberless for good.
* End labels of a dial that ends below centre sit beside the stats row; `GaugeGeometry.statsMaxWidth`
  keeps the row clear. Fix overlaps with draw order (numbers after ticks) before moving labels.
* Phone dials are centred on what they draw (`GaugeGeometry.dialTopOffset` from start/sweep), not on
  their square — a 200° arc leaves the lower circle empty.
* Units: draw `displayUnits(pid.units)` (`renderer/Units.kt`), never `pid.units` directly. ObdMetrics'
  PID resources spell °C as a bare `C`, and they are not ours to change (shared with exports/logs).
* The red end is **styling, identical on every dial** (`GaugeRedZone`, last quarter), never derived
  from alert thresholds. Two wrong turns, both rejected by the user: threshold-only zones (#225) left
  PIDs without alerts with no red; threshold-started zones made the red differ dial to dial. Alerts
  show only through the value / bar colour. The scale bitmap cache is keyed by `GaugeScale` — a cache
  keyed by PID id alone kept a stale scale after a PID edit.
* Phone cards may be taller than the dial (`GaugeGeometry.cardHeight`); `borderArea` is the card, `top`
  the dial. Captions belong to the card. `contentHeight`'s bottom pad must be excluded from the height
  the cards share, or a filled grid scrolls by a few pixels.
* The bar is drawn with `progressPaint` / `glowPaint`, never `paint`: a shader on `paint` (the old
  progress gradient) is invisible. Spec: `doc/specs/gauge-view-improvements.md`.
* Per-PID cross-frame state (eased needle, staleness, last draw) lives in `GaugeFrameStates`; every
  per-PID cache in `GaugeDrawer` must be dropped in `evictUndrawn`, or it leaks for PIDs no longer shown.
  A new reading = a new `metric.source` object (the collector replaces it), not a changed value.
* Centre text on `measureText` (advance), not `getTextBounds` (ink): ink width differs per digit, so the
  value jittered sideways every reading.
* Grid layout is `GaugeGrid` (pure, pinned by `GaugeGridTest`); change AA tuning there, with a DHU check.

### DTC (phone dialog + AA screen)
* List logic (grouping by module, sort, code/description formatting, scan module selection) lives in
  `datalogger/.../bl/datalogger/dtc/DtcListItem.kt`, shared by `DiagnosticTroubleCodePreferenceDialogFragment`
  and AA `DiagnosticTroubleCodesScreen` — keep one copy. Spec: `doc/specs/aa-dtc-screen.md`.
* `DtcListItem` declares `abstract equals/hashCode`: lint's `DiffUtilEquals` (an *error*, fails CI
  `lintGiuliaDebug`) cannot see data-class subclasses in another module. Run `:app:lintGiuliaDebug`
  after moving a type that an `:app` adapter compares.
* Row identity is `DtcListItem.itemKey()` (module + display code, no raw hex): used for DiffUtil's
  `areItemsTheSame` and the phone's expanded row. Code + raw hex merged the same code from two modules,
  and made a status-byte change look like a new row. Never key list state by position — a read or
  clear reorders the list and another row inherited the expanded state.
* Phone dialog buttons: check the PL strings at phone width. Three equal-weight buttons in
  `dialog_dtc_module_picker.xml` broke "Odznacz wszystkie" mid-word, so select/deselect-all now sits
  above the list and the bar holds only Cancel + confirm.
* Module order: modules with codes first, then clean ones, each by name (`toDtcListItems`, shared). Headers carry the
  code count (`ModuleHeader.codeCount`, not part of `itemKey()`). AA templates allow colour spans only in row text
  lines: section headers and row titles are text-only and throw on a colour span, so AA marks codes with a tinted
  `CarIcon`. Phone colours come from night-aware resources; hard-coded `Color.DKGRAY` was unreadable on the dark
  dialog. Spec: `doc/specs/dtc-list-colors-order.md`.
* Code state (Active / Stored / Pending / History + MIL) is decoded from `statusMask` in `dtc/DtcStatus.kt`,
  shared by phone, AA, sort and share report. `statusMask` is always the UDS status byte: ObdMetrics builds every
  code in `UdsMultiFrameDtcParser`. Its `activeStatuses` are translated strings — never parse them, use the mask.
  Spec: `doc/specs/dtc-status-badges.md`. Phone shows it as chips under the code, each with its own icon + colour
  (`STATE_BADGES`) and a text explanation when expanded — the user rejected a coloured text line reusing the codes' red.
  `item_dtc.xml` is a plain top-down stack: description → chips → expanded details → divider (the code sits beside
  the description and is never taller). Wrong turns: chips under the code alone were wider than its column and
  overlapped the description; barriers (`bottom` of code + description, and of all columns for the divider) resolved
  wrongly once a row was expanded, pushing the chips ~850 px down. Space below a view is padding, not margin, if
  anything ever constrains to a barrier again (barriers ignore margins). Check bounds with
  `adb shell uiautomator dump`, not by eye.
* "Timeout waiting for response" on a DTC read/clear is the phone overlay (`ScreenLockManager`), not the car:
  `ScreenLock.timeoutMs` defaults to 10 s, a 13-module scan takes ~13 s. DTC actions pass `dtcActionTimeoutMs(modules)`;
  any other long action behind the overlay needs its own timeout too. The logcat shows the scan completing regardless.
* AA scans the modules the phone's picker left checked (`PREF_DTC_DESELECTED_MODULES`); AA has no picker.
* **The section a code lands in is `DiagnosticTroubleCode.module`, set inside ObdMetrics, not here.**
  An "ECU" (now "Unknown module") section full of codes next to scanned modules that report nothing
  means the module tag was lost upstream: e.g. two DTC read PIDs sharing a query made the decoder use
  the registry copies (`module=ecu`). Check the `[DTC] Adding DTC read command` lines in logcat
  before touching the grouping. Fix lives in ObdMetrics `fix/dtc-module-tagging`.
* CAN header table entries can share a header; `dtcScanTargets` scans each header once (first selected
  entry names it). Without it the same ECU's codes appear under every name pointing at it.
* **ObdMetrics reported `NO_DATA` for every user-triggered clear** (only the init clear had a real
  status, in `VehicleCapabilities`). Fixed upstream on `feat/dtc-clear-status`, but the app judges a
  clear mainly by the read that follows it (`dtcClearOutcome`): only "refused" needs the real status.
  The outcome rides on `DATA_LOGGER_DTC_ACTION_COMPLETED` as a `DtcClearOutcome` extra; reads carry none.
  Spec: `doc/specs/dtc-read-info-clear-outcome.md`.
* Stored codes are stamped (`pref.datalogger.dtc.read_info`: time + VIN) in `updateDTC`. Vehicle
  metadata is keyed by the metadata PID's *label*, which varies by profile **and is translated**
  (Giulia 22F190: "Vehicle Identification Number" / PL "Numer identyfikacyjny pojazdu"). So `vinFrom`
  falls back to recognising the value. Wrong turn: matching keys on "vin" missed the Giulia VIN.
  No VIN at all usually means `pref.adapter.init.fetchDeviceProperties` is off (metadata `{}`).
  Check on device: `adb shell "run-as <pkg> cat shared_prefs/<pkg>_preferences.xml"` (debug builds only).
* **Automatic DTC read on connect = a module scan when modules are configured.** ObdMetrics' own
  init read (`vehicleDtcReadingEnabled`) covers the default header only. Its untagged result replaced the
  grouped list on every connect, and its codes leaked into the next scan as "Unknown module"
  (`DiagnosticTroubleCodeHandler` accumulated *all* DTC replies, including init's; fixed on ObdMetrics
  `feat/dtc-clear-status` with a start marker that resets it).
  So `AdjustmentsStrategy` turns it off and `WorkflowOrchestrator.scheduleConnectScan` scans all modules.
  ObdMetrics' `isRunning` is false inside `onRunning` (threads start after it), so anything scheduled
  "on connect" must wait for `workflow.isRunning` off the callback thread. Untagged codes after a
  reconnect: check the logcat for `InitCompletedHandler: Found Diagnostic Trouble Codes` first.
* **2.0 GME DTC knowledge lives in ObdMetricsGiuliaQV, by the user's choice;** ObdMetrics core only offers generic
  hooks. A PID's decoder is its `codecClass` (loaded by name), so the GME clear is a GiuliaQV codec + `dtcClear` in
  `2.0_gme_ext.json`. Per-code requests (UDS `19 06` extended data) are `dtcFollowUp` templates (`{dtc}` placeholder);
  their values arrive in `DiagnosticTroubleCode.details`. Spec: `doc/specs/dtc-extended-data.md`.
* One clear PID per query across a profile's files: ObdMetrics' registry keeps every PID registered for a query and
  the decoder runs each, so two clear codecs race (file order decides). Hence no `dtcClear` in `giulia_2.0_gme.json`.
* Snapshot/follow-up requests must go to the code's own module (`ATSH`); before ObdMetrics `feat/dtc-follow-up-requests`
  they went to the last scanned module and timed out (handler logs "Timed out waiting", a 5 s gap before "Notyfing").
  The snapshot parser stops at the first DID it cannot resolve. A short freeze frame means a missing DID, or one whose
  `length` differs from the frame: 2.0 GME stores 1812 / 1004 in 1 byte, their live replies 2. Snapshot sizes go in
  the `dtcSnapshot` group (resolved before livedata, never queried live). Wrong turn: a 33-DID layout from the spec
  document; the car sends 11. Trust a captured frame (logcat `TX: 19 04`), not the document.
* A context reply that decodes to `null` is never published, so the DTC handler waits for it until its timeout (23 s
  for 30 codes; the scan looks hung). Snapshot and follow-up codecs return an error response instead.
* String resources drop a leading space: join text parts in code (`DtcReadInfo.summary`).
* Building ObdMetrics: `mvn` needs JDK 11 (`JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64`); the
  delombok plugin fails on JDK 21. Its test helpers (`SimpleLifecycle`, ...) live in the separate
  `ObdMetricsTest` repo and do not capture the clear status, so a test needs its own `Lifecycle`.
* AA `ListTemplate` action strips take **2** actions (`ACTIONS_CONSTRAINTS_SIMPLE`),
  not 4 like the nav template — a third throws at build time.
* AA template screens: a `CarScreen` added as observer of the *parent's* lifecycle (as `gotoScreen` does
  for `RoutinesScreen`) gets the parent's pause when it is pushed, so its receiver should unregister while
  it is on screen (read from the code, not yet confirmed on a DHU). `DiagnosticTroubleCodesScreen` observes its own lifecycle instead.

### Readiness monitors (phone)
* Spec `doc/specs/readiness-monitors.md`. ObdMetrics decodes 01 01 / 01 41 (`ReadinessMonitorsCodec`, ids 21010 /
  21011, `"category": "readiness"`) into a `ReadinessStatus` object, not a number. Hence `isReadinessPid()` keeps
  them out of the PID pickers: on a gauge they would only show NaN. Display logic: `bl/readiness/Readiness.kt`.
* Reached from the menu (`ctx_menu_readiness` → sub-screen `pref.readiness`, which opens the dialog). The menu's
  DTC item opens only the `pref.dtc` sub-screen, so an entry placed in the root settings alone is never seen —
  any new top-level feature needs a menu item, not just a preferences.xml category.
* Link with DTC: the readiness dialog stores the last since-clear result (`pref.readiness.last`, `ReadinessSnapshot`);
  the DTC dialog shows it as a row opening the readiness dialog. An accepted clear drops it (`resetsReadiness`).
  A dialog opened from a dialog goes on its `childFragmentManager`; refresh in `onFragmentViewDestroyed`, since the
  parent dialog gets no onResume when it closes.
* ObdMetrics drops query ids missing from the registry without an error. A screen with a fixed query must check that
  its PIDs' resource module is selected (readiness: `hasReadinessPids()`, Mode01) or it waits forever.
* The dialog switches the running query to `READINESS_QUERY` and restores `DataLoggerRepository.runningQuery()`
  on close. Any other screen that polls its own PIDs from a dialog must restore the same way.
* **"Nothing decoded any more" = check logcat for `ConnectorResponseDecoder ... Unexpected error` / `Completed decoder
  thread`.** Every `updateQuery` queues UDS `10 01` (no PID); since the module-tagging change its reply threw an NPE
  that ended ObdMetrics' decoder thread, so after any query switch no reply (live data, readiness, DTC) was decoded
  until the next connect, while TX/RX still looked healthy. Fixed in ObdMetrics `ConnectorResponseDecoder` (skips
  PID-less replies, and one failing reply no longer ends the thread).
* **`DataLoggerRepository.observe()` conflates** (LiveData `postValue`): metrics decoded from one batch reply arrive
  back to back and only the last reaches the observer. Fine for gauges reading the collector; a screen that needs
  each PID's reply uses `observeEach()`. The readiness dialog sat at "waiting" until it switched.
* ObdMetrics' batch decoder used to map a PID the ECU did not answer onto the reply's answer code (PID 41 in
  `4101...`); fixed in `CachedBatchMessageDecoder` on `feat/readiness-monitors`. The 2.0 GME answers only 01 01 / 01 21
  of the readiness query. To check a batch on the bench, decode the logged TX/RX with `BatchCodec` in a test.
* Adding a mode 01 PID with pid "01" broke ObdMetrics' batch tests: `BatchCodecTestRunner` turned every query token,
  the mode included, into a PID. It now skips the first token. Production builds commands from ids and was fine.

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
