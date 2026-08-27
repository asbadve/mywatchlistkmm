---
name: run-app
description: Run and visually verify the MyWatchList Compose Multiplatform app on desktop (JVM), iOS Simulator, and Android. Includes how to screenshot each platform, and how to drive the UI - Maestro (tapOn/assertVisible by text) on Android/iOS, Appium + Mac2Driver on desktop (with mandatory teardown), adb/cliclick coordinates where neither reaches (raw scroll gestures, web/JS) - without user interaction.
---

# Run & visually verify MyWatchList

Fastest feedback loop for common UI code: **compile desktop → run desktop → screenshot**.
Compile check: `./gradlew :composeApp:compileKotlinDesktop`. Tests: `./gradlew :composeApp:desktopTest`.

**Pick the platform by what you need to see:**

| Need | Platform |
|---|---|
| A static screen, fastest | Desktop |
| Anything triggered by **scrolling, tapping, navigation, or insets** | **Android or iOS, driven with Maestro** (see below) |
| Same, but on **desktop** specifically | **Appium + Mac2Driver** (see below) |
| Expanded/split layouts | iOS Simulator (iPad) or desktop, resized |

System-bar / edge-to-edge behaviour is only real on Android - desktop has no insets.

## Driving the UI: prefer Maestro over raw coordinate taps (Android/iOS)

For anything beyond a single static screenshot - opening a detail screen, filling a dialog,
scrolling to trigger pagination - **use Maestro**, not `adb input tap`/`cliclick` coordinate
math. Confirmed on 2026-08-23: coordinate-based driving needs a screenshot after nearly every
tap just to check it landed (each one costs real tokens to view), plus manual window-position/
coordinate-space recalculation, plus blind retries when a tap silently misses. Maestro's
`tapOn: "text"` finds the element by its actual text/accessibility label and fails loudly if it's
not there - no verification screenshot needed per step, only at real checkpoints. The same
multi-screen flow that took a dozen screenshot round-trips with cliclick took 3 tool calls with
Maestro.

```bash
export PATH="$PATH":"$HOME/.maestro/bin"    # installed via: curl -Ls "https://get.maestro.mobile.dev" | bash
maestro test flow.yaml                       # Android: auto-picks the one attached/booted device
maestro --udid <UDID> test flow.yaml         # iOS: device id is required
```

A flow is a small YAML file (write it to the scratchpad, not the repo):

```yaml
appId: com.ajinkyabadve.kmmmywatchlist.androidApp   # iOS: com.ajinkyabadve.kmmmywatchlist.iosApp
---
- launchApp:
    clearState: false        # keep the existing login session
- tapOn: "Mutiny"             # matches by visible text/accessibility label, not coordinates
- extendedWaitUntil:
    visible: "Genres"         # wait for a real signal the screen finished loading, not a sleep
    timeout: 15000
- takeScreenshot: movie_detail
- tapOn: "Back"
```

- Install the app first the normal way for each platform (`installDebug` / `xcrun simctl
  install`, see below) - Maestro drives an already-installed app, it doesn't build one.
- `assertVisible`/`extendedWaitUntil` are the fail-fast replacement for "screenshot and eyeball
  it" - only take an actual screenshot when you need to see something Maestro can't assert on
  (layout, colors, image content).
- **Coverage gap: Maestro cannot drive this app's desktop (JVM/Swing window) or web/JS targets at
  all** - confirmed by testing both. Desktop's gap is closed by Appium + Mac2Driver instead (next
  section) - web's isn't: Compose renders it to one canvas with no DOM/accessibility tree for
  *any* automation tool to query, Maestro or otherwise. Fall back to `cliclick`/coordinate taps
  for web only.

## Driving the UI: Appium + Mac2Driver (desktop)

Compose Desktop exposes a real, complete macOS accessibility tree (confirmed 2026-08-23 via
`System Events`/AX APIs - every nav item, card, and button shows up as a proper `AXRadioButton`/
`AXButton`/`AXStaticText` with a real accessible label) - Appium's **Mac2Driver** automates
against exactly that tree, closing the gap Maestro leaves on desktop. It needs a real `.app`
bundle (not the bare JVM process `./gradlew :composeApp:run` launches) so it can attach via
`NSWorkspace`/bundle ID:

