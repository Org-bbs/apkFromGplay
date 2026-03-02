package com.example.apkfromgplay.data

import com.example.apkfromgplay.data.model.PlayApp

interface ApkRepository {
    suspend fun searchApps(query: String): List<PlayApp>
    fun resolveApkDownloadUrl(packageName: String): String
}

class NetworkApkRepository(
    private val searchService: GooglePlaySearchService,
    private val downloadResolver: ApkDownloadResolver
) : ApkRepository {

    override suspend fun searchApps(query: String): List<PlayApp> = searchService.searchApps(query)

    override fun resolveApkDownloadUrl(packageName: String): String {
        return downloadResolver.resolveDownloadUrl(packageName)
    }
}
