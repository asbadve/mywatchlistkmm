package com.ajinkyabadve.kmmmywatchlist.features.person.screen.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver
import com.ajinkyabadve.kmmmywatchlist.core.format.toOneDecimalString
import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonCredit
import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonDetail
import com.ajinkyabadve.kmmmywatchlist.features.person.model.firstFilmYear
import com.ajinkyabadve.kmmmywatchlist.features.person.model.heroBackdropCredit
import com.ajinkyabadve.kmmmywatchlist.features.person.model.yearsBetween
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.baseline_person_24
import mywatchlist.composeapp.generated.resources.person_also_known_as
import mywatchlist.composeapp.generated.resources.person_born
import mywatchlist.composeapp.generated.resources.person_born_with_age
import mywatchlist.composeapp.generated.resources.person_died
import mywatchlist.composeapp.generated.resources.person_died_with_age
import mywatchlist.composeapp.generated.resources.person_in_place
import mywatchlist.composeapp.generated.resources.person_stat_first_film
import mywatchlist.composeapp.generated.resources.person_stat_known_credits
import mywatchlist.composeapp.generated.resources.person_stat_popularity
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private object PersonHeroConstant {
    // Width-relative like the movie and TV heroes, rather than a fixed dp - a fixed height keeps
    // shrinking in proportion as the screen gets wider, which is how this banner ended up looking
    // stunted next to them. 8:5 puts it at half the height of those heroes at any width, which is the
    // ratio the designs call for: the person banner only backs an avatar, while the movie and TV heroes
    // carry a title, meta row and buttons over the artwork and need the room.
    const val BANNER_ASPECT_RATIO = 8 / 5f
    const val AVATAR_SIZE_DP = 96
    const val AVATAR_TARGET_WIDTH_DP = 140
    const val ROLE_SEPARATOR = " · "
    const val ROLE_TEXT_ALPHA = 0.6f
    const val VITALS_TEXT_ALPHA = 0.7f
    const val ALIAS_TEXT_ALPHA = 0.45f
    const val STAT_LABEL_ALPHA = 0.55f
    const val STAT_BORDER_ALPHA = 0.12f
    const val MAX_ALIAS_LINES = 2
}

/**
 * Person header built on the same hero system as the movie screen: backdrop behind, identity in
 * front, then a strip of the numbers worth knowing at a glance.
 *
 * The avatar is circular and overlaps the bottom edge of the banner - it is a portrait of a person,
 * not cover art, and the circle plus the overlap say that without needing a label. It sits left
 * rather than centred so the name can run alongside it and long names still get the full width.
 */
@Composable
fun PersonHeroSection(
    person: PersonDetail,
    onCreditClicked: (PersonCredit) -> Unit,
) {
    val heroCredit = person.heroBackdropCredit()

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(PersonHeroConstant.BANNER_ASPECT_RATIO)) {
            heroCredit?.let { credit ->
                PersonHeroBanner(
                    credit = credit,
                    onCreditClicked = onCreditClicked,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            PersonAvatar(
                person = person,
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp)
                        .offset(y = (PersonHeroConstant.AVATAR_SIZE_DP / 2).dp),
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = (PersonHeroConstant.AVATAR_SIZE_DP / 2 + 12).dp),
        ) {
            Text(
                text = person.name,
                fontSize = 26.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            val role =
                listOfNotNull(person.knownForDepartment, person.genderLabel)
                    .filter { it.isNotEmpty() }
                    .joinToString(PersonHeroConstant.ROLE_SEPARATOR)
            if (role.isNotEmpty()) {
                Text(
                    text = role,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = PersonHeroConstant.ROLE_TEXT_ALPHA),
                    modifier = Modifier.padding(top = 3.dp),
                )
            }

            PersonVitals(person = person, modifier = Modifier.padding(top = 12.dp))
            PersonStatStrip(person = person, modifier = Modifier.padding(top = 16.dp))
        }
    }
}

