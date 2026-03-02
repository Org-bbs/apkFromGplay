package com.example.apkfromgplay.data

import com.example.apkfromgplay.data.model.PlayApp
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class GooglePlaySearchService(
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val parser: GooglePlayHtmlParser = GooglePlayHtmlParser()
) {

    suspend fun searchApps(query: String): List<PlayApp> = withContext(Dispatchers.IO) {
        val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        val searchUrl = "https://play.google.com/store/search?c=apps&q=$encodedQuery&hl=en_US&gl=US"
        val request = Request.Builder()
            .url(searchUrl)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
            )
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Google Play search failed: ${response.code}")
            }
            val html = response.body?.string().orEmpty()
            parser.parse(html)
        }
    }
}
