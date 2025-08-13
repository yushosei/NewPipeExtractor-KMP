package com.yushosei.newpipe.presentation.ui.main

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.yushosei.newpipe.extractor.stream.StreamInfoItem
import com.yushosei.newpipe.ktx.interpunctize
import com.yushosei.newpipe.ktx.isScrolledToEnd
import com.yushosei.newpipe.ktx.millisToDuration
import com.yushosei.newpipe.presentation.ui.component.CoverImage
import com.yushosei.newpipe.presentation.ui.component.SearchTextField
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MainScreen(
    viewModel: SearchViewModel = koinViewModel(),
    listState: LazyListState = rememberLazyListState(),
) {
    val searchBarHideThreshold = 1
    val searchBarHeight = 200.dp
    val searchBarVisibility = remember { Animatable(0f) }
    val suggestions by viewModel.suggestions.collectAsState()
    val result by viewModel.result.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val endOfListReached by remember {
        derivedStateOf {
            listState.isScrolledToEnd()
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is SearchUiState.Loaded) {
            listState.animateScrollToItem(0)
        }
    }

    // hide search bar when scrolling after some scroll
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .debounce(30)
            .distinctUntilChanged()
            .map { if (listState.firstVisibleItemIndex > searchBarHideThreshold) it else false }
            .map { if (it) 1f else 0f }
            .collectLatest { searchBarVisibility.animateTo(it) }
    }

    LaunchedEffect(endOfListReached) {
        viewModel.handleAction(SearchAction.LoadMore)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
        //.bottomNavigationPadding()
    ) {
        Box(modifier = Modifier.zIndex(0f)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
            ) {
                val items = result.mapNotNull { item ->
                    if (item is StreamInfoItem)
                        item
                    else
                        null
                }

                searchResult(items, onClickItem = {
                    viewModel.handleAction(SearchAction.LoadData(it))
                })
            }

            SearchAppBar(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = 1 - searchBarVisibility.value
                        translationY = searchBarHeight.value * (-searchBarVisibility.value)
                    },
                onQueryChange = { viewModel.handleAction(SearchAction.QueryChange(it)) },
                onSearch = { viewModel.handleAction(SearchAction.Search) },
                suggestions = suggestions,
            )
        }
    }

    if (uiState is SearchUiState.Loading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(3f),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = Color(0xFF00CA4E))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.searchResult(
    items: List<StreamInfoItem>,
    onClickItem: (StreamInfoItem) -> Unit
) {
    items(items) { item ->
        SearchRow(
            audio = item,
            imageSize = 40.dp,
            modifier = Modifier.animateItem(),
            onClick = {
                onClickItem(item)
            }
        )
    }
}

@Composable
fun SearchRow(
    audio: StreamInfoItem,
    imageSize: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp).clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CoverImage(
            data = audio.thumbnails.firstOrNull()?.url ?: "",
            size = imageSize,
            imageModifier = Modifier
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = audio.name,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            val infoLine = listOfNotNull(
                audio.uploaderName,
                (audio.duration * 1000).millisToDuration()
            ).interpunctize()

            Text(
                text = infoLine,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


@Composable
@OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
private fun SearchAppBar(
    modifier: Modifier = Modifier,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    initialQuery: String = "",
    onRemoveHistory: (String) -> Unit = {},
    suggestions: List<String> = emptyList(),
    focusManager: FocusManager = LocalFocusManager.current,
    windowInfo: WindowInfo = LocalWindowInfo.current,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(suggestions) {
        listState.scrollToItem(0)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize()
            .padding(4.dp)
    ) {
        val keyboardController = LocalSoftwareKeyboardController.current
        val hasWindowFocus = windowInfo.isWindowFocused

        var focused by remember { mutableStateOf(false) }
        val searchActive = focused && hasWindowFocus //&& isKeyboardVisible

        val triggerSearch = {
            onSearch()
            keyboardController?.hide()
            focusManager.clearFocus()
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            var query by rememberSaveable { mutableStateOf(initialQuery) }
            SearchTextField(
                autoFocus = query.isEmpty(),
                value = query,
                onValueChange = { value ->
                    query = value
                    onQueryChange(value)
                },
                onSearch = { triggerSearch() },
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .onFocusChanged {
                        focused = it.isFocused
                    }
            )
            if (searchActive && suggestions.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .wrapContentSize().background(Color.White)
                    //.customImePadding()
                ) {
                    item {
                        SuggestionTitle(isSearching = query.isNotBlank())
                    }

                    items(suggestions) { suggestion ->
                        SuggestionItem(
                            suggestion = suggestion,
                            onClick = {
                                query = suggestion
                                onQueryChange(query)
                                triggerSearch()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionTitle(isSearching: Boolean) {
    Text(
        text = "Result",
        modifier = Modifier
            .padding(
                horizontal = 8.dp,
                vertical = 4.dp
            )
    )
}

@Composable
private fun SuggestionItem(
    suggestion: String,
    onClick: (String) -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clickable {
                onClick(suggestion)
            }
            .padding(
                horizontal = 8.dp,
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier,
                imageVector = Icons.Default.Search,
                contentDescription = null,
            )

            Text(
                text = suggestion,
                modifier = Modifier
                    .padding(
                        start = 8.dp,
                        end = 8.dp
                    )
                    .weight(1f),
                textAlign = TextAlign.Start,
            )
        }

        Divider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        )
    }
}