package com.yushosei.newpipe.presentation.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.sharp.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
fun CoverImage(
    data: Any?,
    modifier: Modifier = Modifier,
    imageModifier: Modifier = Modifier,
    size: Dp = Dp.Unspecified,
    shape: Shape = MaterialTheme.shapes.small,
    icon: VectorPainter = rememberVectorPainter(Icons.Sharp.Refresh),
) {
    val context = LocalPlatformContext.current
    val request = remember(data) {
        ImageRequest.Builder(context)
            .data(data)
            .crossfade(true)
            .build()
    }

    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        placeholder = icon,
        error = icon,
        modifier = imageModifier
            .size(size)
            .clip(shape)
    )
}