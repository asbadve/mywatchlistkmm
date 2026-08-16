---
name: detail-screen-scroll-jank
description: Movie/TV detail screen scroll jank investigation - mostly resolved. Read this before touching TvDetailScreen.kt/MovieDetailScreen.kt scroll performance, or before trusting any on-device jank measurement in this repo - it has the confirmed root cause (measure in release, not debug), the exact repro/benchmark-build commands, and two documented dead ends so they aren't retried.
---

# Detail screen fast-swipe scroll jank

GitHub issue: see repo issues labeled `performance` for the tracking issue (search "scroll
flicker" / "detail screen" if the number isn't obvious from context) - it's been updated with the
finding below and downgraded pending a user confirming whether release-build scrolling still
feels janky on their device.

## Headline finding: measure in a release/non-debuggable build, not `installDebug`

**This is the fix.** Every number in this investigation before switching to a release build was
dominated by debug-build overhead, not a real architectural problem in the detail screens.

Official Compose guidance (developer.android.com, "Lists and grids" performance page):
> "You can only reliably measure the performance of a Lazy layout when running in release mode
> and with R8 optimization enabled. On debug builds, Lazy layout scrolling may appear slower."

Confirmed on a Pixel 3 XL, same exact repro, same device, minutes apart:

| Build | Janky frames | Frames rendered | 50th pct |
|---|---|---|---|
| Debug (`installDebug`) | 22-30% | 30-49 | ~16-22ms |
| Release (`installRelease`, debug-signed) | **1.33-1.35%** | 74-75 | 17ms |

Three release-build samples, each after a confirmed cooldown (`Thermal Status: 0`, ~33.4°C),
came back essentially identical (1.33%, 1.33%, 1.35% - see Methodology below for why that
cooldown discipline matters). That's about as close to "at budget" as real hardware gets, and it
makes debug-build jank look like a measurement artifact more than a genuine bug.

**Building the benchmark APK:**

`composeApp/build.gradle.kts` has a `release` build type signed with the debug key, added purely
so `assembleRelease`/`installRelease` produce an installable APK for local benchmarking (it isn't
wired to any real signing secret - never use this to ship). Without it, AGP won't generate an
`installRelease` task at all for an unsigned release variant.

```bash
./gradlew :composeApp:installRelease -q
adb shell dumpsys package com.ajinkyabadve.kmmmywatchlist.androidApp | grep -i debuggable
# should print nothing - a debug build would show DEBUGGABLE in the flags line
```

## What's actually still true about the mechanism (kept for context, lower priority now)

A fast fling *does* move a `LazyColumn`'s viewport far enough per input batch that several
never-before-composed sections land in the same Choreographer frame and must be composed +
measured + laid out together - confirmed via on-device instrumentation (see Methodology) that
`VideoClipsSection` and `CurrentSeasonSection` compose in the same frame on a **debug** build and
take ~115ms to finish laying out. Whether this mechanism causes any *user-perceptible* stutter on
a release build is now genuinely unclear - the aggregate `gfxinfo` numbers on release are close to
budget, but that doesn't rule out a single still-elevated frame hiding inside a 1-2% jank rate.
If a user reports real, felt stutter on a release build specifically, this mechanism is where to
look next - but don't assume it's still a live problem without new evidence.

## Methodology corrections (read before measuring anything on this device again)

Two additional measurement pitfalls were found this session, on top of the debug/release one
above. Both make results *appear* to get worse over a testing session independent of any code
change - watch for this pattern (numbers trending worse run-over-run within one sitting) as a sign
of contaminated methodology, not a regression.

### 1. Cold-start variance after every relaunch

Repeatedly doing `am force-stop` + `am start` before each measured swipe means most swipes are
dominated by JIT warmup, class loading, and Coil cache-cold-start costs rather than steady-state
scroll performance. Observed: 3 swipes right after a fresh launch gave 100%, 100%, then 22.58%
janky frames - the third, once caches were warm, matched the established baseline exactly.
**Do a throwaway unmeasured swipe (down then back up) after launch before taking any measurement.**

### 2. Thermal throttling accumulates within a handful of rapid swipes

The Pixel 3 XL's big cores (cpu4-7, 2.8GHz max) throttle to their floor (825MHz - 29% of max)
within about 5 rapid back-to-back swipes, even on an already-warm process with no relaunches
between them. Observed within one 20-second burst: 23%, 30%, then 87%, 75%, 87% janky frames -
degrading for thermal reasons alone, nothing to do with code. Checking `Thermal Status` *between*
runs is misleading - the cores spike hot during the swipe and cool back to idle within seconds,
so a clean reading between bursts doesn't mean the burst itself was clean.

**Protocol that produced trustworthy, reproducible numbers** (used for the release-build samples
above): one measured swipe, then a **genuine ~3-minute cooldown** confirmed via:

```bash
adb shell dumpsys thermalservice | grep "Thermal Status"   # must read 0 (NONE), not 1 (LIGHT)
adb shell dumpsys battery | grep temperature                # falling/stable, not still climbing
```

before the next measured swipe. Slower than a rapid-fire batch, but it's the only protocol that
gave results that didn't contradict themselves. A `run_in_background` bash loop polling both of
those every 30s while doing something else (or a `ScheduleWakeup` if in a `/loop`) is more
productive than sitting and waiting.

## Reproduction + measurement commands

Physical device only - see "Why the emulator doesn't reliably show this" below. Use the
**release** build (see above), navigate to a Movie or TV detail screen fresh (top of the list,
after the cold-start throwaway swipe), then:

```bash
adb shell dumpsys gfxinfo com.ajinkyabadve.kmmmywatchlist.androidApp reset > /dev/null
adb shell input swipe 700 2600 700 300 300   # one fast top-to-bottom swipe (device-resolution-dependent coords)
sleep 1.5
adb shell dumpsys gfxinfo com.ajinkyabadve.kmmmywatchlist.androidApp | sed -n '6,10p'
```

### Finer-grained: which section(s) are in a slow frame (debug-build only)

`dumpsys gfxinfo` tells you *that* frames are slow, not *which section*. Layout Inspector isn't
drivable headlessly from this environment, and a Perfetto trace capturing `atrace_categories:
view/gfx` + `atrace_apps` did **not** resolve any app-process slices on this device (Android 12,
Pixel 3 XL) - the trace loaded fine but had zero slices attributed to the app's process, likely a
permission/symbolication gap with headless `perfetto`. Don't spend time on that route again
without first checking whether `atrace_apps` actually captures anything trivial before building a
whole config around it.

What worked instead: temporary per-item timing markers added directly to the `LazyColumn` items,
using wall-clock `logcat` timestamps (not internal timers - internal `TimeSource.Monotonic` per
item is misleading since each item's clock starts at its own first composition, so everything
reads ~0ms; the useful data is the *delta between logcat lines*, which needs no in-app timer):

```kotlin
@Composable
private fun ProfiledSection(label: String, content: @Composable () -> Unit) {
    var logged by remember { mutableStateOf(false) }
    Napier.d(tag = "SectionTiming") { "$label COMPOSE" }
    Box(modifier = Modifier.onGloballyPositioned {
        if (!logged) { logged = true; Napier.d(tag = "SectionTiming") { "$label LAID_OUT" } }
    }) { content() }
}
```

Wrap each `item { ... }`'s content with `ProfiledSection("Name") { ... }`, deploy (debug build is
fine here since you're only comparing sections against each other, not measuring absolute jank),
clear logcat, run the exact repro above, then:

```bash
adb logcat -c
adb shell input swipe 700 2600 700 300 300
sleep 3
adb logcat -d --pid=$(adb shell pidof com.ajinkyabadve.kmmmywatchlist.androidApp) | grep SectionTiming
```

**Always revert this instrumentation before finishing** - confirm with
`grep -n "ProfiledSection\|SectionTiming"` returning nothing before considering the change done.

### Why the emulator doesn't reliably show this

Tried once (2026-08-16) on the `Small_Phone` AVD (`sdk_gphone16k_arm64`, booted via
`"$ANDROID_HOME/emulator/emulator" -avd Small_Phone`, *not* the deprecated `tools/emulator`
wrapper). Same repro (debug build), 3 runs: 23.40%, 3.77%, 3.85% janky frames - reproducible but
far weaker and inconsistent versus the Pixel 3 XL's steady 22-30% (also on debug). The emulator
runs on the host Mac's much faster CPU/GPU, so the same debug-build overhead usually finishes
inside budget anyway. **Use the physical device as ground truth** - the emulator can hint that a
regression got worse, but a fix that "works" only on emulator hasn't been verified. This caveat
matters less now that release-build numbers are already close to budget, but keep it in mind if
debug-build numbers come up again for some reason.

## Two changes were tried on the (incorrect, debug-build) theory that composition bursts needed
## fixing, both reverted - do not retry either without new evidence

Both were measured against the *debug*-build baseline, before the release-build finding above was
known. Given release-mode scrolling is already close to budget, neither is likely worth revisiting
unless a *release*-build measurement shows a real problem first.

### Stable `key`s on all `LazyRow`/`LazyColumn` `items()` calls - no measurable effect, reverted

Added `key = { it.id }` (or equivalent) to every `items()` call across both detail screens'
sections. Correct Compose hygiene in isolation, but re-measured with the exact repro and jank was
unchanged (still 22-30% on debug). Keys help recomposition/reordering *reuse*; they don't reduce
first-time composition cost, which is what was blowing the frame budget. Reverted alongside the
prefetch-strategy change below once the debug/release finding made the whole premise moot - if
re-added later, do it as its own deliberate hygiene pass, not as a jank fix.

### Two-phase deferred render (`DeferredSection`) - reverted, made it WORSE

Wrapped every below-the-fold section in a component rendering a fixed-height placeholder for one
frame, then swapping to real content:

```kotlin
@Composable
fun DeferredSection(placeholderHeight: Dp = 220.dp, content: @Composable () -> Unit) {
    var ready by remember { mutableStateOf(false) }
    if (ready) content() else {
        Box(Modifier.fillMaxWidth().height(placeholderHeight))
        LaunchedEffect(Unit) { withFrameNanos {}; ready = true }
    }
}
```

Measured result: **63-90% janky frames** (debug build) - a regression. The placeholder's fixed
height doesn't match the real content's height, so every placeholder→real swap forces
`LazyColumn` to re-layout everything below it; with 7 sections swapping in quick succession after
one fast fling, that's 7 cascading relayout passes competing with the fling itself.

### Custom `LazyListPrefetchStrategy` widening the lookahead - also reverted, also WORSE

The platform's own windowed strategy (`LazyListCacheWindowStrategy`) does exactly what you'd want
here but is `internal` to `androidx.compose.foundation` in the Compose Multiplatform version
pinned in `gradle/libs.versions.toml` (1.11.1) - not callable from application code. A hand-rolled
`LazyListPrefetchStrategy` scheduling N items ahead of the last visible one (via the sealed
`LazyListPrefetchScope.schedulePrefetch`) was tried instead. Measured result: **75-83% janky
frames** (debug build) - also a regression, most likely because prefetch work competes with the
fling's own main-thread work rather than running in genuinely idle time. No unit test exists for
this kind of strategy: `LazyListPrefetchScope` is `sealed` to Compose Foundation, so application
code cannot implement a fake for it - any future attempt at this needs on-device measurement as
its only verification, same as this one had.
