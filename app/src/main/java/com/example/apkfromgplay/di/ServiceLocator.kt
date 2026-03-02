package com.example.apkfromgplay.di
import com.example.apkfromgplay.data.ApkDownloadResolver
import com.example.apkfromgplay.data.ApkRepository
import com.example.apkfromgplay.data.GooglePlaySearchService
import com.example.apkfromgplay.data.NetworkApkRepository

object ServiceLocator {
    @Volatile
    var repositoryOverride: ApkRepository? = null

    @Volatile
    private var repository: ApkRepository? = null

    fun provideRepository(): ApkRepository {
        repositoryOverride?.let { return it }
        return repository ?: synchronized(this) {
            repository ?: createRepository().also { repository = it }
        }
    }

    fun clearOverride() {
        repositoryOverride = null
    }

    private fun createRepository(): ApkRepository {
        val searchService = GooglePlaySearchService()
        val downloadResolver = ApkDownloadResolver()
        return NetworkApkRepository(searchService, downloadResolver)
    }
}
