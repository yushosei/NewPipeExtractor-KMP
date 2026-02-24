package com.yushosei.newpipe.presentation.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yushosei.newpipe.extractor.InfoItem
import com.yushosei.newpipe.extractor.Page
import com.yushosei.newpipe.extractor.stream.StreamInfoItem
import com.yushosei.newpipe.player.MediaItem
import com.yushosei.newpipe.player.MediaPlayerController
import com.yushosei.newpipe.player.MediaPlayerListener
import com.yushosei.newpipe.util.ExtractorHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.dsl.module

val mainModule = module {
    factory {
        SearchViewModel(get())
    }
}

data class SearchServiceType(
    val serviceId: Int,
    val label: String,
    val contentFilters: List<String>,
)

sealed interface SearchAction {
    data class QueryChange(val query: String = "") : SearchAction
    data object Search : SearchAction
    data object LoadMore : SearchAction
    data class LoadData(val item: StreamInfoItem) : SearchAction
    data class ServiceChange(val service: SearchServiceType) : SearchAction
}

open class SearchUiState {
    object Empty : SearchUiState()
    object Loading : SearchUiState()
    object Loaded : SearchUiState()
    object Error : SearchUiState()
}

class SearchViewModel constructor(
    private val mediaPlayerController: MediaPlayerController,
) : ViewModel() {
    val serviceOptions = listOf(
        SearchServiceType(
            serviceId = 0,
            label = "YouTube",
            contentFilters = listOf("videos")
        ),
        SearchServiceType(
            serviceId = 1,
            label = "SoundCloud",
            contentFilters = listOf("tracks")
        ),
    )

    private val _selectedService = MutableStateFlow(serviceOptions.first())
    val selectedService: StateFlow<SearchServiceType> = _selectedService

    private var searchText = ""
    val initialSearchText = searchText

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions

    private var nextPage: Page? = null

    private val _result = MutableStateFlow<List<InfoItem?>>(emptyList())
    val result: StateFlow<List<InfoItem?>> = _result

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Empty)
    val uiState: StateFlow<SearchUiState> = _uiState

    private val currentSearchTextIntent = Channel<String>(Channel.UNLIMITED)

    private val pendingActions = MutableSharedFlow<SearchAction>()

    private var searchJob: Job = Job()

    init {
        viewModelScope.launch {
            pendingActions.collectLatest { action ->
                when (action) {
                    is SearchAction.QueryChange -> {
                        updateSuggestions(action.query)
                    }

                    is SearchAction.Search -> {
                        search()
                    }

                    is SearchAction.LoadMore -> {
                        loadMore()
                    }

                    is SearchAction.LoadData -> {
                        playAudio(action.item)
                    }

                    is SearchAction.ServiceChange -> {
                        changeService(action.service)
                    }
                }
            }
        }

        viewModelScope.launch {
            withContext(Dispatchers.Main + searchJob) {
                currentSearchTextIntent.consumeAsFlow().debounce(120).collectLatest {
                    loadSuggestions()
                }
            }
        }

        if (searchText.isBlank()) updateSuggestions("")
        else search()
    }

    private fun playAudio(item: StreamInfoItem) = viewModelScope.launch {
        ExtractorHelper.getStreamInfo(item.serviceId, item.url).let { info ->
            val mediaItem = MediaItem(
                title = info.name,
                artist = info.uploaderName,
                artworkUri = info.thumbnails.first().url,
                url = info.audioStreams.first().content,
            )

            mediaPlayerController.prepare(
                mediaItem, object : MediaPlayerListener {
                    override fun onReady() {
                        mediaPlayerController.start()
                    }

                    override fun onAudioCompleted() {

                    }

                    override fun onError() {

                    }
                })
        }
    }

    private fun search() = viewModelScope.launch {
        _uiState.value = SearchUiState.Loading
        try {
            val service = _selectedService.value
            val searchResult = ExtractorHelper.searchFor(
                service.serviceId,
                searchText,
                service.contentFilters,
                "",
            )
            nextPage = searchResult.nextPage
            _result.value = searchResult.relatedItems
            _uiState.value = SearchUiState.Loaded
        } catch (_: Exception) {

        }
    }

    private fun loadMore() = viewModelScope.launch {
        nextPage?.let { page ->
            try {
                val service = _selectedService.value
                val searchResult = ExtractorHelper.getMoreSearchItems(
                    service.serviceId, searchText, service.contentFilters, "", page
                )

                nextPage = searchResult.nextPage
                _result.value += searchResult.items
            } catch (_: Exception) {

            }
        }
    }

    private fun updateSuggestions(text: String) = viewModelScope.launch {
        searchText = text
        currentSearchTextIntent.send(text)
    }

    private fun loadSuggestions() = viewModelScope.launch {
        try {
            val serviceId = _selectedService.value.serviceId
            val apiSuggestions = withContext(Dispatchers.Main) {
                ExtractorHelper.suggestionsFor(serviceId, searchText)
            }.map { suggestion ->
                suggestion
            }
            _suggestions.value = apiSuggestions
        } catch (e: Exception) {
        }
    }

    private fun changeService(service: SearchServiceType) = viewModelScope.launch {
        if (_selectedService.value.serviceId == service.serviceId) {
            return@launch
        }

        _selectedService.value = service
        nextPage = null
        _result.value = emptyList()

        if (searchText.isBlank()) {
            _uiState.value = SearchUiState.Empty
            _suggestions.value = emptyList()
            return@launch
        }

        updateSuggestions(searchText)
        search()
    }

    fun handleAction(action: SearchAction) = viewModelScope.launch {
        pendingActions.emit(action)
    }
}
