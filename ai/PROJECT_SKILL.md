# MyWatchList KMM Project Skill File & Guidelines

This document contains core instructions, architectural decisions, and layout rules for the MyWatchList Compose Multiplatform project. Read and follow these instructions in all subsequent agent sessions.

---

## 1. Environment & Build Configuration
* **Java Version**: Always use **Java 17** for compilation. Ensure compatible JDK setup across Android, Desktop, and iOS.
* **Web Target**: The JS/Web target is currently parked due to signature mismatch issues. Do not spend time trying to compile it unless explicitly requested by the user.
* **Android Target**: Always use the **android-cli** skill and the `android` command-line tool by Google for Android builds and environments diagnostics.

---

## 2. Code Commits & Staging
* **CRITICAL**: Do **NOT** commit, stage, or check in any modified files unless the user explicitly instructs you to do so. All code changes should remain local.

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
