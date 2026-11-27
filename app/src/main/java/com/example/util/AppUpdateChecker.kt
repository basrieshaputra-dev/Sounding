package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URL
import java.net.UnknownHostException

data class GitHubReleaseInfo(
    val tagName: String,
    val releaseName: String,
    val body: String,
    val apkDownloadUrl: String?,
    val htmlUrl: String,
    val publishedAt: String = ""
) {
    val bestDownloadUrl: String
        get() = apkDownloadUrl?.takeIf { it.isNotBlank() } ?: htmlUrl

    val displayVersion: String
        get() {
            val trimmed = tagName.trim()
            if (trimmed.isEmpty()) return releaseName.trim().ifEmpty { "-" }
            return if (trimmed.startsWith("v", ignoreCase = true)) trimmed else "v$trimmed"
        }

    val hasDirectApkAsset: Boolean
        get() = !apkDownloadUrl.isNullOrBlank()
}

sealed interface AppUpdateCheckResult {
    data class UpdateAvailable(
        val currentVersion: String,
        val release: GitHubReleaseInfo
    ) : AppUpdateCheckResult

    data class UpToDate(
        val currentVersion: String,
        val latestTag: String
    ) : AppUpdateCheckResult

    data class Error(
        val reason: UpdateErrorReason,
        val detail: String = ""
    ) : AppUpdateCheckResult
}

enum class UpdateErrorReason {
    OFFLINE_OR_TIMEOUT,
    RATE_LIMITED,
    NO_RELEASES_YET,
    SERVER_OR_PARSE_ERROR
}

private data class GitHubReleaseAssetDto(
    @Json(name = "name") val name: String? = null,
    @Json(name = "browser_download_url") val browserDownloadUrl: String? = null
)

private data class GitHubReleaseDto(
    @Json(name = "tag_name") val tagName: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "body") val body: String? = null,
    @Json(name = "html_url") val htmlUrl: String? = null,
    @Json(name = "published_at") val publishedAt: String? = null,
    @Json(name = "draft") val draft: Boolean? = null,
    @Json(name = "assets") val assets: List<GitHubReleaseAssetDto>? = null
)

object AppUpdateChecker {

    private const val TAG = "AppUpdateChecker"
    const val GITHUB_OWNER = "basrieshaputra-dev"
    const val GITHUB_REPO = "Sounding"
    const val GITHUB_RELEASES_API_URL =
        "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    const val GITHUB_RELEASES_PAGE_URL =
        "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    private const val CONNECT_TIMEOUT_MS = 8_000
    private const val READ_TIMEOUT_MS = 8_000

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val releaseAdapter by lazy {
        moshi.adapter(GitHubReleaseDto::class.java)
    }

    suspend fun checkLatestRelease(
        currentVersionName: String = BuildConfig.VERSION_NAME,
        currentVersionCode: Int = BuildConfig.VERSION_CODE
    ): AppUpdateCheckResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(GITHUB_RELEASES_API_URL)
            connection = (url.openConnection() as? HttpURLConnection)
                ?: return@withContext AppUpdateCheckResult.Error(
                    reason = UpdateErrorReason.SERVER_OR_PARSE_ERROR,
                    detail = "HTTP connection unavailable"
                )
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            connection.setRequestProperty(
                "User-Agent",
                "SoundingTangki-Android/${currentVersionName.ifBlank { "1.0" }}"
            )

