package com.dhs0319.bills.core.playback.subtitle

import com.bapis.bilibili.community.service.dm.v1.DmViewReply
import com.bapis.bilibili.community.service.dm.v1.DmViewReq
import com.bapis.bilibili.community.service.dm.v1.VideoSubtitle
import com.dhs0319.bills.core.auth.AuthStore
import com.dhs0319.bills.core.common.UserAgentBuilder
import com.dhs0319.bills.core.model.ResolvedVideoIds
import com.dhs0319.bills.infra.grpc.BiliGrpcClient
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject

@Singleton
class VideoSubtitleRepository @Inject constructor(
    private val grpcClient: BiliGrpcClient,
    private val okHttpClient: OkHttpClient,
    private val authStore: AuthStore
) {
    suspend fun fetchTracks(ids: ResolvedVideoIds): List<VideoSubtitleTrack> {
        var succeeded = false
        var lastError: Exception? = null
        // Prefer the app API, which uses the same login and device headers as video playback.
        try {
            val reply = grpcClient.call(
                endpoint = "bilibili.community.service.dm.v1.DM/DmView",
                requestBytes = DmViewReq.newBuilder()
                    .setPid(ids.aid).setOid(ids.cid).setType(1)
                    .setSpmid("united.player-video-detail.0.0").build().toByteArray(),
                parser = DmViewReply.parser()
            )
            succeeded = true
            mapTracks(reply.subtitle).takeIf { it.isNotEmpty() }?.let { return it }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            lastError = error
        }

        // New web players expose subtitle metadata separately as VideoSubtitle protobuf.
        val cookie = withContext(Dispatchers.IO) {
            authStore.getSavedCredential()?.cookies.orEmpty().joinToString("; ") { "${it.name}=${it.value}" }
        }
        try {
            val url = "https://api.bilibili.com/x/v2/subtitle/web/view".toHttpUrl().newBuilder()
                .addQueryParameter("oid", ids.cid.toString())
                .addQueryParameter("pid", ids.aid.toString()).build()
            val bytes = download(webRequest(url.toString(), cookie))
            val tracks = withContext(Dispatchers.Default) { mapTracks(VideoSubtitle.parseFrom(bytes)) }
            succeeded = true
            if (tracks.isNotEmpty()) return tracks
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            lastError = error
        }

        try {
            val url = "https://api.bilibili.com/x/player/v2".toHttpUrl().newBuilder()
                .addQueryParameter("aid", ids.aid.toString())
                .addQueryParameter("cid", ids.cid.toString()).build()
            val bytes = download(webRequest(url.toString(), cookie))
            val tracks = withContext(Dispatchers.Default) { parseLegacyTracks(JSONObject(bytes.toString(Charsets.UTF_8))) }
            succeeded = true
            if (tracks.isNotEmpty()) return tracks
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            lastError = error
        }
        if (!succeeded) throw IOException("字幕列表加载失败，请重试", lastError)
        return emptyList()
    }

    suspend fun fetchTimeline(track: VideoSubtitleTrack): SubtitleTimeline {
        // CDN requests carry no account cookies. Cancellation closes a pending download.
        val bytes = download(webRequest(track.url))
        return withContext(Dispatchers.Default) {
            val body = JSONObject(bytes.toString(Charsets.UTF_8)).optJSONArray("body")
                ?: throw IOException("字幕文件格式错误")
            val cues = ArrayList<SubtitleCue>(body.length())
            for (index in 0 until body.length()) {
                val item = body.optJSONObject(index) ?: continue
                val start = item.optDouble("from", Double.NaN)
                val end = item.optDouble("to", Double.NaN)
                val text = item.optString("content").trim()
                if (!start.isFinite() || !end.isFinite() || start < 0 || end <= start || text.isBlank()) continue
                cues += SubtitleCue((start * 1000).roundToLong(), (end * 1000).roundToLong(), text)
            }
            SubtitleTimeline.create(cues)
        }
    }

    private fun mapTracks(subtitle: VideoSubtitle): List<VideoSubtitleTrack> {
        return subtitle.subtitlesList.mapNotNull { item ->
            val url = normalizeUrl(item.subtitleUrl) ?: return@mapNotNull null
            VideoSubtitleTrack(
                id = item.idStr.ifBlank { item.id.takeIf { it > 0 }?.toString() ?: "${item.lan}:$url" },
                language = item.lan,
                name = item.lanDoc.ifBlank { item.lanDocBrief }.ifBlank { item.lan },
                url = url,
                isAi = item.typeValue == 1 || item.aiTypeValue > 0 || item.lan.startsWith("ai-")
            )
        }.distinctBy { it.id }
    }

    private fun parseLegacyTracks(json: JSONObject): List<VideoSubtitleTrack> {
        if (json.optInt("code", -1) != 0) throw IOException("字幕列表加载失败")
        val items = json.optJSONObject("data")?.optJSONObject("subtitle")?.optJSONArray("subtitles")
            ?: return emptyList()
        return buildList {
            for (index in 0 until items.length()) {
                val item = items.optJSONObject(index) ?: continue
                val url = normalizeUrl(item.optString("subtitle_url")) ?: continue
                val language = item.optString("lan")
                add(VideoSubtitleTrack(
                    id = item.optString("id_str").ifBlank { item.optString("id") }.ifBlank { "$language:$url" },
                    language = language,
                    name = item.optString("lan_doc").ifBlank { language },
                    url = url,
                    isAi = item.optInt("ai_type") > 0 || item.optInt("type") == 1 || language.startsWith("ai-")
                ))
            }
        }.distinctBy { it.id }
    }

    private fun normalizeUrl(value: String): String? {
        val absolute = if (value.startsWith("//")) "https:$value" else value
        return absolute.toHttpUrlOrNull()?.newBuilder()?.scheme("https")?.build()?.toString()
    }

    private fun webRequest(url: String, cookie: String = ""): Request {
        return Request.Builder().url(url)
            .header("Referer", "https://www.bilibili.com/")
            .header("User-Agent", UserAgentBuilder.buildWebUserAgent())
            .apply { if (cookie.isNotBlank()) header("Cookie", cookie) }
            .build()
    }

    private suspend fun download(request: Request): ByteArray = suspendCancellableCoroutine { continuation ->
        val call = okHttpClient.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val bytes = response.use {
                        if (!it.isSuccessful) throw IOException("字幕请求失败（HTTP ${it.code}）")
                        val body = it.body ?: throw IOException("字幕响应为空")
                        val source = body.source()
                        if (source.request(MAX_FILE_BYTES + 1)) throw IOException("字幕文件过大")
                        source.readByteArray()
                    }
                    continuation.resume(bytes)
                } catch (error: Exception) {
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
            }
        })
    }

    private companion object {
        const val MAX_FILE_BYTES = 8L * 1024 * 1024
    }
}
