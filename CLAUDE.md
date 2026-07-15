# CLAUDE.md (org.obd.graphs)

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