@Composable
private fun PersonAvatar(
    person: PersonDetail,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current.density
    val profileUrl =
        ImageConfigResolver.resolve(
            path = person.profilePath,
            type = ImageConfigResolver.ImageType.PROFILE,
            targetWidthDp = PersonHeroConstant.AVATAR_TARGET_WIDTH_DP,
            density = density,
        )
    val fallbackPainter = painterResource(Res.drawable.baseline_person_24)

    Box(
        modifier =
            modifier
                .size(PersonHeroConstant.AVATAR_SIZE_DP.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                // A ring in the page background colour, so the avatar reads as sitting in front of
                // the banner rather than punched out of it.
                .border(3.dp, MaterialTheme.colorScheme.background, CircleShape),
    ) {
        Image(
            painter =
                rememberAsyncImagePainter(
                    model = profileUrl,
                    filterQuality = FilterQuality.Medium,
                    error = fallbackPainter,
                    fallback = fallbackPainter,
                ),
            contentDescription = person.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

/** Born/died as prose, which reads more naturally than a table; aliases demoted below them. */
@Composable
private fun PersonVitals(
    person: PersonDetail,
    modifier: Modifier = Modifier,
) {
    val deathday = person.deathday?.takeIf { it.isNotEmpty() }

    Column(modifier = modifier) {
        person.birthday?.takeIf { it.isNotEmpty() }?.let { birthday ->
            // Age while alive only - for someone who has died, the age at death belongs on that line.
            val age =
                if (deathday == null) {
                    yearsBetween(birthday, Clock.System.todayIn(TimeZone.currentSystemDefault()).toString())
                } else {
                    null
                }
            val born =
                if (age != null) {
                    stringResource(Res.string.person_born_with_age, birthday, age)
                } else {
                    stringResource(Res.string.person_born, birthday)
                }
            val place = person.placeOfBirth?.takeIf { it.isNotEmpty() }
            Text(
                text = if (place != null) "$born ${stringResource(Res.string.person_in_place, place)}" else born,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = PersonHeroConstant.VITALS_TEXT_ALPHA),
            )
        }
        deathday?.let {
            val ageAtDeath = yearsBetween(person.birthday, it)
            Text(
                text =
                    if (ageAtDeath != null) {
                        stringResource(Res.string.person_died_with_age, it, ageAtDeath)
                    } else {
                        stringResource(Res.string.person_died, it)
                    },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = PersonHeroConstant.VITALS_TEXT_ALPHA),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (person.alsoKnownAs.isNotEmpty()) {
            Text(
                text = stringResource(Res.string.person_also_known_as, person.alsoKnownAs.joinToString(", ")),
                fontSize = 11.5.sp,
                maxLines = PersonHeroConstant.MAX_ALIAS_LINES,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = PersonHeroConstant.ALIAS_TEXT_ALPHA),
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/**
 * The person-screen counterpart of the movie hero's rating/runtime row - same idea, different
 * numbers, so the two detail screens read as one system.
 */
@Composable
private fun PersonStatStrip(
    person: PersonDetail,
    modifier: Modifier = Modifier,
) {
    val stats =
        buildList {
            person.knownCreditsCount.takeIf { it > 0 }?.let {
                add(it.toString() to stringResource(Res.string.person_stat_known_credits))
            }
            person.popularity.takeIf { it > 0.0 }?.let {
                add(it.toOneDecimalString() to stringResource(Res.string.person_stat_popularity))
            }
            person.firstFilmYear()?.let {
                add(it.toString() to stringResource(Res.string.person_stat_first_film))
            }
        }
    if (stats.isEmpty()) return

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = PersonHeroConstant.STAT_BORDER_ALPHA),
                    shape = RoundedCornerShape(10.dp),
                ).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        stats.forEachIndexed { index, (value, label) ->
            if (index > 0) {
                VerticalDivider(
                    modifier = Modifier.height(28.dp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = PersonHeroConstant.STAT_BORDER_ALPHA),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = label.uppercase(),
                    fontSize = 9.5.sp,
                    letterSpacing = 0.7.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = PersonHeroConstant.STAT_LABEL_ALPHA),
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}
