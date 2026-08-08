package com.dhs0319.bills.feature.settings.errorlog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhs0319.bills.core.common.log.ErrorCollector
import com.dhs0319.bills.core.common.log.ErrorLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ErrorLogViewModel @Inject constructor() : ViewModel() {

    val logs: StateFlow<List<ErrorLog>> = ErrorCollector.flow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun clear() = ErrorCollector.clear()
}
