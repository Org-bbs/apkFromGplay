package com.example.apkfromgplay.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ApkDownloadResolverTest {

    private val resolver = ApkDownloadResolver()

    @Test
    fun `fallback url should format with package name`() {
        val url = resolver.buildApkPureFallbackUrl("com.example.app")

        assertEquals(
            "https://d.apkpure.com/b/APK/com.example.app?version=latest",
            url
        )
    }

    @Test
    fun `extract aptoide url should prefer exact package`() {
        val json = """
            {
              "datalist": {
                "list": [
                  {
                    "package": "com.other.app",
                    "file": { "path": "https://pool.apk.aptoide.com/store/other.apk" }
                  },
                  {
                    "package": "com.example.app",
                    "file": { "path": "https://pool.apk.aptoide.com/store/example.apk" }
                  }
                ]
              }
            }
        """.trimIndent()

        val url = resolver.extractAptoideApkUrl(json, "com.example.app")

        assertEquals("https://pool.apk.aptoide.com/store/example.apk", url)
    }

    @Test
    fun `extract aptoide url should return null when no apk path`() {
        val json = """
            {
              "datalist": {
                "list": [
                  {
                    "package": "com.example.app",
                    "file": { "path": "https://example.com/not_apk.zip" }
                  }
                ]
              }
            }
        """.trimIndent()

        val url = resolver.extractAptoideApkUrl(json, "com.example.app")

        assertNull(url)
    }
}
