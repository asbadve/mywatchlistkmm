# Known bugs

Reproducible defects with a known symptom, tracked here so they survive across sessions.
Engineering/ops work-in-progress lives in the (untracked) `TASKS.md`; product ideas live in
`future_features_checklist.md`.

Each entry: what you see, where it is, what has been ruled out, and the next concrete step.

---

## Hero content is invisible in light theme — PRIORITY

**Symptom.** In light theme the lower half of every detail hero disappears. On movies: the facts
row (`PG-13 2026 · 2h 25m · ★ 7.8`) and the "Play trailer" button. On TV: the network line, the
next-episode line and both provider chips. The filled "Watch on …" button survives because it is
`colorScheme.primary` rather than white. Dark theme is unaffected. Found on device 2026-08-13.

**Cause (confirmed, not a guess).** `heroScrimBrush` in `core/ui/hero/HeroComponents.kt` starts
interpolating toward `MaterialTheme.colorScheme.background` at `BASE_FADE_START = 0.82`, and the
hero's content column is bottom-aligned, so it sits inside that band. Every foreground in the hero
is a hardcoded `Color.White`, because it is drawn over artwork. In dark theme the band is
near-black and nothing shows; in light theme it is white, so white-on-white swallows the content.

`PersonHeroBanner` has a milder version of the same mistake: its flat wash is
`Color.Black.copy(alpha = 0.45f)`, so in light theme the backdrop turns muddy grey while the
gradient above it lightens.

**A fix was written, verified on device in both themes, and then reverted at the user's request**
(2026-08-13) - so this is a known-good approach rather than an open investigation:
1. Keep the scrim black through the whole content zone; only the band below the content may turn
   toward `background`. White content must not have a theme-dependent backing.
2. Hold the content above that band - `BoxWithConstraints` plus a bottom pad of the measured hero
   height times the band fraction, so the text cannot drift back into it as it grows.
3. Release the scrim to ~10% just before the end. Going from heavy black straight to `background`
   leaves a grey smudge across the bottom in light theme: a darkened image cannot dissolve into
   white without passing through grey.
4. `PersonHeroBanner`'s wash takes `colorScheme.background` rather than black - pushing an image
   back means moving it toward the surface behind it.

**No test will catch this.** Compose UI tests assert node existence, not rendered colour, so a
white-on-white string still "exists". Only screenshot testing would.

## Web (JS) — navigation icon not visible

**Symptom.** The navigation (back) icon does not render on the JS/browser target. Reported
2026-08-06. Android, desktop and iOS all show it correctly, so this is target-specific rather than a
regression in the shared composable.

**Where.** Every detail screen now draws its back affordance through one component,
`core/ui/DetailTopBar.kt`, using `Icons.AutoMirrored.Filled.ArrowBack`. The app-level search bar and
`SearchScreen` have their own icons, so a first useful data point is whether *those* icons render on
web too - if they also fail, this is about icon loading in general, not `DetailTopBar`.

**Ruled out.** Not a missing dependency: `libs.material.icons.core` is declared in `commonMain`
(`composeApp/build.gradle.kts:63`), so the JS target inherits it.

**Worth checking, in order.**
1. Whether *any* `androidx.compose.material.icons` vector renders on web, or only this one - 21
   files in `commonMain` import from that package, so a broad failure would be obvious.
2. `Icons.AutoMirrored.*` specifically. The auto-mirrored variants resolve through a different path
   than plain `Icons.Filled.*`; swapping one call site to `Icons.Filled.ArrowBack` is a two-minute
   test of that theory.
3. Tint. `DetailTopBar` tints the icon `onSurface` when solid and white over a hero image. An icon
   drawn in a colour matching its background is invisible rather than absent - check the DOM/canvas
   for a node of the right size before assuming it never drew.

**Blocked on.** Verifying any of this needs the browser target to render at all - see the blank-page
bug below, which has to be fixed first.

## Web (JS) — blank page

**Symptom.** `./gradlew :composeApp:jsBrowserDevelopmentRun` serves on `:8080` but renders nothing.

**Partial diagnosis.** `index.html` carries a stale `<script src="skiko.js">` tag; skiko 0.144.6
ships `skiko.mjs`, meant to be bundled by webpack. That alone should not blank the page, so a second
unidentified 404 is still outstanding.

**Next step.** Log 400+ responses via `page.on('response')` in puppeteer/Chrome DevTools against
`localhost:8080` to find the real missing asset, then fix the stale tag and whatever else 404s.
