# MyWatchList KMM Project Skill File & Guidelines

This document contains core instructions, architectural decisions, and layout rules for the MyWatchList Compose Multiplatform project. Read and follow these instructions in all subsequent agent sessions.

---

## 0. Basic Project Information
* **Project**: MyWatchList — a Kotlin Multiplatform (KMM) / Compose Multiplatform app for browsing trending/movies/TV shows/people via the TMDB API.
* **Root project name**: `MyWatchList` (see `settings.gradle.kts`), single Gradle module: `:composeApp`.
* **Package / namespace**: `com.ajinkyabadve.kmmmywatchlist` (Android applicationId: `com.ajinkyabadve.kmmmywatchlist.androidApp`).
* **Targets**: Android (compileSdk 35, minSdk 24, targetSdk 34), Desktop (JVM, `MainKt`), iOS (iosX64, iosArm64, iosSimulatorArm64 → static `ComposeApp` framework), and JS/Web via Kotlin/JS + Compose HTML (currently parked, see build notes above).
* **Build system**: Gradle Kotlin DSL, Kotlin Multiplatform plugin + Compose Multiplatform plugin, JDK 17 everywhere. Key plugins: `android-application`, `buildConfig` (injects `TMDB_API_KEY` from `MY_WATCH_LIST_TMDB_API_KEY` gradle property), `kotlinx-serialization`, `sqlDelight`, `compose-compiler`.
* **Core libraries**: Ktor (networking, with per-platform engines: OkHttp on Android/Desktop, Darwin on iOS), Koin (DI), Coil (image loading), Napier (logging), kotlinx-coroutines/-serialization/-datetime, Compose Navigation, Material3 adaptive navigation suite, SqlDelight (DB, drivers per platform), multiplatform-settings, Feather compose-icons.
* **Source set layout**: `composeApp/src/{commonMain,androidMain,desktopMain,iosMain,jsMain,commonTest,...}`. Shared UI/logic lives in `commonMain`.
* **Package structure** (under `commonMain/kotlin/com/ajinkyabadve/kmmmywatchlist/`):
  - `core/` — shared constants, models, paging source.
  - `design/` — shared Compose UI components (movie cards, search box, segments, util).
  - `features/` — feature modules: `favorite`, `movies`, `person`, `trending`, `tvshows`, each with `model` / `network` / `repository` / `screen` sub-packages.
  - `homepage/` — home screen tabs.
  - `navigation/` — app navigation (Compose Navigation, no Voyager — see section 5).
  - `network/` — Ktor client builder/config, constants, exceptions.
  - `theme/`, `util/` — theming and utilities.
* **Docs**: `README.md` (setup, run instructions per target, screenshots), `future_features_checklist.md` (TMDB-spec-driven roadmap: search, favorites/watchlist, detail screens, discovery, TMDB auth).
* **Other skill files**: `.agents/skills/tmdb-guidelines/SKILL.md` — TMDB API/OpenAPI usage and image-resolution rules (see that file when touching TMDB network/image code).

---