```bash
npm install -g appium                 # one-time; already installed as of 2026-08-25
appium driver install mac2            # one-time
pip3 install --user Appium-Python-Client   # one-time; client library

./gradlew :composeApp:createDistributable   # builds composeApp/build/compose/binaries/main/app/MyWatchList.app
                                              # bundle id: com.ajinkyabadve.kmmmywatchlist (build.gradle.kts's nativeDistributions.macOS.bundleID)

appium server --port 4723 &           # start the automation server, per test session
```

Python client (Appium-Python-Client), locating elements by their accessible label via
`AppiumBy.ACCESSIBILITY_ID` - the same labels the AX tree dump above showed:

```python
from appium import webdriver
from appium.options.mac import Mac2Options
from appium.webdriver.common.appiumby import AppiumBy

options = Mac2Options()
options.platform_name = "Mac"
options.automation_name = "Mac2"
options.set_capability("appium:bundleId", "com.ajinkyabadve.kmmmywatchlist")

driver = webdriver.Remote("http://127.0.0.1:4723", options=options)
try:
    driver.find_element(AppiumBy.ACCESSIBILITY_ID, "Movies").click()
    driver.find_element(AppiumBy.ACCESSIBILITY_ID, "Now Playing")   # raises if navigation didn't happen
    driver.save_screenshot("out.png")
finally:
    driver.quit()   # mandatory - see teardown below
```

### Teardown - do this after every test run, not just at session end

`driver.quit()` ends the Appium session but **does not** kill the automation helper process it
spawned. Left running, it keeps macOS's accessibility-automation indicator active and can
interfere with the next run's `xcodebuild`/launch step. Confirmed leftover after `driver.quit()`
on 2026-08-25:

```bash
ps aux | grep -i "WebDriverAgentRunner-Runner\|xcodebuild.*WebDriverAgentMac"
```

Kill all of it, plus the server and the app under test, once the test flow is done:

```bash
pkill -f "WebDriverAgentRunner-Runner"          # the automation helper .app Mac2Driver launches
pkill -f "xcodebuild.*WebDriverAgentMac"        # the build-for-testing process that launched it
pkill -f "appium server"                        # the Appium server itself
pkill -f "MyWatchList.app/Contents/MacOS/MyWatchList"   # the app under test
```

Do this at the end of every Mac2Driver-based test, not just when wrapping up for the day - a
stray `WebDriverAgentRunner-Runner` left over from one test can hold accessibility control that
confuses the next one.

## Desktop (JVM) — fastest way to see the app

```bash
./gradlew :composeApp:run   # run in background; window appears in ~30-60s (Gradle daemon +
                              # compile overhead - NOT representative of a real launch, see below)
```

- The window is owned by process `MainKt` when launched via `:composeApp:run` — but owned by
  process `MyWatchList` when launched as the real packaged app (below). Match on **both** names
  or you'll silently find nothing:

```swift
import CoreGraphics
import Foundation
let list = CGWindowListCopyWindowInfo([.optionOnScreenOnly, .excludeDesktopElements], kCGNullWindowID) as! [[String: Any]]
for w in list where (w[kCGWindowLayer as String] as? Int ?? 0) == 0 {
    let owner = w[kCGWindowOwnerName as String] as? String ?? "?"
    if owner.contains("MyWatchList") || owner.contains("MainKt") {
        let b = w[kCGWindowBounds as String] as! [String: Any]
        print("\(w[kCGWindowNumber as String] ?? -1) | \(owner) | \(w[kCGWindowName as String] ?? "") | \(b)")
    }
}
```

- Query needs no accessibility permission. **Find the window's real position before
  screenshotting or clicking** — it may be on a second display (x > 1920).
- Screenshot a region: `screencapture -x -R<x>,<y>,<w>,<h> out.png` (screen recording permission
  is granted; works across displays) - but a region capture grabs *whatever's on screen at those
  pixels*, including another window that's since moved on top (confirmed 2026-08-27: a terminal
  window overlapping the same coordinates got captured instead of the app). Prefer capturing by
  window id instead, which captures that window's actual buffer regardless of what's stacked on
  top of it on screen: `screencapture -x -o -l<windowID> out.png` (window id from the CGWindowList
  script above).
