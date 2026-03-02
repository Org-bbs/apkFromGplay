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
        val candidates = buildList {
            resolveFromAptoide(normalizedPackage)?.let { add(DownloadCandidate("Aptoide", it)) }
            add(DownloadCandidate("APKPure", buildApkPureFallbackUrl(normalizedPackage)))
        }

        val failures = mutableListOf<DownloadProbeFailure>()
        for (candidate in candidates) {
            val probeResult = probeDownloadAvailability(candidate.url)
            if (probeResult.available) {
                return@withContext candidate.url
            }
            failures += DownloadProbeFailure(
                source = candidate.source,
                url = candidate.url,
                httpCode = probeResult.httpCode,
                reason = probeResult.reason
            )
        }
        throw ApkDownloadCheckException(failures)
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
            .header("User-Agent", MOBILE_USER_AGENT)
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

    private fun probeDownloadAvailability(url: String): DownloadProbeResult {
        val headRequest = Request.Builder()
            .url(url)
            .head()
            .header("User-Agent", MOBILE_USER_AGENT)
            .build()
        val headResult = executeProbe(headRequest)
        if (headResult.available) return headResult

        val rangeGetRequest = Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", MOBILE_USER_AGENT)
            .header("Range", "bytes=0-0")
            .build()
        val rangeResult = executeProbe(rangeGetRequest)
        if (rangeResult.available) return rangeResult

        return if (rangeResult.httpCode != null) rangeResult else headResult
    }

    private fun executeProbe(request: Request): DownloadProbeResult {
        return runCatching {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    DownloadProbeResult(available = true, httpCode = response.code, reason = null)
                } else {
                    DownloadProbeResult(
                        available = false,
                        httpCode = response.code,
                        reason = "HTTP ${response.code}"
                    )
                }
            }
        }.getOrElse { throwable ->
            DownloadProbeResult(
                available = false,
                httpCode = null,
                reason = throwable.message ?: throwable.javaClass.simpleName
            )
        }
    }

    data class DownloadProbeFailure(
        val source: String,
        val url: String,
        val httpCode: Int?,
        val reason: String?
    )

    class ApkDownloadCheckException(
        val failures: List<DownloadProbeFailure>
    ) : IOException(buildReasonMessage(failures)) {
        val failureSummary: String
            get() = buildReasonMessage(failures)

        companion object {
            private fun buildReasonMessage(failures: List<DownloadProbeFailure>): String {
                return failures.joinToString("；") { failure ->
                    val codeOrReason = failure.httpCode?.let { "HTTP $it" } ?: failure.reason ?: "unknown"
                    "${failure.source}: $codeOrReason"
                }
            }
        }
    }

    private data class DownloadCandidate(
        val source: String,
        val url: String
    )

    private data class DownloadProbeResult(
        val available: Boolean,
        val httpCode: Int?,
        val reason: String?
    )

    companion object {
        private const val APTOIDE_SEARCH_API = "https://ws75.aptoide.com/api/7/apps/search?query="
        private const val MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
    }
}
