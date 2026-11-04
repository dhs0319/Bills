package com.dhs0319.bills.core.im

import com.dhs0319.bills.core.common.media.httpsImageUrlOrNull
import com.dhs0319.bills.core.model.ImUploadedImage
import org.json.JSONObject

internal fun encodeImTextContent(text: String): String = JSONObject().put("content", text).toString()

internal fun ImUploadedImage.toMessageContent(): String = JSONObject()
    .put("url", url)
    .put("width", width)
    .put("height", height)
    .put("imageType", imageType)
    .put("original", 1)
    .put("size", sizeKb)
    .toString()

internal fun parseImUploadedImage(data: JSONObject, mimeType: String): ImUploadedImage {
    val rawUrl = data.optString("image_url")
    val url = if (rawUrl.startsWith("//")) "https:$rawUrl" else rawUrl.httpsImageUrlOrNull().orEmpty()
    val width = data.optInt("image_width")
    val height = data.optInt("image_height")
    require(url.startsWith("https://") && url.length > "https://".length && width > 0 && height > 0) {
        "图片上传结果无效，请重试"
    }
    return ImUploadedImage(
        url = url,
        width = width,
        height = height,
        imageType = mimeType.substringBefore(';').substringAfter('/').lowercase().let {
            if (it == "jpeg") "jpg" else it
        },
        sizeKb = data.optDouble("img_size", 0.0).takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
    )
}
