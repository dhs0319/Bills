package com.dhs0319.bills.infra.grpc

import com.google.protobuf.MessageLite
import com.google.protobuf.Parser
import com.dhs0319.bills.core.common.BiliConstants
import com.dhs0319.bills.infra.network.BiliApiException
import com.dhs0319.bills.infra.network.executeCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * B站 gRPC 客户端
 * 使用 HTTP/1.1 POST 发送 gRPC 请求（B站不使用标准 gRPC over HTTP/2）
 */
@Singleton
class BiliGrpcClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val grpcHeaderBuilder: GrpcHeaderBuilder
) {
    /**
     * 发送 gRPC 请求
     *
     * @param endpoint gRPC 服务路径，如 "bilibili.api.ticket.v1.Ticket/GetTicket"
     * @param requestBytes protobuf 序列化后的请求数据 (request.toByteArray())
     * @param parser protobuf 响应解析器 (Response.parser())
     * @param extraDeviceBin 自定义 device-bin（不传则使用默认）
     * @param cancelWithCaller 在调用协程取消时中止 HTTP 请求
     * @return 解析后的 protobuf 响应对象
     */
    suspend fun <Resp : MessageLite> call(
        endpoint: String,
        requestBytes: ByteArray,
        parser: Parser<Resp>,
        extraDeviceBin: ByteArray? = null,
        cancelWithCaller: Boolean = false
    ): Resp {
        val encodeResult = GrpcFrameCodec.encode(requestBytes)

        val headers = if (extraDeviceBin != null) {
            grpcHeaderBuilder.build(deviceBin = extraDeviceBin, compressed = encodeResult.compressed)
        } else {
            grpcHeaderBuilder.build(compressed = encodeResult.compressed)
        }

        val requestBuilder = Request.Builder()
            .url("${BiliConstants.BASE_URL_APP}/$endpoint")
            .post(encodeResult.frame.toRequestBody(null))

        headers.forEach { (key, value) -> requestBuilder.addHeader(key, value) }

        return withContext(Dispatchers.IO) {
            val call = okHttpClient.newCall(requestBuilder.build())
            val (responseBytes, grpcEncoding) = if (cancelWithCaller) {
                call.executeCancellable(::readResponse)
            } else {
                readResponse(call.execute())
            }
            val payload = GrpcFrameCodec.decode(responseBytes, grpcEncoding)
            parser.parseFrom(payload)
        }
    }

    private fun readResponse(response: Response): Pair<ByteArray, String?> {
        if (response.code != 200) {
            throw BiliApiException(response.code, "HTTP ${response.code}: ${response.message}")
        }

        val body = response.body?.bytes()
            ?: throw BiliApiException(-1, "Empty gRPC response")
        return body to response.header("grpc-encoding")
    }
}
