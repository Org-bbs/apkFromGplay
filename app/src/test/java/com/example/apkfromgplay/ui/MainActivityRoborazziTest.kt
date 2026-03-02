package com.example.apkfromgplay.ui

import android.view.View
import androidx.test.core.app.ActivityScenario
import com.example.apkfromgplay.MainActivity
import com.example.apkfromgplay.data.ApkRepository
import com.example.apkfromgplay.data.model.PlayApp
import com.example.apkfromgplay.di.ServiceLocator
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import org.junit.After
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
@OptIn(ExperimentalRoborazziApi::class)
class MainActivityRoborazziTest {

    @After
    fun tearDown() {
        ServiceLocator.clearOverride()
    }

    @Test
    fun captureMainPageAfterSearch() {
        ServiceLocator.repositoryOverride = FakeRepository(
            listOf(
                PlayApp(
                    title = "Google Maps",
                    developer = "Google LLC",
                    packageName = "com.google.android.apps.maps",
                    detailsUrl = "https://play.google.com/store/apps/details?id=com.google.android.apps.maps"
                )
            )
        )

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<View>(com.example.apkfromgplay.R.id.queryEditText).apply {
                    if (this is android.widget.EditText) setText("maps")
                }
                activity.findViewById<View>(com.example.apkfromgplay.R.id.searchButton).performClick()
                ShadowLooper.idleMainLooper()
                val output = File("build/outputs/roborazzi/MainActivityRoborazziTest_afterSearch.png")
                output.parentFile?.mkdirs()
                activity.findViewById<View>(android.R.id.content).captureRoboImage(
                    file = output,
                    roborazziOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)
                )
                assertTrue(output.exists())
            }
        }
    }

    private class FakeRepository(
        private val apps: List<PlayApp>
    ) : ApkRepository {
        override suspend fun searchApps(query: String): List<PlayApp> = apps

        override fun resolveApkDownloadUrl(packageName: String): String {
            return "https://example.com/$packageName.apk"
        }
    }
}
