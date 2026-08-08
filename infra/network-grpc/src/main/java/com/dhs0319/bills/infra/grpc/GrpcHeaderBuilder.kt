package com.dhs0319.bills.infra.grpc

import android.util.Base64
import com.dhs0319.bills.core.common.AuthProvider
import com.dhs0319.bills.core.common.BiliConstants
import com.dhs0319.bills.core.common.UserAgentBuilder
import com.dhs0319.bills.infra.crypto.RegionCodeCache
import com.dhs0319.bills.infra.crypto.AuroraEidGenerator
import com.dhs0319.bills.infra.crypto.DeviceIdentity
import com.dhs0319.bills.infra.crypto.LegalRegionCache
import com.dhs0319.bills.infra.crypto.TicketGenerator
import com.dhs0319.bills.infra.crypto.TraceIdGenerator
import com.dhs0319.bills.infra.network.BiliMetadataBuilder
import javax.inject.Inject
import javax.inject.Singleton
/**
 * gRPC 请求 Header 构建器
 * 按照官方顺序构建,自动 Base64 编码所有 -bin 后缀的 header
 * 登录态从 AuthProvider 自动读取
 */
@Singleton
class GrpcHeaderBuilder @Inject constructor(
    private val authProvider: AuthProvider,
    private val deviceIdentity: DeviceIdentity,
    private val metadataBuilder: BiliMetadataBuilder,
    private val regionCodeCache: RegionCodeCache,
    private val legalRegionCache: LegalRegionCache,
    private val ticketGenerator: TicketGenerator
) {
    private val userAgent by lazy {
        UserAgentBuilder.buildGrpcUserAgent(deviceIdentity.model, deviceIdentity.osVer)
    }

    fun build(
        deviceBin: ByteArray = metadataBuilder.buildDevice(),
        compressed: Boolean = false
    ): Map<String, String> {
        val mid = authProvider.mid
        val accessKey = authProvider.accessToken
        return buildMap {
            put("accept", "*/*")
            put("accept-encoding", "gzip, deflate, br")
            put("app-key", BiliConstants.APP_KEY_NAME)
            if (accessKey.isNotEmpty()) {
                put("authorization", "identify_v1 $accessKey")
            }
            put("bili-http-engine", "ignet")
            put("buvid", deviceIdentity.buvid)
            put("content-type", "application/grpc")
            put("env", BiliConstants.ENV)
            if (compressed) {
                put("grpc-accept-encoding", "identity, gzip")
                put("grpc-encoding", "gzip")
            }
            put("user-agent", userAgent)
            if (mid > 0) {
                val auroraEid = AuroraEidGenerator.generate(mid)
                if (auroraEid.isNotEmpty()) {
                    put("x-bili-aurora-eid", auroraEid)
                }
            }
            put("x-bili-device-bin", Base64.encodeToString(deviceBin, Base64.NO_WRAP or Base64.NO_PADDING))
            put("x-bili-fawkes-req-bin", metadataBuilder.buildFawkesBase64())
            put("x-bili-locale-bin", metadataBuilder.buildLocaleBase64())
            put("x-bili-metadata-ip-region", regionCodeCache.get())
            val legalRegion = legalRegionCache.get()
            if (mid > 0 && legalRegion.isNotEmpty()) {
                put("x-bili-metadata-legal-region", legalRegion)
            }
            put("x-bili-metadata-bin", Base64.encodeToString(metadataBuilder.buildMetadata(accessKey), Base64.NO_WRAP or Base64.NO_PADDING))
            if (mid > 0) {
                put("x-bili-mid", mid.toString())
            }
            put("x-bili-network-bin", metadataBuilder.buildNetworkBase64())
            val ticket = ticketGenerator.getTicketForHeader(mid, accessKey)
            if (ticket.isNotEmpty()) {
                put("x-bili-ticket", ticket)
            }
            put("x-bili-trace-id", TraceIdGenerator.generate())
        }
    }
}