## 1. Environment & Build Configuration
* **Java Version**: Always use **Java 17** for compilation. Ensure compatible JDK setup across Android, Desktop, and iOS.
* **Web Target**: The JS/Web target is currently parked due to signature mismatch issues. Do not spend time trying to compile it unless explicitly requested by the user.
* **Android Target**: Always use the **android-cli** skill and the `android` command-line tool by Google for Android builds and environment diagnostics.
  - Skill installed at `.agents/skills/android-cli/SKILL.md` (Google's official Android CLI skill, `android skills add android-cli --project <root>`). Binary lives at `/opt/homebrew/bin/android` (installed via `brew install android-cli`, tap `android/tap`) — `~/.zshrc` was updated to put `/opt/homebrew/bin` ahead of the legacy deprecated `~/Library/Android/sdk/tools` on `PATH` so `android` resolves to this CLI, not the old SDK tool.
  - Useful commands: `android run` (deploy/launch on device/emulator), `android screen capture` (screenshot), `android layout` (UI layout tree, faster than screenshots for debugging), `android emulator start|stop|list`, `android docs search` (Android Knowledge Base lookups), `android sdk install/list`.
* **Android Verification**: Whenever modifying Android UI layouts, gestures, or interactive features, do not rely only on compilation. Always use the `android-cli` tools to launch the app on a running emulator/device and verify the actual runtime behavior.

---

## 2. Code Commits & Staging
* **CRITICAL**: Do **NOT** commit, stage, or check in any modified files unless the user explicitly instructs you to do so. All code changes should remain local. E.g., even if tests run successfully, never commit code without asking for explicit permission first.
* **Test Before Committing**: Do **NOT** commit, stage, or check in the code before running the unit/integration tests successfully only for diff of the files and if the unit test file are availble to run to verify there are no failures.

---

## 3. Navigation & Screen State Preservation
* **Stable Layout Root**: To prevent scroll state loss and resetting of loaded pages/items to page 1 when the desktop window is resized:
  - Do **NOT** use a dynamic `when (layoutType)` branching structure that swaps the root layout containers.
  - Keep a single stable `Row` container wrapping the side panel next to the main `Scaffold` and `NavHost`.
  - Conditional layouts (Drawer vs. Rail) should be placed as conditional blocks within this stable parent tree to maintain nested state paths.
* **ViewModel Persistence**: To preserve loaded movies and scroll positions when switching sub-tabs (e.g. Now Playing <-> Upcoming) or when navigating away from the Movies tab (e.g. Movies <-> Trending) and back:
  - Instantiate the `MovieListScreenModel` ViewModels at the root level of `MainAppScreen` using `remember`.
  - Pass the persisted instances down to `MovieScreenTabs` and `MovieListTab`.
  - This ensures they survive screen navigation and tab switches without being disposed/cancelled.
* **Scroll Position Persistence**: To preserve the scroll positions of the sub-tabs:
  - Instantiate the `LazyGridState` instances for each of the four sub-tabs inside `MovieScreenTabs` using `rememberLazyGridState()`.
  - Pass the states down to their respective tabs.
  - Since `MovieScreenTabs` remains in composition during sub-tab switches, and the NavController's `SaveableStateHolder` saves the `rememberSaveable` state of the active screen when navigating away, the scroll positions of all sub-tabs are fully preserved.

---

## 4. Main Navigation Centering & Spacing Rules
* **Vertical Centering**:
  - The main navigation items (Trending, Movies, Tv shows, Person, My Fav) in the left panel (both the Navigation Drawer and Navigation Rail) must be vertically centered in the viewport.
* **Grouping & Gaps**:
  - Do **NOT** use `Spacer(Modifier.weight(1f))` between individual items, as it creates excessively large, scale-dependent gaps on expanded viewports.
  - Group the items inside a centered Column:
    ```kotlin
    Column(
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally // (for NavigationRail)
    ) {
        // Items list here
    }
    ```

---

## 5. Movies Screen Architecture & Sub-Tabs
* **Sub-Tab Selection Preservation**:
  - Use `rememberSaveable` to store `selectedTabIndex` in `MovieScreenTabs` so that it persists the selected sub-tab index when navigating away at the app-level and coming back.
* **Voyager Removal**: We have replaced Voyager tab dependencies with standard Compose Composable state rendering and dynamic ViewModel instantiation (`MovieListTab`).
* **ViewModel Reusability**: Do not cancel or clear the persisted ViewModels on tab switch. The view models are managed at the root `MainAppScreen` level, and they exist for the lifetime of the application run.

---

## 6. Screenshot & System Theme Guidelines
* **Clean Window Capture**: When capturing desktop screenshots, crop/capture only the desktop build window itself (it should not contain surrounding background app windows or desktop environments).
* **Restore Dark Mode Default**: Whenever you toggle system themes (e.g., changing macOS appearance preferences between light and dark mode to capture light/dark theme screenshots), always revert the system theme back to the default macOS dark mode configuration immediately after capturing.

---

## 7. Unit Testing Guidelines
* **MockK Properties**: Declare mock properties in unit tests using the `@MockK` annotation (e.g., `@MockK(relaxed = true) lateinit var mockRepository: TrendingRepository`) instead of inline mock initialization.
* **Mocking Setup**: Perform MockK annotations initialization (`MockKAnnotations.init(this)`) and all common mock behaviors (such as `coEvery { ... } returns ...`) inside the `@BeforeTest` (or `@Before`) setup method.
* **Avoid Production Constants**: Do **NOT** use actual constants imported from production classes inside unit tests (e.g., constants from `TrendingConstant`). Instead, redefine these constants inside a `private companion object` in the test class.
* **Private Companion Objects**: The companion object inside test classes should be declared `private companion object` to restrict constant visibility.
* **Common Test Constants**: Any common constants used in multiple test classes at the same feature level package should be moved to a shared test constants file (e.g., `TrendingTestConstants.kt`) rather than duplicated across companion objects.

---

## 8. General Code Cleanups & Instruction Propagation Guidelines
* **Remove Unused Imports**: Always check for and remove unused package imports during file modification, especially when types are inferred automatically (e.g., unused `Painter` imports).
* **Remember Instruction Rule**: Whenever the user instructs the agent to remember any rule, guideline, or instruction, the agent must immediately document it and append it to this `PROJECT_SKILL.md` file to preserve it across future agent sessions.

---

## 9. Exception Handling Guidelines
* **CRITICAL**: Do **NOT** use general `catch (e: Exception)` blocks for handling failures.
* **Catch Specific Targets**: Always catch specific, expected exceptions (e.g., `NoSuchMethodException`, `IOException`, `SecurityException`, `IllegalAccessException`, `InvocationTargetException`, `NullPointerException`).
* **Avoid Silent Swallow**: Do not swallow general Exceptions silently. If catching a general block is absolutely required for safety, log the error explicitly and ensure it doesn't mask other failure modes.
