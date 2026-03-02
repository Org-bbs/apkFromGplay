package com.example.apkfromgplay.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ApkDownloadResolverTest {

    private val resolver = ApkDownloadResolver()

    @Test
    fun `resolve url should format with package name`() {
        val url = resolver.resolveDownloadUrl("com.example.app")

        assertEquals(
            "https://d.apkpure.com/b/APK/com.example.app?version=latest",
            url
        )
    }
}
