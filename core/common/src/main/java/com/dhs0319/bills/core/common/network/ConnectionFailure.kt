package com.dhs0319.bills.core.common.network

import java.io.EOFException
import java.io.InterruptedIOException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

// OkHttp cancels the call and wraps the terminal failure when its overall timeout expires.
fun Throwable.isNetworkCallTimeout(): Boolean = this is InterruptedIOException && message == "timeout"

fun Throwable.isNetworkConnectionFailure(): Boolean = generateSequence(this) { it.cause }.any {
    it is UnknownHostException || it is SocketException || it is SocketTimeoutException ||
        it is SSLException || it is EOFException || it.isNetworkCallTimeout()
}
