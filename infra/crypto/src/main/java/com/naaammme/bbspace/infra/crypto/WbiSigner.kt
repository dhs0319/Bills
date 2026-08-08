package com.naaammme.bbspace.infra.crypto

import java.security.MessageDigest

/**
 * Wbi 签名计算器
 * 用于 B 站 Web 接口的签名验证
 */
object WbiSigner {
    private val mixinKeyEncTab = intArrayOf(
        46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35, 27, 43, 5, 49,
        33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13, 37, 48, 7, 16, 24, 55, 40,
        61, 26, 17, 0, 1, 60, 51, 30, 4, 22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11,
        36, 20, 34, 44, 52
    )
    private const val filteredChars = "!'()*"
    private val hexChars = "0123456789ABCDEF".toCharArray()

    /**
     * 计算 Wbi 签名
     * @param params 请求参数
     * @param imgKey 图片密钥
     * @param subKey 子密钥
     * @return 签名后的参数 Map
     */
    fun sign(
        params: Map<String, String>,
        imgKey: String,
        subKey: String,
        wts: Long = System.currentTimeMillis() / 1000L
    ): Map<String, String> {
        val mixinKey = getMixinKey(imgKey + subKey)
        val signParams = params.mapValues { (_, value) -> value.filterNot { it in filteredChars } }
            .toMutableMap()
            .apply { put("wts", wts.toString()) }
            .toSortedMap()
        val query = encodeQuery(signParams)
        val wbiSign = md5(query + mixinKey)

        return params.toMutableMap().apply {
            put("wts", wts.toString())
            put("w_rid", wbiSign)
        }
    }

    fun encodeQuery(params: Map<String, String>): String {
        return params.entries.joinToString("&") { (key, value) ->
            "${encodeComponent(key)}=${encodeComponent(value)}"
        }
    }

    private fun getMixinKey(orig: String): String {
        return mixinKeyEncTab.asSequence()
            .mapNotNull { index -> orig.getOrNull(index) }
            .joinToString("")
            .take(32)
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun encodeComponent(value: String): String {
        val bytes = value.toByteArray(Charsets.UTF_8)
        val out = StringBuilder(bytes.size * 3)
        bytes.forEach { byte ->
            val intValue = byte.toInt() and 0xFF
            val charValue = intValue.toChar()
            if (charValue.isAllowedQueryChar()) {
                out.append(charValue)
            } else {
                out.append('%')
                out.append(hexChars[intValue ushr 4])
                out.append(hexChars[intValue and 0x0F])
            }
        }
        return out.toString()
    }

    private fun Char.isAllowedQueryChar(): Boolean {
        return this in 'A'..'Z' ||
            this in 'a'..'z' ||
            this in '0'..'9' ||
            this == '-' ||
            this == '_' ||
            this == '.' ||
            this == '~'
    }
}
