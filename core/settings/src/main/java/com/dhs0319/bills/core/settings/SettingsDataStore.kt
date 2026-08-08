package com.dhs0319.bills.core.settings

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

internal val Context.appSettingsDataStore by preferencesDataStore("app_settings")
