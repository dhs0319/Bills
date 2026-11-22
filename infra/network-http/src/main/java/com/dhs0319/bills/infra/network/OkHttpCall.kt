package com.dhs0319.bills.infra.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Response

// Register cancellation before executing so it can interrupt the body read on the I/O worker.
suspend fun <T> Call.executeCancellable(read: (Response) -> T): T =
    withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { cancel() }
            continuation.resumeWith(runCatching { execute().use(read) })
        }
    }
