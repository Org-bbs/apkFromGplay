package com.example.apkfromgplay.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.apkfromgplay.data.ApkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: ApkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun searchApps() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) {
            _uiState.update { it.copy(message = "请输入搜索关键词") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null) }
            runCatching { repository.searchApps(query) }
                .onSuccess { apps ->
                    val msg = if (apps.isEmpty()) "未找到匹配应用" else null
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            apps = apps,
                            message = msg
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            message = "搜索失败：${throwable.message ?: "未知错误"}"
                        )
                    }
                }
        }
    }

    suspend fun resolveDownloadUrl(packageName: String): String {
        return repository.resolveApkDownloadUrl(packageName)
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
