package com.ajinkyabadve.kmmmywatchlist.features.person.screen.detail

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.ajinkyabadve.kmmmywatchlist.features.person.PersonTestConstant
import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonCredit
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class PersonHeroBannerUiTest {
    private val credit =
        PersonCredit(
            id = CREDIT_ID,
            mediaType = PersonTestConstant.MEDIA_TYPE_MOVIE,
            title = CREDIT_TITLE,
            backdropPath = "/iconic.jpg",
        )

    /**
     * The backdrop belongs to a title, not to the person, so the banner has to say whose artwork it
     * is. Without this it reads as a decorative stock image and quietly misattributes the work.
     */
    @Test
    fun testNamesTheTitleTheBackdropCameFrom() =
        runComposeUiTest {
            setContent {
                PersonHeroBanner(
                    credit = credit,
                    onCreditClicked = {},
                    modifier = Modifier.size(width = 400.dp, height = 200.dp),
                )
            }

            onNodeWithText(ATTRIBUTION).assertExists()
        }

    /** The attribution doubles as a way into that title. */
    @Test
    fun testAttributionOpensTheCredit() =
        runComposeUiTest {
            var opened: PersonCredit? = null
            setContent {
                PersonHeroBanner(
                    credit = credit,
                    onCreditClicked = { opened = it },
                    modifier = Modifier.size(width = 400.dp, height = 200.dp),
                )
            }

            onNodeWithText(ATTRIBUTION).performClick()

            assertEquals(CREDIT_ID, opened?.id)
        }

    private companion object {
        const val CREDIT_ID = 42L
        const val CREDIT_TITLE = "The Career-Defining Film"

        /** What `Res.string.person_hero_known_for` renders for [CREDIT_TITLE]. */
        const val ATTRIBUTION = "From $CREDIT_TITLE"
    }
}