- **Real, packaged launch is much faster than `:composeApp:run`** (confirmed 2026-08-27): building
  and opening the actual `.app` (`./gradlew :composeApp:createDistributable`, then
  `open composeApp/build/compose/binaries/main/app/MyWatchList.app`) shows a window in **~10s**,
  vs. `:composeApp:run`'s ~30-60s Gradle/daemon overhead. Use the packaged app, not `run`, for
  anything timing-sensitive (splash duration, cold-start feel) - `run`'s number isn't a real launch
  time and will make things look slower/faster than they actually are.
- **The Compose splash (`core/ui/splash/SplashScreen.kt`) is effectively never visible on desktop
  in practice** (confirmed 2026-08-27, both via `:composeApp:run` and the real packaged app): the
  AWT/Skiko window isn't created/shown until Compose has real content ready to paint - there's no
  OS-level placeholder shown at process start the way Android/iOS show one immediately. Since even
  the packaged app's ~10s launch is far longer than the splash's own ~1.8s timer, the entire splash
  sequence completes *before* the window ever becomes visible; the first frame a user actually sees
  is already the fully-loaded main screen. Not a bug in the splash code - just means it has no
  practical effect on desktop, worth knowing before spending time trying to screenshot it there.
- Clicking/dragging: `cliclick c:<x>,<y>` and `osascript ... tell application "System Events"`
  need macOS Accessibility permission for the terminal app - **granted as of 2026-08-27** (earlier
  session notes calling this blocked are stale). Windows list order from CGWindowList is
  front-to-back; verify nothing overlaps the target point before clicking, and use global
  coordinates (second display starts at x=1920).
- **Resizing via System Events**:
  `osascript -e 'tell application "System Events" to tell process "MyWatchList" to set size of window 1 to {W, H}'`
  (read current with `get size of window 1`). Confirmed 2026-08-27: the window clamps to a
  **minimum width of ~650px** - requesting narrower (e.g. 350) silently no-ops and leaves it at
  650. Layout reflows correctly at both the clamped-minimum width (NavigationRail, single-ish
  column grid) and a wide window (NavigationRail, many-column grid) - no glitches at either extreme.
- Stop the app with `pkill -f MainKt` (after `:composeApp:run`) or
  `pkill -f "MyWatchList.app/Contents/MacOS/MyWatchList"` (after the packaged app) before
  relaunching a new build - old instances keep running otherwise.

## iOS Simulator — works without any special permissions

`xcrun simctl` can boot/install/launch/screenshot with no accessibility needed:

```bash
xcrun simctl list devices available                     # pick a device UDID (iPads show expanded layout)
xcrun simctl boot <UDID>
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -destination 'platform=iOS Simulator,id=<UDID>' -derivedDataPath iosApp/build/claude-dd build
xcrun simctl install <UDID> iosApp/build/claude-dd/Build/Products/Debug-iphonesimulator/MyWatchList.app
xcrun simctl launch <UDID> com.ajinkyabadve.kmmmywatchlist.iosApp
xcrun simctl io <UDID> screenshot shot.png              # retry until non-empty; app takes a few seconds to boot
```

- Bundle id: `com.ajinkyabadve.kmmmywatchlist.iosApp`. Scheme: `iosApp`.
- **`simctl boot` is headless** - when the user wants to SEE the simulator, also run
  `open -a Simulator` (optionally `--args -CurrentDeviceUDID <UDID>`) to show the window.
- Downscale screenshots before viewing: `sips -Z 1100 shot.png`.
- **`simctl` itself has no tap command**, but Maestro does drive iOS taps successfully (see
  "Driving the UI" above) - `maestro --udid <UDID> test flow.yaml` against the installed app.
  Reserve raw click-through flows for desktop only when Maestro doesn't apply.

## Android

Compile check: `./gradlew :composeApp:assembleDebug`. For element-based taps/navigation, prefer
Maestro (see "Driving the UI" above) over the raw `adb input tap` coordinates below - reserve
`adb input swipe`/`tap` for things Maestro can't express (e.g. a raw scroll-by-coordinate gesture
to specifically test scroll-triggered `LoadState`/prefetch behavior). Verified against a real
device and emulators on 2026-08-06.

