---
name: run-app
description: Run and visually verify the MyWatchList Compose Multiplatform app on desktop (JVM), iOS Simulator, and Android. Includes how to screenshot each platform without user interaction.
---

# Run & visually verify MyWatchList

Fastest feedback loop for common UI code: **compile desktop → run desktop → screenshot**.
Compile check: `./gradlew :composeApp:compileKotlinDesktop`. Tests: `./gradlew :composeApp:desktopTest`.

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
- Downscale screenshots before viewing: `sips -Z 1100 shot.png`.
- **No tap/UI-driving support**: `simctl` has no tap command and `idb`/`axe` are not installed,
  so only launch-state screens can be verified on iOS. Use desktop for click-through flows.

## Android

- Compile check only: `./gradlew :composeApp:assembleDebug` (no emulator verified yet).

## JS (browser)

- `./gradlew :composeApp:jsBrowserDevelopmentRun` exists but is slow; not part of the normal
  verification loop.
