package com.fivestars.batterytracker

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val tagName: String,
    val versionName: String,
    val releaseTitle: String,
    val changelog: String,
    val releaseUrl: String,
    val apkDownloadUrl: String?
)

sealed class UpdateCheckResult {
    data class UpdateAvailable(val release: ReleaseInfo, val currentVersion: String) : UpdateCheckResult()
    data class UpToDate(val currentVersion: String) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

object UpdateChecker {
    private const val TAG = "UpdateChecker"
    const val GITHUB_API_URL = "https://api.github.com/repos/FrancescoMin/batteryhealthtracker/releases/latest"

    /**
     * Compares two semantic version strings (e.g. "1.6" vs "1.5" or "v1.6.1" vs "1.6").
     * Returns:
     *   > 0 if v1 > v2
     *   < 0 if v1 < v2
     *   0 if v1 == v2
     */
    fun compareVersions(v1: String, v2: String): Int {
        val clean1 = v1.trim().replace(Regex("^[a-zA-Z\\-_]+"), "")
        val clean2 = v2.trim().replace(Regex("^[a-zA-Z\\-_]+"), "")

        val parts1 = clean1.split(".").mapNotNull { it.takeWhile { char -> char.isDigit() }.toIntOrNull() }
        val parts2 = clean2.split(".").mapNotNull { it.takeWhile { char -> char.isDigit() }.toIntOrNull() }

        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val num1 = parts1.getOrElse(i) { 0 }
            val num2 = parts2.getOrElse(i) { 0 }
            if (num1 != num2) {
                return num1.compareTo(num2)
            }
        }
        return 0
    }

    suspend fun checkLatestRelease(currentVersion: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(GITHUB_API_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "BatteryHealthTracker-Android")
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                connectTimeout = 10000
                readTimeout = 10000
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)

                val tagName = json.optString("tag_name", "").trim()
                val releaseTitle = json.optString("name", tagName)
                val body = json.optString("body", "").trim()
                val htmlUrl = json.optString("html_url", "https://github.com/FrancescoMin/batteryhealthtracker/releases")

                // Search for .apk asset in release assets
                var apkUrl: String? = null
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i)
                        val name = asset?.optString("name", "") ?: ""
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            val downloadUrl = asset?.optString("browser_download_url", "")
                            apkUrl = if (!downloadUrl.isNullOrBlank()) downloadUrl else null
                            break
                        }
                    }
                }

                val releaseVersion = tagName.replace(Regex("^[a-zA-Z\\-_]+"), "").trim()
                val hasUpdate = compareVersions(releaseVersion, currentVersion) > 0

                val releaseInfo = ReleaseInfo(
                    tagName = tagName,
                    versionName = releaseVersion,
                    releaseTitle = if (releaseTitle.isNotBlank()) releaseTitle else tagName,
                    changelog = body,
                    releaseUrl = htmlUrl,
                    apkDownloadUrl = apkUrl ?: htmlUrl
                )

                if (hasUpdate) {
                    UpdateCheckResult.UpdateAvailable(releaseInfo, currentVersion)
                } else {
                    UpdateCheckResult.UpToDate(currentVersion)
                }
            } else if (responseCode == 404) {
                UpdateCheckResult.Error("No releases found on GitHub.")
            } else if (responseCode == 403) {
                UpdateCheckResult.Error("GitHub API rate limit reached. Please try again later.")
            } else {
                UpdateCheckResult.Error("GitHub returned error HTTP $responseCode")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking updates", e)
            UpdateCheckResult.Error(e.localizedMessage ?: "Network connection error")
        } finally {
            connection?.disconnect()
        }
    }
}
