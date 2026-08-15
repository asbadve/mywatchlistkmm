package com.ajinkyabadve.kmmmywatchlist.core.ui.hero

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.ajinkyabadve.kmmmywatchlist.theme.LocalIsDarkTheme

private object HeroColorConstant {
    const val DARK_TOP_SCRIM_ALPHA = 0.55f
    const val DARK_MID_SCRIM_ALPHA = 0.45f
    const val DARK_BASE_FADE_ALPHA = 0.92f

    // Higher than their dark counterparts on purpose - see the KDoc on `forTheme`.
    const val LIGHT_TOP_SCRIM_ALPHA = 0.60f
    const val LIGHT_MID_SCRIM_ALPHA = 0.80f
    const val LIGHT_BASE_FADE_ALPHA = 0.95f

    /** Reads as "live" against a black scrim; far too pale to survive a white one. */
    const val DARK_ONGOING = 0xFF6FE0A0

    const val DARK_CHIP_SURFACE_ALPHA = 0.10f
    const val DARK_CHIP_OUTLINE_ALPHA = 0.16f
    const val DARK_BUTTON_SURFACE_ALPHA = 0.12f
    const val DARK_BUTTON_OUTLINE_ALPHA = 0.28f

    // Far higher than their dark counterparts, and not a mirror of them: a translucent white edge
    // on a dark scrim reads as a lit rim at 16%, while the same alpha in near-black on a white veil
    // is barely a smudge. The fills stay low so the controls read as outlined rather than filled.
    const val LIGHT_CHIP_SURFACE_ALPHA = 0.06f
    const val LIGHT_CHIP_OUTLINE_ALPHA = 0.38f
    const val LIGHT_BUTTON_SURFACE_ALPHA = 0.06f
    const val LIGHT_BUTTON_OUTLINE_ALPHA = 0.45f

    /** A warm off-white rather than pure white - it sits better on photographic colour. */
    const val PHOTO_PAPER = 0xFFF3F1EA

    /** Not pure black either: a hair of blue keeps the scrim from deadening warm artwork. */
    const val PHOTO_SCRIM = 0xFF060507

    const val PHOTO_TOP_SCRIM_ALPHA = 0.55f
    const val PHOTO_MID_SCRIM_ALPHA = 0.55f
    const val PHOTO_BASE_FADE_ALPHA = 0.92f
    const val PHOTO_CHIP_SURFACE_ALPHA = 0.10f
    const val PHOTO_CHIP_OUTLINE_ALPHA = 0.18f
    const val PHOTO_BUTTON_SURFACE_ALPHA = 0.08f
    const val PHOTO_BUTTON_OUTLINE_ALPHA = 0.35f
}

/**
 * The colours the hero draws with, which the `ColorScheme` cannot supply on its own.
 *
 * A hero puts content on top of artwork rather than on a surface, so its foreground is not
 * `onSurface`-over-`surface` - it is whatever survives being laid over an unknown image. That makes
 * the choice depend on the theme in a way Material's roles do not express, which is why it lives
 * here rather than as more `colorScheme` lookups scattered through the hero files.
 */
