package com.dhs0319.bills.core.im

import com.dhs0319.bills.core.common.AuthProvider
import com.dhs0319.bills.core.common.BiliConstants
import com.dhs0319.bills.core.model.ImUploadedImage
import com.dhs0319.bills.infra.network.BiliApiException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class ImImageUploader @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val authProvider: AuthProvider
) {
    suspend fun upload(bytes: ByteArray, mimeType: String): ImUploadedImage = withContext(Dispatchers.IO) {
        val accessKey = authProvider.accessToken
        check(accessKey.isNotBlank()) { "请先登录" }
        val extension = when {
            mimeType.contains("png", ignoreCase = true) -> "png"
            mimeType.contains("gif", ignoreCase = true) -> "gif"
            mimeType.contains("webp", ignoreCase = true) -> "webp"
            else -> "jpg"
        }
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("biz", "im")
            .addFormDataPart("access_key", accessKey)
            .addFormDataPart("file_up", "message.$extension", bytes.toRequestBody(mimeType.toMediaType()))
            .build()
        val request = Request.Builder()
            .url("${BiliConstants.BASE_URL_API}/x/dynamic/feed/draw/upload_bfs")
            .post(body)
            .build()
        okHttpClient.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "图片上传失败，请重试" }
            val json = JSONObject(response.body?.string() ?: error("图片上传失败，请重试"))
            val code = json.optInt("code", -1)
            if (code != 0) throw BiliApiException(code, json.optString("message", "图片上传失败"))
            parseImUploadedImage(json.optJSONObject("data") ?: error("图片上传结果为空"), mimeType)
        }
    }
}