```bash
adb devices -l                            # ALWAYS check first
export ANDROID_SERIAL=emulator-5554       # pin the target; adb aborts on >1 device
./gradlew :composeApp:installDebug        # installs to every attached device
adb shell am force-stop com.ajinkyabadve.kmmmywatchlist.androidApp
adb shell am start -n com.ajinkyabadve.kmmmywatchlist.androidApp/com.ajinkyabadve.kmmmywatchlist.AppActivity
```

- **Pin the device with `ANDROID_SERIAL`, not a `$D="-s foo"` variable** - zsh does not
  word-split unquoted variables, so `adb $D shell` fails with `-s requires an argument`.
- Launcher activity: `com.ajinkyabadve.kmmmywatchlist.androidApp/com.ajinkyabadve.kmmmywatchlist.AppActivity`.
- Always `force-stop` before `am start`; otherwise you re-photograph the previous run's state.

### Screenshots - do NOT use `adb exec-out screencap -p >file.png`

On any multi-display emulator `screencap` prepends a *"Multiple displays were found"* warning to
stdout, which corrupts the PNG (it reads as text, not an image). Write on-device and pull instead:

```bash
adb shell dumpsys SurfaceFlinger --display-id      # list display ids
adb shell screencap -p -d <DISPLAY_ID> /sdcard/s.png
adb pull /sdcard/s.png shot.png && sips -Z 900 shot.png
```

- **The first display listed is not necessarily the live one.** On `sdk_gphone64_arm64` the app was
  on the *second* id (`...147201`); the first captured pure black. If a shot is all black, try the
  other id before assuming the app failed to draw.
- `-d 0` is rejected - these are full 19-digit display ids, not indices.

### Driving the UI

```bash
adb shell wm size                                  # e.g. 1080x2424, for picking coordinates
adb shell input swipe 540 1900 540 500 250         # scroll DOWN the list (finger moves up)
adb shell input swipe 540 500 540 1900 250         # scroll back UP
adb shell input tap <x> <y>
adb shell input keyevent KEYCODE_BACK
adb shell settings put global window_animation_scale 0   # steadier before/after shots
```

Swipe args are `x1 y1 x2 y2 duration_ms`. Durations under ~200ms fling; ~250ms gives a controlled
scroll. Sleep ~1s after a swipe before screenshotting so the frame settles.

### Logs

```bash
adb logcat -c                                                   # clear first
adb logcat -d --pid=$(adb shell pidof com.ajinkyabadve.kmmmywatchlist.androidApp)
```

- Logcat is flooded with `I/View  setRequestedFrameRate` spam - always grep for what you want.
- HTTP logging only appears if `initLogging()` ran (debuggable builds only, see
  `core/logging/AppLogging.kt`); grep the tag `HTTP Client`.

### Force-firing the episode-notification poll (checklist item 3a)

`TvEpisodeNotificationPoller` runs on a `WorkManager` `PeriodicWorkRequest` (see
`core/notification/NotificationScheduler.kt`, androidMain) with a 6h interval and no 15-minute-floor
override - waiting for it to fire naturally isn't practical for verification. Two ways to force it,
in order of preference:

1. **Debug-only "Poll episode notifications now" row** on the Account screen (`AccountScreen.kt` -
   only rendered when `isDebugBuild()` is true, i.e. never in a release build). Tap it to call
   `TvEpisodeNotificationPoller().poll()` directly, bypassing `WorkManager`/`BGTaskScheduler`
   entirely. Fast and reliable, but only proves the poller+notifier logic, not the scheduling
   wiring itself.
2. **Force-run the actual scheduled job via `adb`** (proves the real `WorkManager` path fires) -
   toggle "Episode notifications" on in Settings at least once first so the job exists, then:

```bash
adb shell dumpsys jobscheduler | grep -B2 -A20 "com.ajinkyabadve.kmmmywatchlist.androidApp"
# find the "JOB #<uid>/<jobId>: ..." line for EpisodeNotificationWorker
adb shell cmd jobscheduler run -f com.ajinkyabadve.kmmmywatchlist.androidApp <jobId>
```

Re-run the same trigger a second time and confirm no duplicate notification appears - that's the
dedup ledger (`notificationLedger` table) doing its job, not a fluke of only firing once.

## JS (browser)

- `./gradlew :composeApp:jsBrowserDevelopmentRun` exists but is slow; not part of the normal
  verification loop.
