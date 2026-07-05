package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun SectionHeaderWithScrollHint(
    title: String,
    listSize: Int,
    lazyRowState: LazyListState,
    scrollStep: Int = 3
) {
    val coroutineScope = rememberCoroutineScope()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (listSize > scrollStep) {
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        val nextIndex = (lazyRowState.firstVisibleItemIndex + scrollStep).coerceAtMost(listSize - 1)
                        lazyRowState.animateScrollToItem(nextIndex)
                    }
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Scroll $title Right",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

fun formatFullReleaseDate(rawDate: String): String {
    if (rawDate.length != 10) return rawDate
    val parts = rawDate.split("-")
    if (parts.size != 3) return rawDate
    val year = parts[0]
    val month = when (parts[1]) {
        "01" -> "Jan"
        "02" -> "Feb"
        "03" -> "Mar"
        "04" -> "Apr"
        "05" -> "May"
        "06" -> "Jun"
        "07" -> "Jul"
        "08" -> "Aug"
        "09" -> "Sep"
        "10" -> "Oct"
        "11" -> "Nov"
        "12" -> "Dec"
        else -> parts[1]
    }
    val day = parts[2].toIntOrNull()?.toString() ?: parts[2]
    return "$month $day, $year"
}

fun formatRuntime(minutes: Int?): String {
    if (minutes == null || minutes <= 0) return ""
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    return if (hours > 0) {
        "${hours}h ${remainingMinutes}m"
    } else {
        "${minutes}m"
    }
}

fun formatBudget(budget: Long?): String {
    if (budget == null || budget <= 0) return ""
    return if (budget >= 1_000_000) {
        "$${budget / 1_000_000}M"
    } else {
        "$$budget"
    }
}
