package com.yushosei.newpipe.ktx

import androidx.compose.foundation.lazy.LazyListState

fun LazyListState.isScrolledToEnd() =
    layoutInfo.visibleItemsInfo.lastOrNull()?.index == layoutInfo.totalItemsCount - 1