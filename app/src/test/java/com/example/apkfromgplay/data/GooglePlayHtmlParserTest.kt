package com.example.apkfromgplay.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GooglePlayHtmlParserTest {

    private val parser = GooglePlayHtmlParser()

    @Test
    fun `parse should extract apps and deduplicate package name`() {
        val html = """
            <html>
              <body>
                <a href="/store/apps/details?id=com.demo.alpha" aria-label="Alpha App">Alpha App</a>
                <a href="/store/apps/details?id=com.demo.alpha">Alpha Duplicate</a>
                <a href="/store/apps/details?id=com.demo.beta">Beta App</a>
              </body>
            </html>
        """.trimIndent()

        val apps = parser.parse(html)

        assertEquals(2, apps.size)
        assertEquals("com.demo.alpha", apps[0].packageName)
        assertEquals("Alpha App", apps[0].title)
        assertEquals("com.demo.beta", apps[1].packageName)
    }

    @Test
    fun `extract package name should return null for invalid link`() {
        assertNull(parser.extractPackageName("https://example.com/app"))
    }
}
