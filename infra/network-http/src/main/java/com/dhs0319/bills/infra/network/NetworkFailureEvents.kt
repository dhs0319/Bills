package com.dhs0319.bills.infra.network

import com.dhs0319.bills.core.common.network.isNetworkCallTimeout
import com.dhs0319.bills.core.common.network.isNetworkConnectionFailure
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.Call
import okhttp3.EventListener

@Singleton
class NetworkFailureEvents @Inject constructor() : EventListener() {
    private val _failures = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val failures = _failures.asSharedFlow()

    override fun callFailed(call: Call, ioe: IOException) {
        if (call.isCanceled() && !ioe.isNetworkCallTimeout()) return
        if (ioe.isNetworkConnectionFailure()) {
            _failures.tryEmit(Unit)
        }
    }
}