            val responseCode = connection.responseCode
            when {
                responseCode in 200..299 -> {
                    val responseBody = connection.inputStream.use { stream ->
                        BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
                    }
                    val release = parseGitHubReleaseJson(responseBody)
                        ?: return@withContext AppUpdateCheckResult.Error(
                            reason = UpdateErrorReason.SERVER_OR_PARSE_ERROR,
                            detail = "Invalid release payload"
                        )

                    if (isRemoteVersionNewer(currentVersionName, currentVersionCode, release.tagName)) {
                        AppUpdateCheckResult.UpdateAvailable(
                            currentVersion = formatCurrentVersionLabel(currentVersionName),
                            release = release
                        )
                    } else {
                        AppUpdateCheckResult.UpToDate(
                            currentVersion = formatCurrentVersionLabel(currentVersionName),
                            latestTag = release.displayVersion
                        )
                    }
                }

                responseCode == HttpURLConnection.HTTP_NOT_FOUND -> {
                    AppUpdateCheckResult.Error(
                        reason = UpdateErrorReason.NO_RELEASES_YET,
                        detail = "HTTP 404"
                    )
                }

                responseCode == HttpURLConnection.HTTP_FORBIDDEN || responseCode == 429 -> {
                    AppUpdateCheckResult.Error(
                        reason = UpdateErrorReason.RATE_LIMITED,
                        detail = "HTTP $responseCode"
                    )
                }

                else -> {
                    AppUpdateCheckResult.Error(
                        reason = UpdateErrorReason.SERVER_OR_PARSE_ERROR,
                        detail = "HTTP $responseCode"
                    )
                }
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: UnknownHostException) {
            safeLogDebug("Update check skipped (offline): ${e.message}")
            AppUpdateCheckResult.Error(
                reason = UpdateErrorReason.OFFLINE_OR_TIMEOUT,
                detail = e.localizedMessage.orEmpty()
            )
        } catch (e: SocketTimeoutException) {
            safeLogDebug("Update check timed out: ${e.message}")
            AppUpdateCheckResult.Error(
                reason = UpdateErrorReason.OFFLINE_OR_TIMEOUT,
                detail = e.localizedMessage.orEmpty()
            )
        } catch (e: IOException) {
            safeLogWarn("Update check I/O failure", e)
            AppUpdateCheckResult.Error(
                reason = UpdateErrorReason.OFFLINE_OR_TIMEOUT,
                detail = e.localizedMessage.orEmpty()
            )
        } catch (t: Throwable) {
            safeLogWarn("Unexpected error checking GitHub release", t)
            AppUpdateCheckResult.Error(
                reason = UpdateErrorReason.SERVER_OR_PARSE_ERROR,
                detail = t.localizedMessage.orEmpty()
            )
        } finally {
            runCatching { connection?.disconnect() }
        }
    }

    internal fun parseGitHubReleaseJson(rawJson: String): GitHubReleaseInfo? {
        return try {
            val trimmed = rawJson.trim()
            if (trimmed.isEmpty()) return null
            val dto = releaseAdapter.fromJson(trimmed) ?: return null

            if (dto.draft == true) return null

            val tagName = dto.tagName.orEmpty().trim()
            if (tagName.isEmpty()) return null

            val releaseName = dto.name.orEmpty().trim().ifEmpty { tagName }
            val body = dto.body.orEmpty().trim()
            val rawHtmlUrl = dto.htmlUrl.orEmpty().trim()
            val htmlUrl = if (isSafeReleaseUrl(rawHtmlUrl)) rawHtmlUrl else GITHUB_RELEASES_PAGE_URL
            val publishedAt = dto.publishedAt.orEmpty().trim()

            val apkDownloadUrl = dto.assets
                .orEmpty()
                .firstOrNull { asset ->
                    val assetName = asset.name.orEmpty().trim()
                    val downloadUrl = asset.browserDownloadUrl.orEmpty().trim()
                    assetName.endsWith(".apk", ignoreCase = true) && isSafeReleaseUrl(downloadUrl)
                }
                ?.browserDownloadUrl
                ?.trim()

            GitHubReleaseInfo(
                tagName = tagName,
                releaseName = releaseName,
                body = body,
                apkDownloadUrl = apkDownloadUrl,
                htmlUrl = htmlUrl,
                publishedAt = publishedAt
            )
        } catch (t: Throwable) {
            safeLogWarn("Failed to parse GitHub release JSON", t)
            null
        }
    }

    /**
     * Compares a GitHub release tag (e.g. "v1.0.1", "1.2.0", "v1.0.0+2") against the installed
     * app versionName / versionCode. Returns true only when remote is strictly newer.
     */
    internal fun isRemoteVersionNewer(
        currentVersionName: String,
        currentVersionCode: Int,
        remoteTagName: String
    ): Boolean {
        val current = parseVersionComponents(currentVersionName, fallbackBuildCode = currentVersionCode)
            ?: return false
        val remote = parseVersionComponents(remoteTagName, fallbackBuildCode = null)
            ?: return false

        val maxLen = maxOf(current.numbers.size, remote.numbers.size)
        for (i in 0 until maxLen) {
            val c = current.numbers.getOrElse(i) { 0 }
            val r = remote.numbers.getOrElse(i) { 0 }
            if (r != c) return r > c
        }

        // Same numeric version (e.g., 1.0.1-beta vs 1.0.1): stable release is newer than pre-release
        if (current.isPreRelease != remote.isPreRelease) {
            return current.isPreRelease && !remote.isPreRelease
        }

        // If remote tag explicitly encodes a build number (e.g., v1.0+2), compare with versionCode
        val remoteBuild = remote.buildCode
        if (remoteBuild != null && currentVersionCode > 0 && remoteBuild != currentVersionCode) {
            return remoteBuild > currentVersionCode
        }

        return false
    }

    internal fun isSafeReleaseUrl(rawUrl: String): Boolean {
        if (rawUrl.isBlank()) return false
        return try {
            val uri = URI(rawUrl.trim())
            val scheme = uri.scheme?.lowercase() ?: return false
            val host = uri.host?.lowercase() ?: return false
            if (scheme != "https") return false
            host == "github.com" ||
                host.endsWith(".github.com") ||
                host == "githubusercontent.com" ||
                host.endsWith(".githubusercontent.com")
        } catch (_: Throwable) {
            false
        }
    }

    fun formatCurrentVersionLabel(versionName: String = BuildConfig.VERSION_NAME): String {
        val clean = versionName.trim().ifEmpty { "1.0" }
        return if (clean.startsWith("v", ignoreCase = true)) clean else "v$clean"
    }

    fun openUpdateDownloadUrl(context: Context, rawUrl: String): Boolean {
        val safeUrl = if (isSafeReleaseUrl(rawUrl)) rawUrl.trim() else GITHUB_RELEASES_PAGE_URL
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (t: Throwable) {
            safeLogWarn("Unable to open update download URL: $safeUrl", t)
            false
        }
    }

    private fun safeLogDebug(message: String) {
        runCatching { Log.d(TAG, message) }
    }

    private fun safeLogWarn(message: String, t: Throwable) {
        runCatching { Log.w(TAG, message, t) }
    }

    private data class ParsedVersion(
        val numbers: List<Int>,
        val isPreRelease: Boolean,
        val buildCode: Int?
    )

    private fun parseVersionComponents(raw: String, fallbackBuildCode: Int?): ParsedVersion? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        val firstDigitIdx = trimmed.indexOfFirst { it.isDigit() }
        if (firstDigitIdx < 0) return null
        val versionPart = trimmed.substring(firstDigitIdx)

        val plusParts = versionPart.split("+", limit = 2)
        val mainAndPre = plusParts[0]
        val explicitBuild = plusParts.getOrNull(1)
            ?.takeWhile { it.isDigit() }
            ?.toIntOrNull()

        val dashParts = mainAndPre.split("-", limit = 2)
        val coreNumeric = dashParts[0]
        val preSuffix = dashParts.getOrNull(1)?.trim().orEmpty()
        val isPreRelease = preSuffix.isNotEmpty()

        val numbers = coreNumeric
            .split(".")
            .mapNotNull { segment ->
                segment.takeWhile { it.isDigit() }.toIntOrNull()
            }

        if (numbers.isEmpty()) return null

        return ParsedVersion(
            numbers = numbers,
            isPreRelease = isPreRelease,
            buildCode = explicitBuild ?: fallbackBuildCode
        )
    }
}
