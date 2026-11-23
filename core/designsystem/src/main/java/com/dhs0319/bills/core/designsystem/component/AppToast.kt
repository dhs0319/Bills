package com.dhs0319.bills.core.designsystem.component

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.TextUtils
import android.view.Gravity
import android.view.accessibility.AccessibilityManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.doOnAttach
import androidx.core.view.doOnDetach
import kotlin.math.roundToInt

private const val TOAST_TIMEOUT_MILLIS = 1_500

// App messages are shown in the foreground, where custom toast views are supported.
@Suppress("DEPRECATION")
fun Context.showAppToast(message: CharSequence) {
    val configuration = resources.configuration
    val density = resources.displayMetrics.density
    val horizontalPadding = (20 * density).roundToInt()
    val verticalPadding = (10 * density).roundToInt()
    val maxWidthDp = (configuration.screenWidthDp - 32).coerceIn(1, 320)
    val bottomOffset = (configuration.screenHeightDp * density / 7f).roundToInt()
    val toastView = TextView(applicationContext).apply {
        text = message
        textSize = 16f
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        maxWidth = (maxWidthDp * density).roundToInt()
        maxLines = 2
        ellipsize = TextUtils.TruncateAt.END
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        background = GradientDrawable().apply {
            cornerRadius = 26 * density
            setColor(Color.argb(204, 0, 0, 0))
        }
    }
    val toast = Toast(applicationContext).apply {
        view = toastView
        setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, bottomOffset)
    }
    val timeoutMillis = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        getSystemService(AccessibilityManager::class.java)?.getRecommendedTimeoutMillis(
            TOAST_TIMEOUT_MILLIS,
            AccessibilityManager.FLAG_CONTENT_TEXT
        ) ?: TOAST_TIMEOUT_MILLIS
    } else {
        TOAST_TIMEOUT_MILLIS
    }
    toastView.doOnAttach { view ->
        val dismiss = Runnable(toast::cancel)
        view.postDelayed(dismiss, timeoutMillis.toLong())
        view.doOnDetach { it.removeCallbacks(dismiss) }
    }
    toast.show()
}
