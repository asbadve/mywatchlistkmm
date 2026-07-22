package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.VideoResult
import com.ajinkyabadve.kmmmywatchlist.openUrl
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.section_trailers_clips
import org.jetbrains.compose.resources.stringResource

@Composable
fun VideoClipsSection(videos: List<VideoResult>) {
    val clips = videos.filter { it.site.lowercase() == "youtube" }
    if (clips.isNotEmpty()) {
        val lazyRowState = rememberLazyListState()
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
        ) {
            SectionHeaderWithScrollHint(
                title = stringResource(Res.string.section_trailers_clips),
                listSize = clips.size.coerceAtMost(5),
                lazyRowState = lazyRowState,
                scrollStep = 2,
            )
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                state = lazyRowState,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(clips.take(5)) { video ->
                    Card(
                        modifier =
                            Modifier
                                .width(180.dp)
                                .aspectRatio(16 / 9f)
                                .clickable {
                                    openUrl("https://www.youtube.com/watch?v=${video.key}")
                                },
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            val thumbUrl = "https://img.youtube.com/vi/${video.key}/hqdefault.jpg"
                            val painter = rememberAsyncImagePainter(model = thumbUrl)
                            Image(
                                painter = painter,
                                contentDescription = video.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.3f)),
                            )
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Video",
                                modifier =
                                    Modifier
                                        .align(Alignment.Center)
                                        .size(36.dp),
                                tint = Color.White,
                            )
                            Text(
                                text = video.name,
                                color = Color.White,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier =
                                    Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
