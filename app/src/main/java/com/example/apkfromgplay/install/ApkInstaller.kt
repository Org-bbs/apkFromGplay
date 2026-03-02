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

    fun getDownloadFailureReason(downloadId: Long): String? {
        val query = DownloadManager.Query().setFilterById(downloadId)
        downloadManager.query(query).use { cursor ->
            if (!cursor.moveToFirst()) return null
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
            if (statusIndex < 0 || reasonIndex < 0) return null
            val status = cursor.getInt(statusIndex)
            if (status != DownloadManager.STATUS_FAILED) return null
            val reasonCode = cursor.getInt(reasonIndex)
            return mapFailureReason(reasonCode)
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

        private fun mapFailureReason(reasonCode: Int): String {
            val reasonName = when (reasonCode) {
                DownloadManager.ERROR_CANNOT_RESUME -> "ERROR_CANNOT_RESUME"
                DownloadManager.ERROR_DEVICE_NOT_FOUND -> "ERROR_DEVICE_NOT_FOUND"
                DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "ERROR_FILE_ALREADY_EXISTS"
                DownloadManager.ERROR_FILE_ERROR -> "ERROR_FILE_ERROR"
                DownloadManager.ERROR_HTTP_DATA_ERROR -> "ERROR_HTTP_DATA_ERROR"
                DownloadManager.ERROR_INSUFFICIENT_SPACE -> "ERROR_INSUFFICIENT_SPACE"
                DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "ERROR_TOO_MANY_REDIRECTS"
                DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "ERROR_UNHANDLED_HTTP_CODE"
                DownloadManager.ERROR_UNKNOWN -> "ERROR_UNKNOWN"
                else -> "UNKNOWN"
            }
            return "$reasonName($reasonCode)"
        }
    }
}
