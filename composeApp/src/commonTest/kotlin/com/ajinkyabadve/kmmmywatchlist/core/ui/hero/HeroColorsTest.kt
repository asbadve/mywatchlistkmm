package com.ajinkyabadve.kmmmywatchlist.core.ui.hero

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.ajinkyabadve.kmmmywatchlist.theme.md_theme_dark_background
import com.ajinkyabadve.kmmmywatchlist.theme.md_theme_dark_onSurface
import com.ajinkyabadve.kmmmywatchlist.theme.md_theme_dark_primary
import com.ajinkyabadve.kmmmywatchlist.theme.md_theme_dark_surface
import com.ajinkyabadve.kmmmywatchlist.theme.md_theme_light_background
import com.ajinkyabadve.kmmmywatchlist.theme.md_theme_light_onSurface
import com.ajinkyabadve.kmmmywatchlist.theme.md_theme_light_primary
import com.ajinkyabadve.kmmmywatchlist.theme.md_theme_light_surface
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The numeric guard on the light-theme hero.
 *
 * Every foreground in the hero used to be a hardcoded white drawn over a gradient whose lower half
 * resolves to the page background. In dark theme that background is near-black and the text reads;
 * in light theme it is near-white, so the facts row, trailer button, network line and provider chips
 * were white-on-white. Asserting contrast is the only way to see that - node-existence assertions
 * pass happily on invisible text.
 */
class HeroColorsTest {
    /**
     * The bug itself, stated as a number. The bottom of the scrim *is* the page background, so a
     * foreground that does not contrast with the background is a foreground that vanishes there.
     */
    @Test
    fun testForegroundContrastsWithTheBackgroundTheScrimFadesInto() {
        val light = HeroColors.forTheme(isDark = false, colorScheme = LIGHT_SCHEME)
        val dark = HeroColors.forTheme(isDark = true, colorScheme = DARK_SCHEME)

        assertTrue(
            contrastRatio(light.onHero, md_theme_light_background) >= HeroContrastConstant.AA_NORMAL_TEXT,
            "Light hero foreground must survive the scrim's fade to the page background",
        )
        assertTrue(
            contrastRatio(dark.onHero, md_theme_dark_background) >= HeroContrastConstant.AA_NORMAL_TEXT,
            "Dark hero foreground must survive the scrim's fade to the page background",
        )
    }

    /**
     * The content band sits over artwork this app does not control, so the guarantee has to hold for
     * the extremes of it. Large-text AA rather than normal-text AA is the honest bar here: a
     * translucent scrim over an arbitrary image cannot promise 4.5:1 without hiding the image
     * entirely, and dark theme has always sat around 3.4:1 against a white frame.
     */
    @Test
    fun testForegroundContrastsWithTheContentBandOverAnyBackdrop() {
        listOf(
            HeroColors.forTheme(isDark = false, colorScheme = LIGHT_SCHEME),
            HeroColors.forTheme(isDark = true, colorScheme = DARK_SCHEME),
        ).forEach { colors ->
            listOf(Color.Black, Color.White).forEach { backdrop ->
                val band = scrimOver(colors.scrim, colors.midScrimAlpha, backdrop)
                assertTrue(
                    contrastRatio(colors.onHero, band) >= HeroContrastConstant.AA_LARGE_TEXT,
                    "Hero foreground unreadable on the content band over a $backdrop backdrop",
                )
            }
        }
    }

    /**
     * The "still running" badge is the one hero colour that is neither foreground nor scrim. Its
     * dark-theme mint is far too pale for a white veil, so light theme takes the palette's own
     * green instead - and that substitution is only worth anything if it actually reads.
     */
    @Test
    fun testOngoingBadgeReadsOnTheContentBandInBothThemes() {
        listOf(
            HeroColors.forTheme(isDark = false, colorScheme = LIGHT_SCHEME),
            HeroColors.forTheme(isDark = true, colorScheme = DARK_SCHEME),
        ).forEach { colors ->
            val band = scrimOver(colors.scrim, colors.midScrimAlpha, Color.Black)
            assertTrue(
                contrastRatio(colors.ongoing, band) >= HeroContrastConstant.AA_LARGE_TEXT,
                "Ongoing badge unreadable on the content band",
            )
        }
    }

