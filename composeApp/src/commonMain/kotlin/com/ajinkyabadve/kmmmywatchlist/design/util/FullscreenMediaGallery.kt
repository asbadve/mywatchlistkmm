package com.ajinkyabadve.kmmmywatchlist.design.util

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import compose.icons.feathericons.ZoomIn
import compose.icons.feathericons.ZoomOut
import compose.icons.FeatherIcons
import compose.icons.feathericons.Download
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.rememberAsyncImagePainter

/**
 * Reusable full-screen media/photo gallery component supporting pinch-to-zoom gestures,
 * panning, and a custom action callback (e.g. download trigger).
 */
@OptIn(ExperimentalFoundationApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun FullscreenMediaGallery(
    images: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onDownload: ((imageUrl: String) -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = com.ajinkyabadve.kmmmywatchlist.getDialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        com.ajinkyabadve.kmmmywatchlist.ConfigureDialogWindow()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            val pagerState = rememberPagerState(initialPage = initialIndex) { images.size }
            var isZoomed by remember { mutableStateOf(false) }
            var activeScale by remember { mutableStateOf(1f) }
            var activeOffset by remember { mutableStateOf(Offset.Zero) }

            // Reset zoom on swipe away
            LaunchedEffect(pagerState.currentPage) {
                activeScale = 1f
                activeOffset = Offset.Zero
                isZoomed = false
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !isZoomed
            ) { page ->
                val imageUrl = images[page]

                // Pinch-to-zoom & Pan state (active page local or outer)
                val isCurrent = page == pagerState.currentPage
                val scale = if (isCurrent) activeScale else 1f
                val offset = if (isCurrent) activeOffset else Offset.Zero

                var hasMultiplePointers by remember { mutableStateOf(false) }

                val state = rememberTransformableState { zoomChange, offsetChange, _ ->
                    if (isCurrent) {
                        activeScale = (activeScale * zoomChange).coerceIn(1f, 5f)
                        isZoomed = activeScale > 1.02f
                        if (activeScale > 1.02f) {
                            activeOffset += offsetChange
                        } else {
                            activeOffset = Offset.Zero
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val activePointers = event.changes.filter { it.pressed }
                                    hasMultiplePointers = activePointers.size > 1
                                }
                            }
                        }
                        .let { modifier ->
                            if (isCurrent && scale > 1.02f) {
                                modifier.pointerInput(scale) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        activeOffset += dragAmount
                                    }
                                }
                            } else {
                                modifier
                            }
                        }
                        .let { modifier ->
                            if (isCurrent && com.ajinkyabadve.kmmmywatchlist.getPlatformName() == "Desktop") {
                                modifier.pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            if (event.type == PointerEventType.Scroll) {
                                                val isCtrlOrMeta = event.keyboardModifiers.isCtrlPressed || event.keyboardModifiers.isMetaPressed
                                                if (isCtrlOrMeta) {
                                                    val scrollDelta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                                    if (scrollDelta != 0f) {
                                                        val zoomFactor = if (scrollDelta > 0) 0.9f else 1.1f
                                                        activeScale = (activeScale * zoomFactor).coerceIn(1f, 5f)
                                                        isZoomed = activeScale > 1.02f
                                                        if (activeScale <= 1.02f) {
                                                            activeOffset = Offset.Zero
                                                        }
                                                        event.changes.forEach { it.consume() }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                modifier
                            }
                        }
                        .transformable(state = state, enabled = hasMultiplePointers || isZoomed)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val painter = rememberAsyncImagePainter(model = imageUrl)
                    Image(
                        painter = painter,
                        contentDescription = "Gallery Image $page",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // Top action bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Optional download button
                if (onDownload != null) {
                    IconButton(
                        onClick = { onDownload(images[pagerState.currentPage]) },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            imageVector = FeatherIcons.Download,
                            contentDescription = "Download Photo",
                            tint = Color.White
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }

                // Close button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close gallery",
                        tint = Color.White
                    )
                }
            }

            // Bottom pagination overlay (e.g. "3 / 10")
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${images.size}",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }

            // Zoom indicator (bottom-left guidance for accessibility and screen reader users)
            val zoomPercent = (activeScale * 100).toInt()
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 32.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Zoom: $zoomPercent%",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }

            // Zoom controls panel (floating on the right center)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Zoom In Button
                IconButton(
                    onClick = {
                        activeScale = (activeScale + 0.5f).coerceIn(1f, 5f)
                        isZoomed = activeScale > 1.02f
                    },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = FeatherIcons.ZoomIn,
                        contentDescription = "Zoom In (Current: $zoomPercent%)",
                        tint = Color.White
                    )
                }

                // Zoom Out Button
                IconButton(
                    onClick = {
                        activeScale = (activeScale - 0.5f).coerceIn(1f, 5f)
                        isZoomed = activeScale > 1.02f
                        if (activeScale <= 1.02f) {
                            activeScale = 1f
                            activeOffset = Offset.Zero
                        }
                    },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        imageVector = FeatherIcons.ZoomOut,
                        contentDescription = "Zoom Out (Current: $zoomPercent%)",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
