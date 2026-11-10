package com.dhs0319.bills.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhs0319.bills.core.auth.AuthRepository
import com.dhs0319.bills.core.common.log.Logger
import com.dhs0319.bills.core.im.ImRepository
import com.dhs0319.bills.core.model.ImUnreadBadge
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@HiltViewModel
class MessageUnreadViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val imRepo: ImRepository
) : ViewModel() {
    private val isActive = MutableStateFlow(false)
    private val _badge = MutableStateFlow<ImUnreadBadge>(ImUnreadBadge.None)
    val badge = _badge.asStateFlow()
    private var latestUnread: ImUnreadBadge? = null
    private var isBadgeDismissed = false
    private var dismissalVersion = 0L

    init {
        viewModelScope.launch {
            authRepo.currentMidFlow
                .onEach {
                    latestUnread = null
                    isBadgeDismissed = false
                    _badge.value = ImUnreadBadge.None
                }
                .collectLatest account@{ mid ->
                    if (mid <= 0L) return@account
                    isActive.collectLatest visibility@{ active ->
                        if (!active) return@visibility
                        merge(
                            // StateFlow emits immediately on each foreground entry.
                            imRepo.sessionListVersion.map { Unit },
                            flow {
                                while (true) {
                                    delay(REFRESH_INTERVAL_MILLIS)
                                    emit(Unit)
                                }
                            }
                        ).conflate().collect {
                            refresh(mid)
                        }
                    }
                }
        }
    }

    fun setActive(active: Boolean) {
        isActive.value = active
    }

    fun dismissBadge() {
        dismissalVersion++
        isBadgeDismissed = true
        _badge.value = ImUnreadBadge.None
    }

    private suspend fun refresh(mid: Long) {
        val requestDismissalVersion = dismissalVersion
        try {
            val unread = imRepo.fetchTotalUnread()
            if (authRepo.currentMidFlow.value == mid) {
                val previousUnread = latestUnread
                val hasNewUnread = when {
                    previousUnread == null -> false
                    previousUnread is ImUnreadBadge.Count && unread is ImUnreadBadge.Count ->
                        unread.count > previousUnread.count
                    else -> unread != previousUnread
                }
                // A pending response or the same unread count must not undo a click.
                if (unread == ImUnreadBadge.None ||
                    (requestDismissalVersion == dismissalVersion && hasNewUnread)
                ) {
                    isBadgeDismissed = false
                }
                latestUnread = unread
                _badge.value = if (isBadgeDismissed) ImUnreadBadge.None else unread
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Logger.e("MessageUnreadViewModel", error) { "刷新消息未读数失败" }
        }
    }

    private companion object {
        const val REFRESH_INTERVAL_MILLIS = 120_000L
    }
}
