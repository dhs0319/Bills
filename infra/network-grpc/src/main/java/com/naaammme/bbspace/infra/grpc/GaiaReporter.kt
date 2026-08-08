package com.naaammme.bbspace.infra.grpc

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import bilibili.gaia.gw.GwApi.DeviceAppList
import bilibili.gaia.gw.GwApi.EncryptType
import bilibili.gaia.gw.GwApi.FetchPublicKeyReply
import bilibili.gaia.gw.GwApi.GaiaEncryptMsgReq
import bilibili.gaia.gw.GwApi.GaiaMsgHeader
import bilibili.gaia.gw.GwApi.PayloadType
import bilibili.gaia.gw.GwApi.UploadAppListReply
import com.google.protobuf.ByteString
import com.google.protobuf.Empty
import com.naaammme.bbspace.core.common.log.Logger
import com.naaammme.bbspace.infra.crypto.GaiaEncryptor
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gaia 风控上报
 * 获取公钥 加密应用列表 上报 管理上报频率
 */
@Singleton
class GaiaReporter @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val grpcClient: BiliGrpcClient
) {
    companion object {
        private const val TAG = "GaiaReporter"
        private const val PREFS_NAME = "gaia_reporter"
        private const val KEY_LAST_UPLOAD = "last_upload_time"
        private const val KEY_FIRST_INSTALL = "first_install"
        private const val UPLOAD_INTERVAL = 7 * 24 * 60 * 60 * 1000L

        private const val FETCH_KEY_EP = "bilibili.gaia.gw.Gaia/ExGetAxe"
        private const val UPLOAD_EP = "bilibili.gaia.gw.Gaia/ExClimbAppleTrees"

        private val APP_LIST_BLACKLIST = setOf(
            "com.bilibili.app.blue",
            "tv.danmaku.bili",
            "com.topjohnwu.magisk",
            "com.topjohnwu.magisk.lite",
            "io.github.huskydg.magisk",
            "me.weishu.kernelsu",
            "me.bmax.apatch",
            "me.bmax.apatch.next",
            "de.robv.android.xposed.installer",
            "org.lsposed.manager",
            "org.meowcat.edxposed.manager",
            "com.saurik.substrate",
            "com.devadvance.rootcloak",
            "com.devadvance.rootcloak2",
            "com.koushikdutta.superuser",
            "eu.chainfire.supersu",
            "com.noshufou.android.su",
            "com.kingroot.kinguser",
            "com.kingo.root",
            "com.chelpus.lackypatch",
            "com.android.vending.billing.InAppBillingService.COIN",
            "com.dimonvideo.luckypatcher",
            "com.lbe.parallel",
            "com.bly.dkplat",
            "io.va.exposed",
            "com.excelliance.dualaid",
            "com.lody.virtual",
            "com.qihoo.magic"
        )

        private val APP_LIST_BLACKLIST_PREFIXES = listOf(
            "de.robv.android.xposed",
            "org.lsposed",
            "org.meowcat.edxposed",
            "com.topjohnwu.magisk",
            "io.github.huskydg.magisk",
            "me.weishu.kernelsu",
            "me.bmax.apatch",
            "com.bly.dkplat",
            "com.lbe.parallel",
            "com.excelliance.dualaid",
            "com.lody.virtual"
        )

        private const val MAX_APP_COUNT = 100
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 检查并执行风控上报
     * 首次安装或距上次上报超过24小时才会执行
     */
    suspend fun reportIfNeeded() {
        try {
            if (!shouldReport()) {
                Logger.d(TAG) { "跳过上报" }
                return
            }

            val isFirst = prefs.getBoolean(KEY_FIRST_INSTALL, true)
            Logger.d(TAG) { "开始风控上报 isFirst=$isFirst" }

            val keyReply = fetchPublicKey()
            Logger.d(TAG) { "公钥获取成功 version=${keyReply.version} deadline=${keyReply.deadline}" }

            if (keyReply.deadline < System.currentTimeMillis() / 1000) {
                Logger.w(TAG) { "公钥已过期" }
                return
            }

            val userApps = collectApps()
            val sysApps = emptyList<String>()
            Logger.d(TAG) { "应用列表收集完成 sys=${sysApps.size} user=${userApps.size}" }

            val source = if (isFirst) "first_installation" else "first_open"
            val reply = upload(sysApps, userApps, source, keyReply.publicKey)

            Logger.i(TAG) { "上报成功 trace_id: ${reply.traceId}" }
            markUploaded()
        } catch (e: Exception) {
            Logger.e(TAG, e) { "上报失败" }
        }
    }

    private fun shouldReport(): Boolean {
        if (prefs.getBoolean(KEY_FIRST_INSTALL, true)) return true
        val last = prefs.getLong(KEY_LAST_UPLOAD, 0)
        return System.currentTimeMillis() - last >= UPLOAD_INTERVAL
    }

    private suspend fun fetchPublicKey(): FetchPublicKeyReply {
        return grpcClient.call(
            endpoint = FETCH_KEY_EP,
            requestBytes = Empty.getDefaultInstance().toByteArray(),
            parser = FetchPublicKeyReply.parser()
        )
    }

    private suspend fun upload(
        sysApps: List<String>,
        userApps: List<String>,
        source: String,
        publicKey: String
    ): UploadAppListReply {
        val appList = DeviceAppList.newBuilder()
            .setSource(source)
            .addAllSystemAppList(sysApps)
            .addAllUserAppList(userApps)
            .build()

        val (encKey, encPayload) = GaiaEncryptor.encrypt(appList.toByteArray(), publicKey)

        val req = GaiaEncryptMsgReq.newBuilder()
            .setHeader(
                GaiaMsgHeader.newBuilder()
                    .setEncodeType(EncryptType.SERVER_RSA_AES)
                    .setPayloadType(PayloadType.DEVICE_APP_LIST)
                    .setEncodedAesKey(ByteString.copyFrom(encKey))
                    .setTs(System.currentTimeMillis())
            )
            .setEncryptPayload(ByteString.copyFrom(encPayload))
            .build()

        return grpcClient.call(
            endpoint = UPLOAD_EP,
            requestBytes = req.toByteArray(),
            parser = UploadAppListReply.parser()
        )
    }

    private fun collectApps(): List<String> {
        return try {
            val packages = context.packageManager
                .getInstalledApplications(PackageManager.GET_META_DATA)
            val user = mutableListOf<String>()

            for (app in packages) {
                val pkg = app.packageName
                if (
                    (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0 &&
                    pkg.isNotBlank() &&
                    !isBlacklistedApp(pkg)
                ) {
                    user.add(pkg)
                    if (user.size >= MAX_APP_COUNT) break
                }
            }

            user
        } catch (e: Exception) {
            Logger.w(TAG) { "收集应用列表失败 使用空列表" }
            emptyList()
        }
    }

    private fun isBlacklistedApp(pkg: String): Boolean {
        return pkg in APP_LIST_BLACKLIST ||
                APP_LIST_BLACKLIST_PREFIXES.any { prefix -> pkg.startsWith(prefix) }
    }

    private fun markUploaded() {
        prefs.edit()
            .putBoolean(KEY_FIRST_INSTALL, false)
            .putLong(KEY_LAST_UPLOAD, System.currentTimeMillis())
            .apply()
    }
}
