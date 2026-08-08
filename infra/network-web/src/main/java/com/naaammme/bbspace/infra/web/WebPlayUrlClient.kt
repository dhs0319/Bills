package com.naaammme.bbspace.infra.web

import com.naaammme.bbspace.core.common.BiliConstants
import com.naaammme.bbspace.core.common.UserAgentBuilder
import com.naaammme.bbspace.infra.crypto.WbiSigner
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

@Singleton
class WebPlayUrlClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val dmParamsFactory: WebDmParamsFactory
) {
    @Volatile
    private var buvid3 = ""

    @Volatile
    private var wbiKeys: WbiKeys? = null

    suspend fun fetchPlayback(request: WebPlayUrlRequest): JSONObject = withContext(Dispatchers.IO) {
        val dmParams = dmParamsFactory.create()
        val webBuvid3 = getBuvid3()
        val keys = getWbiKeys()
        val signedParams = WbiSigner.sign(
            params = buildParams(request, dmParams),
            imgKey = keys.imgKey,
            subKey = keys.subKey
        )
        val url = "${BiliConstants.BASE_URL_API}$PLAY_URL_ENDPOINT?${WbiSigner.encodeQuery(signedParams)}"
        val httpRequest = Request.Builder()
            .url(url)
            .get()
            .addHeader("accept", "application/json, text/plain, */*")
            .addHeader("origin", BiliConstants.BASE_URL_API.replace("api.", "www."))
            .addHeader("referer", request.referer())
            .addHeader("user-agent", UserAgentBuilder.buildWebUserAgent())
            .addHeader("cookie", "buvid3=$webBuvid3")
            .build()
        val json = executeJson(httpRequest)
        ensureSuccess(json, "web playurl")
        json
    }

    private fun buildParams(
        request: WebPlayUrlRequest,
        dmParams: WebDmParams
    ): Map<String, String> {
        return buildMap {
            put("avid", request.aid.toString())
            put("cid", request.cid.toString())
            request.bvid?.takeIf(String::isNotBlank)?.let { put("bvid", it) }
            put("qn", WEB_QN)
            put("fnver", "0")
            put("fnval", "4048")
            put("fourk", "1")
            put("gaia_source", "")
            put("from_client", "BROWSER")
            put("is_main_page", "true")
            put("need_fragment", "false")
            put("isGaiaAvoided", "false")
            put("client_attr", "0")
            put("version_name", "4.9.78")
            put("app_id", "100")
            put("voice_balance", "1")
            put("try_look", "1")
            put("web_location", "1315873")
            put("dm_img_list", dmParams.dmImgList)
            put("dm_img_str", dmParams.dmImgStr)
            put("dm_cover_img_str", dmParams.dmCoverImgStr)
            put("dm_img_inter", dmParams.dmImgInter)
        }
    }

    private fun getBuvid3(): String {
        buvid3.takeIf(String::isNotBlank)?.let { return it }
        val request = Request.Builder()
            .url("${BiliConstants.BASE_URL_API}$GET_BUVID_ENDPOINT")
            .get()
            .addHeader("referer", "https://www.bilibili.com/")
            .addHeader("user-agent", UserAgentBuilder.buildWebUserAgent())
            .build()
        val json = executeJson(request)
        ensureSuccess(json, "getbuvid")
        val value = json.optJSONObject("data")
            ?.optString("buvid")
            .orEmpty()
            .takeIf(String::isNotBlank)
            ?: error("getbuvid 缺少 buvid")
        buvid3 = value
        return value
    }

    private fun getWbiKeys(): WbiKeys {
        wbiKeys?.let { return it }
        val request = Request.Builder()
            .url("${BiliConstants.BASE_URL_API}$NAV_ENDPOINT")
            .get()
            .addHeader("referer", "https://www.bilibili.com/")
            .addHeader("user-agent", UserAgentBuilder.buildWebUserAgent())
            .build()
        val json = executeJson(request)
        val wbiImg = json.optJSONObject("data")
            ?.optJSONObject("wbi_img")
            ?: error("nav 缺少 wbi_img")
        val keys = WbiKeys(
            imgKey = wbiImg.optString("img_url").extractWbiKey(),
            subKey = wbiImg.optString("sub_url").extractWbiKey()
        )
        wbiKeys = keys
        return keys
    }

    private fun executeJson(request: Request): JSONObject {
        return okHttpClient.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: error("empty response")
            JSONObject(body)
        }
    }

    private fun ensureSuccess(json: JSONObject, api: String) {
        val code = json.optInt("code", -1)
        if (code == 0) return
        val msg = json.optString("message").ifBlank { "code=$code" }
        error("$api 失败: $msg")
    }

    private fun String.extractWbiKey(): String {
        return substringAfterLast('/')
            .substringBefore('.')
            .takeIf(String::isNotBlank)
            ?: error("wbi key 为空")
    }

    private fun WebPlayUrlRequest.referer(): String {
        val bvid = bvid?.takeIf(String::isNotBlank)
        return if (bvid != null) {
            "https://www.bilibili.com/video/$bvid/"
        } else {
            "https://www.bilibili.com/video/av$aid/"
        }
    }

    private companion object {
        const val GET_BUVID_ENDPOINT = "/x/web-frontend/getbuvid"
        const val NAV_ENDPOINT = "/x/web-interface/nav"
        const val PLAY_URL_ENDPOINT = "/x/player/wbi/playurl"
        const val WEB_QN = "120"
    }
}

data class WebPlayUrlRequest(
    val aid: Long,
    val bvid: String? = null,
    val cid: Long
)

private data class WbiKeys(
    val imgKey: String,
    val subKey: String
)