@Immutable
internal data class HeroColors(
    /** The veil laid over the artwork so the foreground has something predictable to sit on. */
    val scrim: Color,
    /** Every foreground drawn on the veil: title, facts, chips, buttons, back icon. */
    val onHero: Color,
    /** The "still running" accent on the TV hero's status badge. */
    val ongoing: Color,
    val chipSurface: Color,
    val chipOutline: Color,
    val buttonSurface: Color,
    val buttonOutline: Color,
    val topScrimAlpha: Float,
    val midScrimAlpha: Float,
    val baseFadeAlpha: Float,
) {
    companion object {
        /**
         * Dark theme darkens the artwork and writes on it in white; light theme lightens the
         * artwork and writes on it in near-black. Same gradient shape, mirrored tokens.
         *
         * The light alphas are the higher pair, which is not an oversight. A black scrim at 0.45
         * still reads as cinematic; a white veil at 0.45 only reads as faded, and leaves a mid-grey
         * behind near-black text. 0.80 across the content band clears WCAG AA against a backdrop of
         * any brightness, at the cost of showing less of the artwork - which is the unavoidable
         * price of putting dark text on someone else's image.
         *
         * Everything in the hero takes [onHero] - there is no second foreground role and no plate
         * behind the facts. What does not carry across is the *alpha*: chip and button edges tuned
         * as translucent white on a dark scrim vanish when the same figure is applied to near-black
         * on a white veil, which is why the light outline alphas here are two to three times their
         * dark counterparts rather than a mirror of them.
         *
         * Pure and non-composable so the contrast it promises can be asserted in a unit test.
         */
        fun forTheme(
            isDark: Boolean,
            colorScheme: ColorScheme,
        ): HeroColors =
            if (isDark) {
                HeroColors(
                    scrim = Color.Black,
                    onHero = Color.White,
                    ongoing = Color(HeroColorConstant.DARK_ONGOING),
                    chipSurface = Color.White.copy(alpha = HeroColorConstant.DARK_CHIP_SURFACE_ALPHA),
                    chipOutline = Color.White.copy(alpha = HeroColorConstant.DARK_CHIP_OUTLINE_ALPHA),
                    buttonSurface = Color.White.copy(alpha = HeroColorConstant.DARK_BUTTON_SURFACE_ALPHA),
                    buttonOutline = Color.White.copy(alpha = HeroColorConstant.DARK_BUTTON_OUTLINE_ALPHA),
                    topScrimAlpha = HeroColorConstant.DARK_TOP_SCRIM_ALPHA,
                    midScrimAlpha = HeroColorConstant.DARK_MID_SCRIM_ALPHA,
                    baseFadeAlpha = HeroColorConstant.DARK_BASE_FADE_ALPHA,
                )
            } else {
                HeroColors(
                    scrim = colorScheme.surface,
                    onHero = colorScheme.onSurface,
                    // The palette's own dark green: same "live" meaning, legible on a white veil.
                    ongoing = colorScheme.primary,
                    chipSurface = colorScheme.onSurface.copy(alpha = HeroColorConstant.LIGHT_CHIP_SURFACE_ALPHA),
                    chipOutline = colorScheme.onSurface.copy(alpha = HeroColorConstant.LIGHT_CHIP_OUTLINE_ALPHA),
                    buttonSurface = colorScheme.onSurface.copy(alpha = HeroColorConstant.LIGHT_BUTTON_SURFACE_ALPHA),
                    buttonOutline = colorScheme.onSurface.copy(alpha = HeroColorConstant.LIGHT_BUTTON_OUTLINE_ALPHA),
                    topScrimAlpha = HeroColorConstant.LIGHT_TOP_SCRIM_ALPHA,
                    midScrimAlpha = HeroColorConstant.LIGHT_MID_SCRIM_ALPHA,
                    baseFadeAlpha = HeroColorConstant.LIGHT_BASE_FADE_ALPHA,
                )
            }

        /**
         * The theme-independent set, for a hero treated as a photographic panel rather than as part
         * of the page.
         *
         * The rule it encodes: content painted directly onto the backdrop keeps fixed tokens in
         * both themes, and only content painted on the app's own surface follows the theme. A photo
         * does not get lighter when the page does, so neither does anything sitting on it.
         *
         * Currently the movie hero only - see `heroOnPhotoScrimBrush`. Note the cost this buys:
         * the scrim ends dark instead of dissolving into the page, so in light theme the hero meets
         * the content below it as a hard edge rather than a fade.
         */
        fun onPhoto(): HeroColors {
            val paper = Color(HeroColorConstant.PHOTO_PAPER)
            return HeroColors(
                scrim = Color(HeroColorConstant.PHOTO_SCRIM),
                onHero = paper,
                ongoing = Color(HeroColorConstant.DARK_ONGOING),
                chipSurface = paper.copy(alpha = HeroColorConstant.PHOTO_CHIP_SURFACE_ALPHA),
                chipOutline = paper.copy(alpha = HeroColorConstant.PHOTO_CHIP_OUTLINE_ALPHA),
                buttonSurface = paper.copy(alpha = HeroColorConstant.PHOTO_BUTTON_SURFACE_ALPHA),
                buttonOutline = paper.copy(alpha = HeroColorConstant.PHOTO_BUTTON_OUTLINE_ALPHA),
                topScrimAlpha = HeroColorConstant.PHOTO_TOP_SCRIM_ALPHA,
                midScrimAlpha = HeroColorConstant.PHOTO_MID_SCRIM_ALPHA,
                baseFadeAlpha = HeroColorConstant.PHOTO_BASE_FADE_ALPHA,
            )
        }
    }
}

/** [HeroColors] for the theme in effect at this point in the tree. */
@Composable
internal fun heroColors(): HeroColors = HeroColors.forTheme(LocalIsDarkTheme.current, MaterialTheme.colorScheme)
