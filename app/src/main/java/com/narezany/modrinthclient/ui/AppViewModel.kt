package com.narezany.modrinthclient.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.narezany.modrinthclient.data.ModDownloader
import com.narezany.modrinthclient.data.ModHit
import com.narezany.modrinthclient.data.ModVersion
import com.narezany.modrinthclient.data.ModrinthApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Состояние скачивания конкретной версии мода. */
sealed interface DownloadState {
    data class Running(val progress: Float) : DownloadState
    data class Done(val path: String) : DownloadState
    data class Failed(val message: String) : DownloadState
}

data class ModrinthUiState(
    val query: String = "",
    val gameVersion: String? = null,
    val results: List<ModHit> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
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

    init {
        viewModelScope.launch { runSearch() }
    }

    // ------------------------------------------------------------ поиск

    fun updateQuery(value: String) {
        _state.update { it.copy(query = value) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400) // дебаунс, чтобы не долбить API на каждой букве
            runSearch()
        }
    }

    fun setGameVersion(version: String?) {
        _state.update { it.copy(gameVersion = version) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch { runSearch() }
    }

    fun retry() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch { runSearch() }
    }

    private suspend fun runSearch() {
        val current = _state.value
        _state.update { it.copy(isLoading = true, error = null) }

        runCatching { ModrinthApi.search(current.query, current.gameVersion) }
            .onSuccess { found ->
                _state.update { it.copy(results = found, isLoading = false, error = null) }
            }
            .onFailure { throwable ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = throwable.message ?: "Не удалось загрузить список модов"
                    )
                }
            }
    }

    // ------------------------------------------------------------ карточка

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
            runCatching { ModrinthApi.versions(mod.id) }
                .onSuccess { list ->
                    _state.update { it.copy(versions = list, versionsLoading = false) }
                }
                .onFailure { throwable ->
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

    // ---------------------------------------------------------- скачивание

    fun download(context: Context, version: ModVersion) {
        if (_state.value.downloads[version.id] is DownloadState.Running) return

        _state.update {
            it.copy(downloads = it.downloads + (version.id to DownloadState.Running(0f)))
        }

        val appContext = context.applicationContext

        viewModelScope.launch {
            runCatching {
                ModDownloader.download(appContext, version) { fraction ->
                    _state.update { current ->
                        current.copy(
                            downloads = current.downloads + (
                                version.id to DownloadState.Running(fraction)
                                )
                        )
                    }
                }
            }
                .onSuccess { path ->
                    _state.update {
                        it.copy(
                            downloads = it.downloads + (version.id to DownloadState.Done(path)),
                            message = "Сохранено: $path"
                        )
                    }
                }
                .onFailure { throwable ->
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
        /** Быстрые фильтры по популярным версиям Minecraft. */
        val GAME_VERSIONS = listOf(
            "1.21.8",
            "1.21.1",
            "1.20.6",
            "1.20.1",
            "1.19.2",
            "1.18.2",
            "1.16.5"
        )
    }
}
