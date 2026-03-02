package com.example.apkfromgplay.ui

import com.example.apkfromgplay.data.ApkRepository
import com.example.apkfromgplay.data.model.PlayApp
import com.example.apkfromgplay.testutil.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `searchApps should show message when query is blank`() {
        val viewModel = MainViewModel(FakeRepository())

        viewModel.onQueryChanged("   ")
        viewModel.searchApps()

        assertEquals("请输入搜索关键词", viewModel.uiState.value.message)
    }

    @Test
    fun `searchApps should update apps when repository returns data`() = runTest {
        val fakeApps = listOf(
            PlayApp(
                title = "Maps",
                developer = "Google LLC",
                packageName = "com.google.android.apps.maps",
                detailsUrl = "https://play.google.com/store/apps/details?id=com.google.android.apps.maps"
            )
        )
        val viewModel = MainViewModel(FakeRepository(fakeApps))

        viewModel.onQueryChanged("maps")
        viewModel.searchApps()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(1, viewModel.uiState.value.apps.size)
        assertEquals("Maps", viewModel.uiState.value.apps.first().title)
        assertTrue(viewModel.uiState.value.message == null)
    }

    private class FakeRepository(
        private val apps: List<PlayApp> = emptyList()
    ) : ApkRepository {
        override suspend fun searchApps(query: String): List<PlayApp> = apps

        override fun resolveApkDownloadUrl(packageName: String): String {
            return "https://example.com/$packageName.apk"
        }
    }
}
