package com.dhs0319.bills.infra.network

import java.net.ConnectException
import java.net.InetAddress
import java.net.Proxy
import java.net.UnknownHostException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class NetworkFailureEventsTest {
    private val request = Request.Builder().url("https://example.invalid/api").build()

    @Test
    fun publishesDnsFailureAndPropagatesTheException() = runBlocking {
        val events = NetworkFailureEvents()
        val failure = async(start = CoroutineStart.UNDISPATCHED) { events.failures.first() }
        val client = OkHttpClient.Builder()
            .proxy(Proxy.NO_PROXY)
            .dns(object : Dns {
                override fun lookup(hostname: String): List<InetAddress> {
                    throw UnknownHostException(hostname)
                }
            })
            .eventListener(events)
            .build()

        assertThrows(UnknownHostException::class.java) {
            client.newCall(request).execute()
        }
        withTimeout(2_000) { failure.await() }
    }

    @Test
    fun ignoresHttpResponsesAndCanceledCalls() = runBlocking {
        val events = NetworkFailureEvents()
        val failure = async(start = CoroutineStart.UNDISPATCHED) { events.failures.first() }
        val client = OkHttpClient.Builder()
            .eventListener(events)
            .addInterceptor { chain ->
                if (chain.call().isCanceled()) throw ConnectException("Socket closed")
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(503)
                    .message("Service Unavailable")
                    .body("".toResponseBody())
                    .build()
            }
            .build()

        client.newCall(request).execute().use { assertEquals(503, it.code) }
        val canceledCall = client.newCall(request).apply { cancel() }
        assertThrows(ConnectException::class.java) { canceledCall.execute() }
        yield()

        assertFalse(failure.isCompleted)
        failure.cancel()
    }
}
