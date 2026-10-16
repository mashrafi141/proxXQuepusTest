package com.example.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Vertical Reels/Shorts-style Paged Media Container:
 * Directly driven by Jetpack Compose's native hardware-accelerated VerticalPager and PagerState.
 * Prevents any recursive state update or feedback loop so scrolling is 100% stable, smooth,
 * and snaps cleanly one page at a time without runaway multi-page flings.
 */
@Composable
fun VerticalReelMediaContainer(
    pagerState: PagerState,
    isZoomed: Boolean,
    modifier: Modifier = Modifier,
    renderItem: @Composable (index: Int) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        VerticalPager(
            state = pagerState,
            userScrollEnabled = !isZoomed,
            beyondViewportPageCount = 1,
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                snapAnimationSpec = tween(durationMillis = 280)
            ),
            modifier = Modifier.fillMaxSize()
        ) { page ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                renderItem(page)
            }
        }
    }
}
