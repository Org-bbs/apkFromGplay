package com.example.apkfromgplay.install

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import com.example.apkfromgplay.data.model.PlayApp

class ApkInstaller(
    private val context: Context,
    private val downloadManager: DownloadManager = context.getSystemService(DownloadManager::class.java)
) {

    fun enqueueDownload(app: PlayApp, downloadUrl: String): Long {
        val fileName = "${app.packageName}-${System.currentTimeMillis()}.apk"
        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle("${app.title}.apk")
            .setDescription(app.packageName)
            .setMimeType(APK_MIME_TYPE)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        return downloadManager.enqueue(request)
    }

    fun isDownloadSuccessful(downloadId: Long): Boolean {
        val query = DownloadManager.Query().setFilterById(downloadId)
        downloadManager.query(query).use { cursor ->
            if (!cursor.moveToFirst()) return false
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            if (statusIndex < 0) return false
            return cursor.getInt(statusIndex) == DownloadManager.STATUS_SUCCESSFUL
        }
    }

    fun installDownload(downloadId: Long): InstallResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            return InstallResult.NEED_UNKNOWN_SOURCE_PERMISSION
        }
        val apkUri = downloadManager.getUriForDownloadedFile(downloadId) ?: return InstallResult.FAILED
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return runCatching {
            context.startActivity(installIntent)
        }.fold(
            onSuccess = { InstallResult.STARTED },
            onFailure = { InstallResult.FAILED }
        )
    }

    fun openUnknownSourceSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    enum class InstallResult {
        STARTED,
        NEED_UNKNOWN_SOURCE_PERMISSION,
        FAILED
    }

    companion object {
        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}
