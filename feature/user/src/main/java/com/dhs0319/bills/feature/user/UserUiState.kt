package com.dhs0319.bills.feature.user

import androidx.compose.runtime.Immutable
import com.dhs0319.bills.core.model.User

@Immutable
data class UserUiState(
    val user: User? = null,
    val showAccountExpiredDialog: Boolean = false
)
