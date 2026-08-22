package com.dhs0319.bills.feature.settings.audioVideo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhs0319.bills.core.designsystem.component.CollapsingTopBarScaffold
import com.dhs0319.bills.feature.settings.SettingsViewModel
import com.dhs0319.bills.feature.settings.components.SettingDropdown
import com.dhs0319.bills.feature.settings.components.SettingSwitch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioVideoSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val enableHdrAnd8k by viewModel.enableHdrAnd8k.collectAsStateWithLifecycle()
    val defaultVideoQuality by viewModel.defaultVideoQuality.collectAsStateWithLifecycle()
    val defaultAudioQuality by viewModel.defaultAudioQuality.collectAsStateWithLifecycle()
    val forceHost by viewModel.forceHost.collectAsStateWithLifecycle()
    val needTrial by viewModel.needTrial.collectAsStateWithLifecycle()
    val preferredCodec by viewModel.preferredCodec.collectAsStateWithLifecycle()
    val enableWebPlayback by viewModel.enableWebPlayback.collectAsStateWithLifecycle()
    val playerSettings by viewModel.playerSettings.collectAsStateWithLifecycle()

    CollapsingTopBarScaffold(
        topBar = { scrollBehavior ->
            TopAppBar(
                title = {
                    Text("播放器设置")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingCategory(title = "画质")

            SettingSwitch(
                title = "启用 HDR 和 8K",
                subtitle = "允许请求 HDR 和 8K 视频流（如果视频支持）",
                checked = enableHdrAnd8k,
                onCheckedChange = viewModel::updateEnableHdrAnd8k
            )

            SettingCategory(title = "本地选择")

            SettingDropdown(
                title = "默认视频画质",
                selected = defaultVideoQuality,
                options = VIDEO_QUALITIES,
                optionLabel = ::getVideoQualityName,
                onSelect = viewModel::updateDefaultVideoQuality
            )

            SettingDropdown(
                title = "默认音频质量",
                selected = defaultAudioQuality,
                options = AUDIO_QUALITIES,
                optionLabel = ::getAudioQualityName,
                onSelect = viewModel::updateDefaultAudioQuality
            )

            SettingCategory(title = "其他")

            SettingDropdown(
                title = "优先编码格式",
                selected = preferredCodec,
                options = CODECS,
                optionLabel = ::getCodecName,
                onSelect = viewModel::updatePreferredCodec
            )

            SettingSwitch(
                title = "软解优先",
                subtitle = "优先使用软件解码",
                checked = playerSettings.playback.preferSoftwareDecode,
                onCheckedChange = viewModel::updatePreferSoftwareDecode
            )

            SettingSwitch(
                title = "解码失败自动回退",
                subtitle = "允许切换到低优先级解码器",
                checked = playerSettings.playback.decoderFallback,
                onCheckedChange = viewModel::updateDecoderFallback
            )

            SettingSwitch(
                title = "使用https播放",
                checked = forceHost > 0,
                onCheckedChange = { viewModel.updateForceHost(if (it) 1 else 0) }
            )

            SettingSwitch(
                title = "需要4k",
                checked = needTrial,
                onCheckedChange = viewModel::updateNeedTrial
            )

            SettingSwitch(
                title = "免登录看1080p",
                subtitle = "打开就算不登录也能看1080p喵",
                checked = enableWebPlayback,
                onCheckedChange = viewModel::updateEnableWebPlayback
            )

            SettingCategory(title = "播放行为")

            SettingSwitch(
                title = "后台播放",
                subtitle = "退出页面或切到后台后继续播放，并显示系统通知",
                checked = playerSettings.playback.backgroundPlayback,
                onCheckedChange = viewModel::updateBackgroundPlayback
            )

            SettingSwitch(
                title = "应用内小窗",
                subtitle = "允许把视频和直播缩成应用内小窗继续播放",
                checked = playerSettings.playback.inAppMiniPlayer,
                onCheckedChange = viewModel::updateInAppMiniPlayer
            )

            SettingSwitch(
                title = "播放行为上报",
                subtitle = "向服务端上报播放心跳和历史，关闭影响个性化推荐和历史记录",
                checked = playerSettings.playback.reportPlayback,
                onCheckedChange = viewModel::updateReportPlayback
            )

            SettingSwitch(
                title = "全屏自动横屏",
                subtitle = "点击全屏按钮时自动强制横屏",
                checked = playerSettings.playback.autoRotateFullscreen,
                onCheckedChange = viewModel::updateAutoRotateFullscreen
            )
        }

    }
}

@Composable
private fun SettingCategory(title: String) {
    Text(
        text = title,
        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
        color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

private fun getVideoQualityName(quality: Int): String {
    return when (quality) {
        16 -> "360P"
        32 -> "480P"
        64 -> "720P"
        80 -> "1080P"
        112 -> "1080P+"
        116 -> "1080P 60fps"
        120 -> "4K"
        125 -> "HDR"
        126 -> "杜比视界"
        127 -> "8K"
        else -> "未知"
    }
}

private fun getAudioQualityName(quality: Int): String {
    return when (quality) {
        0 -> "自动"
        30216 -> "64K"
        30232 -> "132K"
        30280 -> "192K"
        30250 -> "杜比全景声"
        30251 -> "Hi-Res 无损"
        else -> "未知"
    }
}

private fun getCodecName(codec: Int): String {
    return when (codec) {
        1 -> "AVC/H.264"
        2 -> "HEVC/H.265"
        3 -> "AV1"
        else -> "未知"
    }
}

private val VIDEO_QUALITIES = listOf(16, 32, 64, 80, 112, 116, 120, 125, 126, 127)
private val AUDIO_QUALITIES = listOf(0, 30216, 30232, 30280, 30250, 30251)
private val CODECS = listOf(1, 2, 3)
