package com.dhs0319.bills.core.feed

import com.dhs0319.bills.infra.crypto.BiliSessionId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class PageActionTracker @Inject constructor() {
    private val _actionId = MutableStateFlow(BiliSessionId.polarisAction())
    val actionId: StateFlow<String> = _actionId.asStateFlow()

    fun currentActionId(): String {
        return _actionId.value
    }

    fun refresh() {
        _actionId.value = BiliSessionId.polarisAction()
    }
}
