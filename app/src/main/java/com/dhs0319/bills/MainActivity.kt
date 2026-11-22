package com.dhs0319.bills

import android.content.Intent
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.view.Display
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dhs0319.bills.core.settings.AppSettings
import com.dhs0319.bills.core.settings.update.AppUpdateCheckResult
import com.dhs0319.bills.core.settings.update.AppUpdateChecker
import com.dhs0319.bills.core.settings.update.toDialogState
import com.dhs0319.bills.core.designsystem.component.AppUpdateDialog
import com.dhs0319.bills.core.designsystem.component.AppUpdateDialogState
import com.dhs0319.bills.core.designsystem.component.NetworkErrorPopup
import com.dhs0319.bills.core.designsystem.theme.BiliTheme
import com.dhs0319.bills.core.designsystem.theme.FrameRateMode
import com.dhs0319.bills.core.designsystem.theme.ThemeConfig
import com.dhs0319.bills.core.model.WebLinkParser
import com.dhs0319.bills.core.model.WebLinkTarget
import com.dhs0319.bills.infra.network.NetworkFailureEvents
import com.dhs0319.bills.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var appSettings: AppSettings

    @Inject
    lateinit var updateChecker: AppUpdateChecker

    @Inject
    lateinit var networkFailureEvents: NetworkFailureEvents

    private var updateDialog by mutableStateOf<AppUpdateDialogState?>(null)

    private var cachedDisplayModes: Array<Display.Mode>? = null

    private var pendingAppLink by mutableStateOf<WebLinkTarget?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            autoCheckUpdate()
        }
        pendingAppLink = toAppLink(intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = Color.TRANSPARENT,
                darkScrim = Color.TRANSPARENT
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            val themeConfig by appSettings.themeConfig.collectAsState(initial = ThemeConfig())

            LaunchedEffect(themeConfig.preferredFrameRate) {
                applyFrameRate(themeConfig.preferredFrameRate)
            }
            BiliTheme(config = themeConfig) {
                ApplySystemBarAppearance()
                Box(Modifier.fillMaxSize()) {
                    AppNavHost(
                        themeConfig = themeConfig,
                        appLink = pendingAppLink,
                        onAppLinkConsumed = { pendingAppLink = null }
                    )
                    NetworkFailureOverlay(Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp))
                }
                updateDialog?.let { release ->
                    AppUpdateDialog(
                        state = release,
                        onDismiss = { updateDialog = null },
                        onOpenUrl = {
                            updateDialog = null
                            startActivity(Intent(Intent.ACTION_VIEW, it.toUri()))
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun NetworkFailureOverlay(modifier: Modifier) {
        val hostState = remember { SnackbarHostState() }
        LaunchedEffect(hostState) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                var popup: Job? = null
                networkFailureEvents.failures.collect {
                    if (popup?.isActive != true) {
                        popup = launch {
                            hostState.showSnackbar(
                                message = "网络连接失败，请检查网络设置",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                }
            }
        }
        NetworkErrorPopup(hostState, modifier)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingAppLink = toAppLink(intent)
    }

    private fun autoCheckUpdate() {
        lifecycleScope.launch {
            if (!appSettings.autoCheckUpdate.first()) return@launch
            if (!appSettings.tryBeginAutomaticUpdateCheck()) return@launch
            val result = updateChecker.check().getOrNull() ?: return@launch
            if (result is AppUpdateCheckResult.HasUpdate) {
                updateDialog = result.toDialogState()
            }
        }
    }

    private fun applyFrameRate(mode: FrameRateMode) {
        val attrs = window.attributes
        if (mode == FrameRateMode.AUTO) {
            attrs.preferredDisplayModeId = 0
            attrs.preferredRefreshRate = 0f
        } else {
            val targetMode = findBestDisplayMode(mode.value)
            if (targetMode != null) {
                attrs.preferredDisplayModeId = targetMode.modeId
            }
            attrs.preferredRefreshRate = mode.value
        }
        window.attributes = attrs
    }

    private fun findBestDisplayMode(targetHz: Float): Display.Mode? {
        val display = getSystemService(DisplayManager::class.java)
            ?.getDisplay(Display.DEFAULT_DISPLAY)
            ?: return null
        val currentMode = display.mode
        val w = currentMode.physicalWidth
        val h = currentMode.physicalHeight
        val modes = cachedDisplayModes ?: display.supportedModes
            ?.also { cachedDisplayModes = it }
            ?: return null
        return modes
            .filter { it.physicalWidth == w && it.physicalHeight == h }
            .minByOrNull { abs(it.refreshRate - targetHz) }
            ?.takeIf { abs(it.refreshRate - targetHz) < 5f }
    }

    private fun toAppLink(intent: Intent?): WebLinkTarget? {
        val url = intent?.dataString ?: return null
        return when (val target = WebLinkParser.parse(url)) {
            is WebLinkTarget.ToVideo,
            is WebLinkTarget.ToSpace,
            is WebLinkTarget.ToLive -> target
            is WebLinkTarget.External,
            is WebLinkTarget.Stay -> null
        }
    }

    @Composable
    private fun ApplySystemBarAppearance() {
        val view = LocalView.current
        val orientation = LocalConfiguration.current.orientation
        val density = LocalDensity.current
        val layoutDirection = LocalLayoutDirection.current
        val tappableInsets = WindowInsets.tappableElement
        val navigationBarInset = tappableInsets.getBottom(density) +
            tappableInsets.getLeft(density, layoutDirection) +
            tappableInsets.getRight(density, layoutDirection)
        val useDarkSystemBarContent = MaterialTheme.colorScheme.background.luminance() > 0.5f

        if (!view.isInEditMode) {
            LaunchedEffect(useDarkSystemBarContent, orientation, navigationBarInset) {
                applySystemBarAppearance(useDarkSystemBarContent)
                view.post { applySystemBarAppearance(useDarkSystemBarContent) }
            }
        }
    }

    private fun applySystemBarAppearance(useDarkSystemBarContent: Boolean) {
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = useDarkSystemBarContent
        insetsController.isAppearanceLightNavigationBars = useDarkSystemBarContent
    }
}
