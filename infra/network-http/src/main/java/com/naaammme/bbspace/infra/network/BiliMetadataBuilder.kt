package com.naaammme.bbspace.infra.network

import android.util.Base64
import bilibili.metadata.MetadataOuterClass
import bilibili.metadata.device.DeviceOuterClass
import bilibili.metadata.fawkes.Fawkes
import bilibili.metadata.locale.LocaleOuterClass
import bilibili.metadata.network.NetworkOuterClass
import com.naaammme.bbspace.core.common.BiliConstants
import com.naaammme.bbspace.infra.crypto.BiliSessionId
import com.naaammme.bbspace.infra.crypto.DeviceIdentity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BiliMetadataBuilder @Inject constructor(
    private val deviceIdentity: DeviceIdentity
) {
    private val fawkesBytes by lazy {
        val sessionId = BiliSessionId.header()
        Fawkes.FawkesReq.newBuilder().apply {
            appkey = BiliConstants.APP_KEY_NAME
            env = BiliConstants.ENV
            this.sessionId = sessionId
        }.build().toByteArray()
    }

    private val localeBytes by lazy {
        val cLocale = LocaleOuterClass.LocaleIds.newBuilder()
            .setLanguage("zh")
            .setScript("Hans")
            .setRegion("CN")
            .build()
        val sLocale = LocaleOuterClass.LocaleIds.newBuilder()
            .setLanguage("zh")
            .setScript("Hans")
            .setRegion("CN")
            .build()

        LocaleOuterClass.Locale.newBuilder().apply {
            this.cLocale = cLocale
            this.sLocale = sLocale
            timezone = "Asia/Shanghai"
            utcOffset = "+08:00"
        }.build().toByteArray()
    }

    private val networkBytes by lazy {
        val quality = NetworkOuterClass.NetQuality.newBuilder()
            .setSuccessRate(-1.0f)
            .build()

        NetworkOuterClass.Network.newBuilder().apply {
            type = NetworkOuterClass.NetworkType.WIFI
            tf = NetworkOuterClass.TFType.TF_UNKNOWN
            oid = "46000"
            this.quality = quality
        }.build().toByteArray()
    }

    private val localeBase64 by lazy {
        Base64.encodeToString(localeBytes, BASE64_FLAGS)
    }

    private val fawkesBase64 by lazy {
        Base64.encodeToString(fawkesBytes, BASE64_FLAGS)
    }

    private val networkBase64 by lazy {
        Base64.encodeToString(networkBytes, BASE64_FLAGS)
    }

    fun buildMetadata(accessKey: String = ""): ByteArray {
        return MetadataOuterClass.Metadata.newBuilder().apply {
            if (accessKey.isNotEmpty()) {
                this.accessKey = accessKey
            }
            mobiApp = BiliConstants.MOBI_APP
            device = ""
            build = BiliConstants.BUILD
            channel = BiliConstants.CHANNEL
            buvid = deviceIdentity.buvid
            platform = BiliConstants.PLATFORM
        }.build().toByteArray()
    }

    fun buildDevice(): ByteArray {
        val fts = System.currentTimeMillis() / 1000 - (30 * 24 * 3600)
        return DeviceOuterClass.Device.newBuilder().apply {
            appId = BiliConstants.APP_ID
            build = BiliConstants.BUILD
            buvid = deviceIdentity.buvid
            mobiApp = BiliConstants.MOBI_APP
            platform = BiliConstants.PLATFORM
            device = ""
            channel = BiliConstants.CHANNEL
            brand = deviceIdentity.brand
            model = deviceIdentity.model
            osver = deviceIdentity.osVer
            fpLocal = deviceIdentity.fp
            fpRemote = deviceIdentity.fp
            versionName = BiliConstants.VERSION
            fp = deviceIdentity.fp
            this.fts = fts
        }.build().toByteArray()
    }
    fun buildLocale(): ByteArray {
        return localeBytes
    }

    fun buildLocaleBase64(): String = localeBase64

    fun buildFawkes(): ByteArray = fawkesBytes

    fun buildFawkesBase64(): String = fawkesBase64

    // TODO 动态化 type用ConnectivityManager检测WiFi或蜂窝 oid用TelephonyManager.getNetworkOperator读运营商 cellular读蜂窝代数 tf等免流模块实现后接入
    fun buildNetwork(): ByteArray = networkBytes

    fun buildNetworkBase64(): String = networkBase64

    private companion object {
        const val BASE64_FLAGS = Base64.NO_WRAP or Base64.NO_PADDING
    }
}
