package com.dhs0319.bills.core.settings.update

import android.content.Context
import android.os.Build
import com.dhs0319.bills.core.common.log.Logger
import com.dhs0319.bills.core.designsystem.component.AppUpdateDialogState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class AppReleaseInfo(
    val version: String,
    val url: String,
    val desc: String?
)

sealed interface AppUpdateCheckResult {
    data class UpToDate(val release: AppReleaseInfo) : AppUpdateCheckResult
    data class HasUpdate(val release: AppReleaseInfo) : AppUpdateCheckResult
}

fun AppUpdateCheckResult.toDialogState(): AppUpdateDialogState {
    return when (this) {
        is AppUpdateCheckResult.UpToDate -> AppUpdateDialogState(
            title = "已是最新版本",
            desc = release.desc ?: "当前已是最新版本"
        )
        is AppUpdateCheckResult.HasUpdate -> release.toDialogState()
    }
}

fun AppReleaseInfo.toDialogState(): AppUpdateDialogState {
    return AppUpdateDialogState(
        title = "发现新版本 v$version",
        desc = desc ?: "暂无更新说明",
        confirmText = "前往下载",
        url = url
    )
}

fun Throwable.toDialogState(): AppUpdateDialogState {
    return AppUpdateDialogState(
        title = "检查更新失败",
        desc = message ?: "未知错误"
    )
}

@Singleton
class AppUpdateChecker @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "AppUpdateChecker"
        private const val LATEST_METADATA_URL =
            "https://raw.githubusercontent.com/dhs0319/Bills/main/latest.json"
    }

    suspend fun check(): Result<AppUpdateCheckResult> = withContext(Dispatchers.IO) {
        runCatching {
            val release = fetchLatestReleaseInfo()
            if (release.version == currentVersionName()) {
                AppUpdateCheckResult.UpToDate(release)
            } else {
                AppUpdateCheckResult.HasUpdate(release)
            }
        }.onFailure { error ->
            Logger.w(TAG) { "检查更新失败: ${error.message}" }
        }
    }

    private fun fetchLatestReleaseInfo(): AppReleaseInfo {
        val request = Request.Builder()
            .url(LATEST_METADATA_URL)
            .header("Accept", "application/json")
            .header("User-Agent", "Bills")
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("请求失败 ${response.code}")
            }
            val body = response.body?.string() ?: error("响应为空")
            val json = JSONObject(body)
            return AppReleaseInfo(
                version = json.requireNonBlankString("version").trimStart('v', 'V'),
                url = json.selectDownloadUrl(),
                desc = json.optString("releaseNotes")
                    .replace("\r\n", "\n")
                    .trim()
                    .takeIf(String::isNotBlank)
            )
        }
    }

    private fun currentVersionName(): String {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return (info.versionName ?: "")
            .trimStart('v', 'V')
    }
}

private fun JSONObject.requireNonBlankString(name: String): String {
    return optString(name)
        .trim()
        .takeIf(String::isNotBlank)
        ?: error("缺少有效字段: $name")
}

private fun JSONObject.selectDownloadUrl(): String {
    val urls = optJSONObject("downloadUrls")
    if (urls != null) {
        Build.SUPPORTED_ABIS
            .asSequence()
            .mapNotNull { abi -> urls.optString(abi).trim().takeIf(String::isNotBlank) }
            .firstOrNull()
            ?.let { return it }
    }

    return optString("releaseUrl")
        .trim()
        .takeIf(String::isNotBlank)
        ?: requireNonBlankString("downloadUrl")
}
