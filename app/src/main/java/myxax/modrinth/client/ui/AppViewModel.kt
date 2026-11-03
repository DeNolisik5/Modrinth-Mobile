package myxax.modrinth.client.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import myxax.modrinth.client.data.ContentType
import myxax.modrinth.client.data.ModDownloader
import myxax.modrinth.client.data.ModHit
import myxax.modrinth.client.data.ModVersion
import myxax.modrinth.client.data.ModrinthApi
import myxax.modrinth.client.data.SearchFilters

/** Состояние скачивания конкретной версии. */
sealed interface DownloadState {
    data class Running(val progress: Float) : DownloadState
    data class Done(val path: String) : DownloadState
    data class Failed(val message: String) : DownloadState
}

data class ModrinthUiState(
    val query: String = "",
    val tab: ContentType = ContentType.MOD,
    val filters: SearchFilters = SearchFilters(),

    /** Версии Minecraft из API Modrinth; до загрузки — запасной список. */
    val availableVersions: List<String> = AppViewModel.GAME_VERSIONS,

    val results: List<ModHit> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false,
    val error: String? = null,

    val filtersOpen: Boolean = false,

    val selected: ModHit? = null,
    val versions: List<ModVersion> = emptyList(),
    val versionsLoading: Boolean = false,
    val versionsError: String? = null,

    val downloads: Map<String, DownloadState> = emptyMap(),
    val message: String? = null
)

class AppViewModel : ViewModel() {

    private val _state = MutableStateFlow(ModrinthUiState())
    val state: StateFlow<ModrinthUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    /**
     * Номер «поколения» поиска. Растёт при каждом сбросе (смена вкладки,
     * фильтров или запроса) и защищает от гонки: ответ старого запроса,
     * пришедший позже, просто игнорируется.
     */
    private var generation = 0

    init {
        restartSearch()
        loadGameVersions()
    }

    /** Тянем справочник версий Minecraft из API, чтобы не хардкодить его. */
    private fun loadGameVersions() {
        viewModelScope.launch {
            try {
                val list = ModrinthApi.gameVersions()
                if (list.isNotEmpty()) {
                    _state.update { it.copy(availableVersions = list) }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                // Не критично: остаётся запасной список GAME_VERSIONS.
            }
        }
    }

    // ---------------------------------------------------------------- поиск

    fun updateQuery(value: String) {
        _state.update { it.copy(query = value) }
        restartSearch(debounceMillis = 400)
    }

    fun selectTab(tab: ContentType) {
        if (_state.value.tab == tab) return
        _state.update { it.copy(tab = tab) }
        restartSearch()
    }

    fun setGameVersion(version: String?) {
        val current = _state.value.filters
        if (current.gameVersion == version) return
        _state.update { it.copy(filters = current.copy(gameVersion = version)) }
        restartSearch()
    }

    fun openFilters() = _state.update { it.copy(filtersOpen = true) }

    fun closeFilters() = _state.update { it.copy(filtersOpen = false) }

    fun applyFilters(filters: SearchFilters) {
        _state.update { it.copy(filters = filters, filtersOpen = false) }
        restartSearch()
    }

    fun retry() = restartSearch()

    private fun restartSearch(debounceMillis: Long = 0L) {
        generation++
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounceMillis > 0) delay(debounceMillis)
            _state.update {
                it.copy(
                    isLoading = true,
                    isLoadingMore = false,
                    error = null,
                    results = emptyList(),
                    endReached = false
                )
            }
            fetchPage(offset = 0)
        }
    }

    /**
     * Вызывается списком, когда пользователь докрутил почти до конца.
     * Повторные вызовы во время загрузки игнорируются.
     */
    fun loadMore() {
        val current = _state.value
        if (current.isLoading || current.isLoadingMore || current.endReached) return
        if (current.results.isEmpty()) return

        _state.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch { fetchPage(offset = current.results.size) }
    }

    private suspend fun fetchPage(offset: Int) {
        val token = generation
        val snapshot = _state.value

        try {
            val page = ModrinthApi.search(
                query = snapshot.query,
                type = snapshot.tab,
                filters = snapshot.filters,
                offset = offset
            )
            if (token != generation) return

            _state.update { current ->
                val merged = if (offset == 0) page.items else current.results + page.items
                current.copy(
                    results = merged,
                    isLoading = false,
                    isLoadingMore = false,
                    endReached = !page.hasMore,
                    error = null
                )
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            if (token != generation) return
            _state.update { current ->
                if (offset == 0) {
                    current.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = throwable.message ?: "Не удалось загрузить список"
                    )
                } else {
                    current.copy(
                        isLoadingMore = false,
                        message = throwable.message ?: "Не удалось загрузить ещё"
                    )
                }
            }
        }
    }

    // -------------------------------------------------------------- карточка

    fun openMod(mod: ModHit) {
        _state.update {
            it.copy(
                selected = mod,
                versions = emptyList(),
                versionsError = null,
                versionsLoading = true
            )
        }
        viewModelScope.launch {
            try {
                val list = ModrinthApi.versions(mod.id)
                _state.update { it.copy(versions = list, versionsLoading = false) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                _state.update {
                    it.copy(
                        versionsLoading = false,
                        versionsError = throwable.message ?: "Не удалось загрузить версии"
                    )
                }
            }
        }
    }

    fun closeMod() {
        _state.update { it.copy(selected = null, versions = emptyList()) }
    }

    // ------------------------------------------------------------ скачивание

    fun download(context: Context, version: ModVersion) {
        if (_state.value.downloads[version.id] is DownloadState.Running) return

        _state.update {
            it.copy(downloads = it.downloads + (version.id to DownloadState.Running(0f)))
        }

        val appContext = context.applicationContext

        viewModelScope.launch {
            try {
                val path = ModDownloader.download(appContext, version) { fraction ->
                    _state.update { current ->
                        current.copy(
                            downloads = current.downloads +
                                (version.id to DownloadState.Running(fraction))
                        )
                    }
                }
                _state.update {
                    it.copy(
                        downloads = it.downloads + (version.id to DownloadState.Done(path)),
                        message = "Сохранено: $path"
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (throwable: Throwable) {
                _state.update {
                    it.copy(
                        downloads = it.downloads + (
                            version.id to DownloadState.Failed(
                                throwable.message ?: "Ошибка загрузки"
                            )
                            ),
                        message = "Не удалось скачать файл"
                    )
                }
            }
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    companion object {
        /**
         * Запасной список версий Minecraft — показывается, пока справочник
         * из API не загрузился (или если сети нет).
         */
        val GAME_VERSIONS = listOf(
            "1.21.9", "1.21.8", "1.21.7", "1.21.6", "1.21.5", "1.21.4",
            "1.21.3", "1.21.2", "1.21.1", "1.21",
            "1.20.6", "1.20.4", "1.20.1",
            "1.19.4", "1.19.2",
            "1.18.2",
            "1.17.1",
            "1.16.5",
            "1.15.2",
            "1.12.2"
        )
    }
}
