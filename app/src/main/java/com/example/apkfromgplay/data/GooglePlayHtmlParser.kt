package com.example.apkfromgplay.data

import com.example.apkfromgplay.data.model.PlayApp
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class GooglePlayHtmlParser {

    fun parse(searchHtml: String): List<PlayApp> {
        val document = Jsoup.parse(searchHtml)
        val links = document.select("a[href*=/store/apps/details?id=],a[href*=play.google.com/store/apps/details?id=]")
        val seenPackageNames = linkedSetOf<String>()
        val apps = mutableListOf<PlayApp>()

        for (link in links) {
            val packageName = extractPackageName(link.attr("href")) ?: continue
            if (!seenPackageNames.add(packageName)) continue

            apps += PlayApp(
                title = resolveTitle(link, packageName),
                developer = resolveDeveloper(link),
                packageName = packageName,
                detailsUrl = "https://play.google.com/store/apps/details?id=$packageName"
            )
        }
        return apps
    }

    internal fun extractPackageName(detailsLink: String): String? {
        val marker = "id="
        val markerStart = detailsLink.indexOf(marker)
        if (markerStart < 0) return null
        val valueStart = markerStart + marker.length
        val tail = detailsLink.substring(valueStart)
        val end = tail.indexOf('&').takeIf { it >= 0 } ?: tail.length
        return tail.substring(0, end).takeIf { it.matches(PACKAGE_PATTERN) }
    }

    private fun resolveTitle(link: Element, fallbackPackageName: String): String {
        val directCandidates = listOf(
            link.attr("aria-label"),
            link.attr("title"),
            link.text(),
            link.selectFirst("[title]")?.attr("title"),
            link.selectFirst("img[alt]")?.attr("alt")
        )
        return directCandidates.firstOrNull { !it.isNullOrBlank() }?.trim() ?: fallbackPackageName
    }

    private fun resolveDeveloper(link: Element): String {
        val developerNode = link.parent()?.selectFirst("a[href*=/store/apps/developer?id=]")
            ?: link.closest("div")?.selectFirst("a[href*=/store/apps/developer?id=]")
            ?: link.closest("div")?.selectFirst("span")
        return developerNode?.text()?.takeIf { it.isNotBlank() } ?: "Unknown"
    }

    companion object {
        private val PACKAGE_PATTERN = Regex("[a-zA-Z0-9._]+")
    }
}
