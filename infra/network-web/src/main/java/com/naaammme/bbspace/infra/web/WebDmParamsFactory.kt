package com.naaammme.bbspace.infra.web

import android.util.Base64
import java.util.Random
import javax.inject.Inject
import org.json.JSONArray
import org.json.JSONObject

class WebDmParamsFactory @Inject constructor() {
    fun create(): WebDmParams {
        val random = Random(System.currentTimeMillis())
        return WebDmParams(
            dmImgStr = base64NoPadding(DM_IMG_RAW),
            dmCoverImgStr = base64NoPadding(DM_COVER_IMG_RAW),
            dmImgList = buildDmImgList(random),
            dmImgInter = buildDmImgInter(random)
        )
    }

    private fun buildDmImgList(random: Random): String {
        val arr = JSONArray()
        var timestamp = random.nextIntIn(800, 1201)
        var x = random.nextIntIn(1500, 2501)
        var y = random.nextIntIn(-400, 601)

        repeat(DM_IMG_LIST_COUNT) {
            x += random.nextIntIn(-300, 301)
            y += random.nextIntIn(-300, 301)
            arr.put(
                JSONObject().apply {
                    put("x", x)
                    put("y", y)
                    put("z", random.nextIntIn(0, 1501))
                    put("timestamp", timestamp)
                    put("k", random.nextIntIn(60, 128))
                    put("type", 0)
                }
            )
            timestamp += random.nextIntIn(90, 161)
        }
        return arr.toString()
    }

    private fun buildDmImgInter(random: Random): String {
        val ds = JSONArray().apply {
            put(
                JSONObject().apply {
                    put("t", TAG_CODES[random.nextInt(TAG_CODES.size)])
                    put("c", base64NoPadding(CLS_POOL[random.nextInt(CLS_POOL.size)]))
                    put("p", randomTri(random))
                    put("s", randomTri(random))
                }
            )
        }
        return JSONObject().apply {
            put("ds", ds)
            put("wh", randomTri(random))
            put("of", randomTri(random))
        }.toString()
    }

    private fun randomTri(random: Random): JSONArray {
        return JSONArray().apply {
            put(random.nextIntIn(1, 1_000_000))
            put(random.nextIntIn(-9_999, 10_000))
            put(random.nextIntIn(-9_999, 10_000))
        }
    }

    private fun base64NoPadding(value: String): String {
        return Base64.encodeToString(
            value.toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP
        ).trimEnd('=')
    }

    private fun Random.nextIntIn(start: Int, endExclusive: Int): Int {
        return start + nextInt(endExclusive - start)
    }

    private companion object {
        const val DM_IMG_RAW = "WebGL 1.0 (OpenGL ES 2.0 Chromium)"
        const val DM_COVER_IMG_RAW = "ANGLE (NVIDIA, GeForce RTX 4070 Laptop GPU, D3D11)Google Inc. (NVIDIA)"
        const val DM_IMG_LIST_COUNT = 13
        val TAG_CODES = intArrayOf(
            1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
            16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29
        )
        val CLS_POOL = arrayOf("", "btn", "item", "video-card", "container", "panel", "list", "card")
    }
}

data class WebDmParams(
    val dmImgStr: String,
    val dmCoverImgStr: String,
    val dmImgList: String,
    val dmImgInter: String
)
