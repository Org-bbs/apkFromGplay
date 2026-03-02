package com.example.apkfromgplay.ui

import com.example.apkfromgplay.data.model.PlayApp

data class MainUiState(
    val query: String = "",
    val isLoading: Boolean = false,
    val apps: List<PlayApp> = emptyList(),
    val message: String? = null
)
