/*
 * SSH Remote
 * Copyright (C) 2026  Stefan Sundin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.stefansundin.sshremote.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val ScrollbarThickness = 4.dp
private val ScrollbarPadding = 2.dp
private val ScrollbarMinThumbLength = 24.dp
private val ScrollbarGutterSize = ScrollbarThickness + (ScrollbarPadding * 2)

@Composable
internal fun ScrollbarContainer(
    modifier: Modifier = Modifier,
    verticalScrollState: ScrollState? = null,
    horizontalScrollState: ScrollState? = null,
    content: @Composable (Modifier) -> Unit,
) {
    val visibleVerticalScrollState = verticalScrollState?.takeIf { it.maxValue > 0 }
    val visibleHorizontalScrollState = horizontalScrollState?.takeIf { it.maxValue > 0 }
    val verticalGutter = visibleVerticalScrollState.hasScrollbar().gutterSize()
    val horizontalGutter = visibleHorizontalScrollState.hasScrollbar().gutterSize()

    Box(modifier = modifier) {
        content(
            Modifier.padding(
                end = verticalGutter,
                bottom = horizontalGutter,
            ),
        )

        visibleVerticalScrollState?.let { state ->
            VisualScrollbar(
                state = state,
                isVertical = true,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxHeight()
                    .padding(bottom = horizontalGutter),
            )
        }

        visibleHorizontalScrollState?.let { state ->
            VisualScrollbar(
                state = state,
                isVertical = false,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(end = verticalGutter),
            )
        }
    }
}

@Composable
private fun VisualScrollbar(
    state: ScrollState,
    isVertical: Boolean,
    modifier: Modifier = Modifier,
) {
    if (state.maxValue <= 0) return

    val color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)

    Canvas(
        modifier = if (isVertical) {
            modifier.width(ScrollbarGutterSize)
        } else {
            modifier.height(ScrollbarGutterSize)
        },
    ) {
        val thicknessPx = ScrollbarThickness.toPx()
        val paddingPx = ScrollbarPadding.toPx()
        val minThumbLengthPx = ScrollbarMinThumbLength.toPx()

        if (isVertical) {
            val trackLength = (size.height - (paddingPx * 2)).coerceAtLeast(0f)
            if (trackLength <= 0f) return@Canvas

            val thumbLength = calculateScrollbarThumbLength(trackLength, state, minThumbLengthPx)
            val thumbOffset = calculateScrollbarThumbOffset(trackLength, thumbLength, state)

            drawRoundRect(
                color = color,
                topLeft = Offset(size.width - paddingPx - thicknessPx, paddingPx + thumbOffset),
                size = Size(thicknessPx, thumbLength),
                cornerRadius = CornerRadius(thicknessPx / 2, thicknessPx / 2),
            )
        } else {
            val trackLength = (size.width - (paddingPx * 2)).coerceAtLeast(0f)
            if (trackLength <= 0f) return@Canvas

            val thumbLength = calculateScrollbarThumbLength(trackLength, state, minThumbLengthPx)
            val thumbOffset = calculateScrollbarThumbOffset(trackLength, thumbLength, state)

            drawRoundRect(
                color = color,
                topLeft = Offset(paddingPx + thumbOffset, size.height - paddingPx - thicknessPx),
                size = Size(thumbLength, thicknessPx),
                cornerRadius = CornerRadius(thicknessPx / 2, thicknessPx / 2),
            )
        }
    }
}

private fun calculateScrollbarThumbLength(
    trackLength: Float,
    scrollState: ScrollState,
    minThumbLength: Float,
): Float {
    val contentLength = trackLength + scrollState.maxValue
    return (trackLength * trackLength / contentLength)
        .coerceIn(minThumbLength, trackLength)
}

private fun calculateScrollbarThumbOffset(
    trackLength: Float,
    thumbLength: Float,
    scrollState: ScrollState,
): Float {
    if (scrollState.maxValue == 0) return 0f

    val scrollRange = (trackLength - thumbLength).coerceAtLeast(0f)
    return scrollRange * (scrollState.value / scrollState.maxValue.toFloat())
}

private fun Boolean.gutterSize(): Dp = if (this) ScrollbarGutterSize else 0.dp

private fun ScrollState?.hasScrollbar(): Boolean = this != null
