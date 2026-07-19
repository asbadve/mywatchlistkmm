package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.CastMember
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.baseline_person_24
import org.jetbrains.compose.resources.painterResource

@Composable
fun CastSection(
    castList: List<CastMember>,
    title: String = "Cast & Crew",
    onPersonClicked: (Long) -> Unit = {},
) {
    if (castList.isNotEmpty()) {
        val lazyRowState = rememberLazyListState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp)
        ) {
            SectionHeaderWithScrollHint(
                title = title,
                listSize = castList.size.coerceAtMost(15),
                lazyRowState = lazyRowState,
                scrollStep = 3
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                state = lazyRowState,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(castList.take(15)) { member ->
                    CastMemberItem(member = member, onClick = { onPersonClicked(member.id.toLong()) })
                }
            }
        }
    }
}

@Composable
private fun CastMemberItem(member: CastMember, onClick: () -> Unit) {
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val profileUrl = com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver.resolve(
        path = member.profilePath,
        type = com.ajinkyabadve.kmmmywatchlist.core.ImageConfigResolver.ImageType.PROFILE,
        targetWidthDp = 70,
        density = density
    )
    val fallbackPainter = painterResource(Res.drawable.baseline_person_24)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp).clickable(onClick = onClick)
    ) {
        val painter = rememberAsyncImagePainter(
            model = profileUrl,
            filterQuality = FilterQuality.Medium,
            error = fallbackPainter,
            fallback = fallbackPainter
        )
        val painterState by painter.state.collectAsState()
        val contentScale = if (painterState is AsyncImagePainter.State.Success) {
            ContentScale.Crop
        } else {
            ContentScale.Fit
        }

        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Image(
                painter = painter,
                contentDescription = member.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = member.name,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = member.character,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
