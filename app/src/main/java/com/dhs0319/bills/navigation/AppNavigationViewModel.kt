package com.dhs0319.bills.navigation

import androidx.lifecycle.ViewModel
import com.dhs0319.bills.core.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppNavigationViewModel @Inject constructor(
    private val authRepo: AuthRepository
) : ViewModel() {
    val isLoggedIn: Boolean
        get() = authRepo.currentMidFlow.value > 0L
}