    /**
     * Chips and buttons are outlined rather than filled, so the outline is the whole affordance -
     * and it is the part that does not survive being mirrored. A white edge at 16% reads as a lit
     * rim on a black scrim; near-black at 16% on a white veil is nothing at all, which is how the
     * provider chips and the trailer button came to look like empty space in light theme.
     */
    @Test
    fun testLightThemeOutlinesAreStrongerThanTheirDarkCounterparts() {
        val light = HeroColors.forTheme(isDark = false, colorScheme = LIGHT_SCHEME)
        val dark = HeroColors.forTheme(isDark = true, colorScheme = DARK_SCHEME)

        assertTrue(light.chipOutline.alpha > dark.chipOutline.alpha, "Light chip outline must not mirror the dark one")
        assertTrue(light.buttonOutline.alpha > dark.buttonOutline.alpha, "Light button outline must not mirror the dark one")
    }

    /**
     * The on-photo set is the movie hero's, and it answers to a stricter bar than the theme-aware
     * one: its scrim does not fade to the page, so the colour behind the content is known rather
     * than inherited. Nothing here may depend on the theme - that is the whole premise.
     */
    @Test
    fun testOnPhotoColoursReadAgainstTheirOwnScrim() {
        val photo = HeroColors.onPhoto()

        // The mid band is the title's zone - 34sp ExtraBold, so large-text AA is the bar it answers
        // to. Against a white frame it lands near 4:1, which is why this is not the stricter figure.
        listOf(Color.Black, Color.White).forEach { backdrop ->
            val band = scrimOver(photo.scrim, photo.midScrimAlpha, backdrop)
            assertTrue(
                contrastRatio(photo.onHero, band) >= HeroContrastConstant.AA_LARGE_TEXT,
                "On-photo title unreadable on its own scrim over a $backdrop backdrop",
            )
        }

        // The lower band, where the facts and buttons sit, is denser - and that small print does
        // have to clear normal-text AA, against the worst backdrop the scrim can be handed.
        val lowerBand = scrimOver(photo.scrim, photo.baseFadeAlpha, Color.White)
        assertTrue(
            contrastRatio(photo.onHero, lowerBand) >= HeroContrastConstant.AA_NORMAL_TEXT,
            "On-photo foreground unreadable where the content sits",
        )
    }

    /**
     * Dark theme was never broken, so this change must not have moved it. Pins the exact values the
     * hero shipped with rather than a property, because "unchanged" is the whole claim.
     */
    @Test
    fun testDarkThemeKeepsItsOriginalScrimAndForeground() {
        val dark = HeroColors.forTheme(isDark = true, colorScheme = DARK_SCHEME)

        assertEquals(Color.Black, dark.scrim)
        assertEquals(Color.White, dark.onHero)
        assertEquals(Color(ORIGINAL_ONGOING_GREEN), dark.ongoing)
        assertEquals(ORIGINAL_TOP_SCRIM_ALPHA, dark.topScrimAlpha)
        assertEquals(ORIGINAL_MID_SCRIM_ALPHA, dark.midScrimAlpha)
        assertEquals(ORIGINAL_BASE_FADE_ALPHA, dark.baseFadeAlpha)
    }

    /** Light theme must not silently fall back to the dark tokens. */
    @Test
    fun testLightThemeTakesItsColoursFromTheScheme() {
        val light = HeroColors.forTheme(isDark = false, colorScheme = LIGHT_SCHEME)

        assertEquals(md_theme_light_surface, light.scrim)
        assertEquals(md_theme_light_onSurface, light.onHero)
        assertEquals(md_theme_light_primary, light.ongoing)
        assertEquals(md_theme_light_onSurface.value, light.chipOutline.copy(alpha = 1f).value)
        assertTrue(light.midScrimAlpha > ORIGINAL_MID_SCRIM_ALPHA, "A light veil needs more opacity than a dark scrim")
    }

    private companion object {
        val LIGHT_SCHEME =
            lightColorScheme(
                background = md_theme_light_background,
                surface = md_theme_light_surface,
                onSurface = md_theme_light_onSurface,
                primary = md_theme_light_primary,
            )
        val DARK_SCHEME =
            darkColorScheme(
                background = md_theme_dark_background,
                surface = md_theme_dark_surface,
                onSurface = md_theme_dark_onSurface,
                primary = md_theme_dark_primary,
            )

        // Deliberately restated rather than imported: these are the pre-change values this test
        // exists to prove were not disturbed, and importing the constant under test would pass on
        // any edit to it.
        const val ORIGINAL_ONGOING_GREEN = 0xFF6FE0A0
        const val ORIGINAL_TOP_SCRIM_ALPHA = 0.55f
        const val ORIGINAL_MID_SCRIM_ALPHA = 0.45f
        const val ORIGINAL_BASE_FADE_ALPHA = 0.92f
    }
}
