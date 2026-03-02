package com.example.apkfromgplay.data

import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class ApkDownloadResolver(
    private val httpClient: OkHttpClient = OkHttpClient()
) {
    suspend fun resolveDownloadUrl(packageName: String): String = withContext(Dispatchers.IO) {
        val normalizedPackage = packageName.trim()
        resolveFromAptoide(normalizedPackage) ?: buildApkPureFallbackUrl(normalizedPackage)
    }

    internal fun buildApkPureFallbackUrl(packageName: String): String {
        val safePackageName = URLEncoder.encode(packageName.trim(), StandardCharsets.UTF_8.toString())
        return "https://d.apkpure.com/b/APK/$safePackageName?version=latest"
    }

    internal fun extractAptoideApkUrl(jsonBody: String, packageName: String): String? {
        val normalizedPackage = packageName.trim()
        val root = JSONObject(jsonBody)
        val appList = root.optJSONObject("datalist")?.optJSONArray("list") ?: return null

        var firstApkCandidate: String? = null
        for (index in 0 until appList.length()) {
            val app = appList.optJSONObject(index) ?: continue
            val fileNode = app.optJSONObject("file") ?: continue
            val path = fileNode.optString("path").takeIf { isApkUrl(it) }
                ?: fileNode.optString("path_alt").takeIf { isApkUrl(it) }
                ?: continue
            if (firstApkCandidate == null) firstApkCandidate = path
            val itemPackage = app.optString("package")
            if (itemPackage.equals(normalizedPackage, ignoreCase = true)) {
                return path
            }
        }
        return firstApkCandidate
    }

    private fun resolveFromAptoide(packageName: String): String? {
        val queryValue = URLEncoder.encode(packageName, StandardCharsets.UTF_8.toString())
        val request = Request.Builder()
            .url("$APTOIDE_SEARCH_API$queryValue&limit=15")
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
            )
            .build()

        return runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Aptoide API failed: ${response.code}")
                }
                val body = response.body?.string().orEmpty()
                extractAptoideApkUrl(body, packageName)
            }
        }.getOrNull()
    }

    private fun isApkUrl(url: String): Boolean {
        return url.startsWith("https://") && (
            url.contains(".apk", ignoreCase = true) || url.endsWith(".apk", ignoreCase = true)
            )
    }

    companion object {
        private const val APTOIDE_SEARCH_API = "https://ws75.aptoide.com/api/7/apps/search?query="
    }
}
