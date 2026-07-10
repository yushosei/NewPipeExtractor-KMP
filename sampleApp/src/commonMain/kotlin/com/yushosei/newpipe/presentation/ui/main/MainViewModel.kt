package com.yushosei.newpipe.presentation.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yushosei.newpipe.extractor.InfoItem
import com.yushosei.newpipe.extractor.MediaFormat
import com.yushosei.newpipe.extractor.Page
import com.yushosei.newpipe.extractor.stream.StreamInfoItem
import com.yushosei.newpipe.player.MediaItem
import com.yushosei.newpipe.player.MediaPlayerController
import com.yushosei.newpipe.player.MediaPlayerListener
import com.yushosei.newpipe.player.MediaType
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

enum class PlaybackMode(val label: String) {
    AUDIO("Audio"),
    VIDEO("Video")
}

sealed interface SearchAction {
    data class QueryChange(val query: String = "") : SearchAction
    data object Search : SearchAction
    data object LoadMore : SearchAction
    data class LoadData(val item: StreamInfoItem) : SearchAction
    data class ServiceChange(val service: SearchServiceType) : SearchAction
    data class PlaybackModeChange(val mode: PlaybackMode) : SearchAction
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

    private val _playbackMode = MutableStateFlow(PlaybackMode.AUDIO)
    val playbackMode: StateFlow<PlaybackMode> = _playbackMode

    private val _currentMedia = MutableStateFlow<MediaItem?>(null)
    val currentMedia: StateFlow<MediaItem?> = _currentMedia

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError

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
                        play(action.item)
                    }

                    is SearchAction.ServiceChange -> {
                        changeService(action.service)
                    }

                    is SearchAction.PlaybackModeChange -> {
                        _playbackMode.value = action.mode
                        _playbackError.value = null
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

    private fun play(item: StreamInfoItem) = viewModelScope.launch {
        _playbackError.value = null
        try {
            val info = ExtractorHelper.getStreamInfo(item.serviceId, item.url)
            val mediaItem = when (_playbackMode.value) {
                PlaybackMode.AUDIO -> {
                    val stream = info.audioStreams.maxWithOrNull(
                        compareBy<com.yushosei.newpipe.extractor.stream.AudioStream> {
                            if (it.format == MediaFormat.M4A) 1 else 0
                        }.thenBy { it.averageBitrate }
                    )
                        ?: error("No audio stream is available for this item")
                    MediaItem(
                        title = info.name,
                        artist = info.uploaderName,
                        artworkUri = info.thumbnails.firstOrNull()?.url,
                        url = stream.content,
                        type = MediaType.AUDIO
                    )
                }

                PlaybackMode.VIDEO -> {
                    val stream = info.videoStreams
                        .filter { it.isUrl() }
                        .maxWithOrNull(
                            compareBy<com.yushosei.newpipe.extractor.stream.VideoStream> {
                                if (it.format == MediaFormat.MPEG_4) 1 else 0
                            }
                                .thenBy { it.height }
                                .thenBy { it.fps }
                                .thenBy { it.bitrate }
                        )
                    val videoUrl = stream?.content
                        ?: info.hlsUrl.takeIf { it.isNotEmpty() }
                        ?: error("No video stream with embedded audio is available for this item")
                    MediaItem(
                        title = info.name,
                        artist = info.uploaderName,
                        artworkUri = info.thumbnails.firstOrNull()?.url,
                        url = videoUrl,
                        type = MediaType.VIDEO
                    )
                }
            }

            _currentMedia.value = mediaItem
            mediaPlayerController.prepare(
                mediaItem,
                object : MediaPlayerListener {
                    override fun onReady() {
                        mediaPlayerController.start()
                    }

                    override fun onAudioCompleted() = Unit

                    override fun onError() {
                        _playbackError.value = "Playback failed for ${mediaItem.title}"
                    }
                }
            )
        } catch (e: Exception) {
            _playbackError.value = e.message ?: "Could not load the selected stream"
        }
    }

    private fun search() = viewModelScope.launch {
        _uiState.value = SearchUiState.Loading
        _playbackError.value = null
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
        } catch (e: Exception) {
            _uiState.value = SearchUiState.Error
            _playbackError.value = e.message ?: "Search failed on this platform"
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
            } catch (e: Exception) {
                _playbackError.value = e.message ?: "Could not load more results"
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
