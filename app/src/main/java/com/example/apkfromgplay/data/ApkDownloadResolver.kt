package com.example.apkfromgplay.data

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class ApkDownloadResolver {
    fun resolveDownloadUrl(packageName: String): String {
        val safePackageName = URLEncoder.encode(packageName.trim(), StandardCharsets.UTF_8.toString())
        return "https://d.apkpure.com/b/APK/$safePackageName?version=latest"
    }
}
