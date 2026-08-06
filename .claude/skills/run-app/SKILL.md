---
name: run-app
description: Run and visually verify the MyWatchList Compose Multiplatform app on desktop (JVM), iOS Simulator, and Android. Includes how to screenshot each platform, and how to drive the Android UI (scroll/tap) via adb, without user interaction.
---

# Run & visually verify MyWatchList

Fastest feedback loop for common UI code: **compile desktop → run desktop → screenshot**.
Compile check: `./gradlew :composeApp:compileKotlinDesktop`. Tests: `./gradlew :composeApp:desktopTest`.

**Pick the platform by what you need to see:**

| Need | Platform |
|---|---|
| A static screen, fastest | Desktop |
| Anything triggered by **scrolling, tapping or insets** | **Android** (only `adb` can drive the UI) |
| Expanded/split layouts | iOS Simulator (iPad) or desktop, resized |

System-bar / edge-to-edge behaviour is only real on Android - desktop has no insets.

## Desktop (JVM) — fastest way to see the app

```bash
./gradlew :composeApp:run   # run in background; window appears in ~30-60s
```

- The window is owned by process `MainKt`, titled `MyWatchList`.
- **Find the window's real position before screenshotting or clicking** — it may be on a
  second display (x > 1920). Query CGWindowList (needs no accessibility permission) with a
  tiny Swift script:

```swift
import CoreGraphics
import Foundation
let list = CGWindowListCopyWindowInfo([.optionOnScreenOnly, .excludeDesktopElements], kCGNullWindowID) as! [[String: Any]]
for w in list where (w[kCGWindowLayer as String] as? Int ?? 0) == 0 {
    let b = w[kCGWindowBounds as String] as! [String: Any]
    print("\(w[kCGWindowOwnerName as String] ?? "?") | \(w[kCGWindowName as String] ?? "") | \(b)")
}
```

- Screenshot a region: `screencapture -x -R<x>,<y>,<w>,<h> out.png` (screen recording permission
  is granted; works across displays).
- Clicking: `cliclick c:<x>,<y>` exists but **requires macOS Accessibility permission for the
  terminal app — currently NOT granted**, and osascript System Events is blocked for the same
  reason. Windows list order from CGWindowList is front-to-back; verify nothing overlaps the
  target point before clicking, and use global coordinates (second display starts at x=1920).
- Stop the app with `pkill -f MainKt` before relaunching a new build (old instance keeps running).

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
- **No tap/UI-driving support**: `simctl` has no tap command and `idb`/`axe` are not installed,
  so only launch-state screens can be verified on iOS. Use desktop for click-through flows.

## Android — the only platform that can be UI-driven end to end

Compile check: `./gradlew :composeApp:assembleDebug`. Unlike iOS, `adb` can *drive* the app
(scroll, tap, back), so scroll-triggered behaviour can be verified here and nowhere else.
Verified against a real device and emulators on 2026-08-06.

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

## JS (browser)

- `./gradlew :composeApp:jsBrowserDevelopmentRun` exists but is slow; not part of the normal
  verification loop.
