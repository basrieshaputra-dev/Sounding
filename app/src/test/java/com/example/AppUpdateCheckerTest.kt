package com.example

import com.example.util.AppUpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateCheckerTest {

    @Test
    fun isRemoteVersionNewer_detectsHigherSemanticVersions() {
        assertTrue(AppUpdateChecker.isRemoteVersionNewer("1.0", 1, "v1.0.1"))
        assertTrue(AppUpdateChecker.isRemoteVersionNewer("1.0.0", 1, "v1.1.0"))
        assertTrue(AppUpdateChecker.isRemoteVersionNewer("1.0.2", 1, "v1.0.10"))
        assertTrue(AppUpdateChecker.isRemoteVersionNewer("1.9.9", 1, "v2.0.0"))
        assertTrue(AppUpdateChecker.isRemoteVersionNewer("1.0.1-beta.1", 1, "v1.0.1"))
        assertTrue(AppUpdateChecker.isRemoteVersionNewer("1.0", 1, "v1.0+2"))
    }

    @Test
    fun isRemoteVersionNewer_returnsFalseForSameOrOlderVersions() {
        assertFalse(AppUpdateChecker.isRemoteVersionNewer("1.0", 1, "v1.0"))
        assertFalse(AppUpdateChecker.isRemoteVersionNewer("1.0", 1, "v1.0.0"))
        assertFalse(AppUpdateChecker.isRemoteVersionNewer("1.2.0", 2, "v1.1.9"))
        assertFalse(AppUpdateChecker.isRemoteVersionNewer("1.0.1", 1, "v1.0.1-rc1"))
        assertFalse(AppUpdateChecker.isRemoteVersionNewer("1.0", 2, "v1.0+2"))
        assertFalse(AppUpdateChecker.isRemoteVersionNewer("1.0", 1, ""))
        assertFalse(AppUpdateChecker.isRemoteVersionNewer("1.0", 1, "not-a-version"))
    }

    @Test
    fun parseGitHubReleaseJson_prefersApkAssetAndFallsBackToHtmlUrl() {
        val withApkJson = """
            {
              "tag_name": "v1.0.1",
              "name": "Sounding Tangki v1.0.1",
              "body": "- Perbaikan stabilitas\n- Tema Corporate Executive",
              "draft": false,
              "html_url": "https://github.com/basrieshaputra-dev/Sounding/releases/tag/v1.0.1",
              "assets": [
                {
                  "name": "checksums.txt",
                  "browser_download_url": "https://github.com/basrieshaputra-dev/Sounding/releases/download/v1.0.1/checksums.txt"
                },
                {
                  "name": "sounding-tangki-v1.0.1.apk",
                  "browser_download_url": "https://github.com/basrieshaputra-dev/Sounding/releases/download/v1.0.1/sounding-tangki-v1.0.1.apk"
                }
              ]
            }
        """.trimIndent()

        val parsed = AppUpdateChecker.parseGitHubReleaseJson(withApkJson)
        assertNotNull(parsed)
        assertEquals("v1.0.1", parsed!!.displayVersion)
        assertTrue(parsed.hasDirectApkAsset)
        assertEquals(
            "https://github.com/basrieshaputra-dev/Sounding/releases/download/v1.0.1/sounding-tangki-v1.0.1.apk",
            parsed.bestDownloadUrl
        )

        val noApkJson = """
            {
              "tag_name": "1.0.2",
              "name": "Release 1.0.2",
              "body": "",
              "draft": false,
              "html_url": "https://github.com/basrieshaputra-dev/Sounding/releases/tag/1.0.2",
              "assets": []
            }
        """.trimIndent()

        val parsedFallback = AppUpdateChecker.parseGitHubReleaseJson(noApkJson)
        assertNotNull(parsedFallback)
        assertEquals("v1.0.2", parsedFallback!!.displayVersion)
        assertFalse(parsedFallback.hasDirectApkAsset)
        assertEquals(
            "https://github.com/basrieshaputra-dev/Sounding/releases/tag/1.0.2",
            parsedFallback.bestDownloadUrl
        )
    }

    @Test
    fun parseGitHubReleaseJson_handlesDraftAndMalformedJsonSafely() {
        val draftJson = """{"tag_name":"v2.0.0","draft":true,"html_url":"https://github.com/basrieshaputra-dev/Sounding/releases/tag/v2.0.0"}"""
        assertNull(AppUpdateChecker.parseGitHubReleaseJson(draftJson))
        assertNull(AppUpdateChecker.parseGitHubReleaseJson(""))
        assertNull(AppUpdateChecker.parseGitHubReleaseJson("{invalid json"))
    }

    @Test
    fun isSafeReleaseUrl_validatesHttpsGithubHostsOnly() {
        assertTrue(AppUpdateChecker.isSafeReleaseUrl("https://github.com/basrieshaputra-dev/Sounding/releases/latest"))
        assertTrue(AppUpdateChecker.isSafeReleaseUrl("https://objects.githubusercontent.com/github-production-release-asset/test.apk"))
        assertFalse(AppUpdateChecker.isSafeReleaseUrl("http://github.com/basrieshaputra-dev/Sounding/releases/latest"))
        assertFalse(AppUpdateChecker.isSafeReleaseUrl("https://evil.example.com/app.apk"))
        assertFalse(AppUpdateChecker.isSafeReleaseUrl("javascript:alert(1)"))
    }
}
