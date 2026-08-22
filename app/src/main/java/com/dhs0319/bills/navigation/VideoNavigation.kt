package com.dhs0319.bills.navigation

import android.net.Uri
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhs0319.bills.core.model.SpaceRoute
import com.dhs0319.bills.core.model.StreamPlaybackTarget
import com.dhs0319.bills.core.model.VideoDownloadRequest
import com.dhs0319.bills.core.model.VideoSrc
import com.dhs0319.bills.core.model.VideoTarget
import com.dhs0319.bills.core.model.VideoTargetTool
import com.dhs0319.bills.core.model.isSameEntry
import com.dhs0319.bills.feature.video.VideoScreen
import com.dhs0319.bills.feature.video.VideoViewModel
import com.dhs0319.bills.playback.PlaybackHostViewModel

const val VIDEO_ROUTE = "video"

private const val VIDEO_KIND_ARG = "kind"
private const val VIDEO_AID_ARG = "aid"
private const val VIDEO_CID_ARG = "cid"
private const val VIDEO_BVID_ARG = "bvid"
private const val VIDEO_EP_ID_ARG = "epId"
private const val VIDEO_SEASON_ID_ARG = "seasonId"
private const val VIDEO_SUB_TYPE_ARG = "subType"
private const val VIDEO_FROM_ARG = "from"
private const val VIDEO_FROM_SPMID_ARG = "fromSpmid"
private const val VIDEO_TRACK_ID_ARG = "trackId"
private const val VIDEO_REPORT_FLOW_DATA_ARG = "reportFlowData"

private const val VIDEO_ROUTE_PATTERN =
    "$VIDEO_ROUTE?$VIDEO_KIND_ARG={$VIDEO_KIND_ARG}" +
        "&$VIDEO_AID_ARG={$VIDEO_AID_ARG}" +
        "&$VIDEO_CID_ARG={$VIDEO_CID_ARG}" +
        "&$VIDEO_BVID_ARG={$VIDEO_BVID_ARG}" +
        "&$VIDEO_EP_ID_ARG={$VIDEO_EP_ID_ARG}" +
        "&$VIDEO_SEASON_ID_ARG={$VIDEO_SEASON_ID_ARG}" +
        "&$VIDEO_SUB_TYPE_ARG={$VIDEO_SUB_TYPE_ARG}" +
        "&$VIDEO_FROM_ARG={$VIDEO_FROM_ARG}" +
        "&$VIDEO_FROM_SPMID_ARG={$VIDEO_FROM_SPMID_ARG}" +
        "&$VIDEO_TRACK_ID_ARG={$VIDEO_TRACK_ID_ARG}" +
        "&$VIDEO_REPORT_FLOW_DATA_ARG={$VIDEO_REPORT_FLOW_DATA_ARG}"

fun NavController.navigateToVideo(target: VideoTarget) {
    navigate(target.toRoute())
}

fun NavGraphBuilder.videoScreen(
    videoViewModel: VideoViewModel,
    playbackHostViewModel: PlaybackHostViewModel,
    onBack: () -> Unit,
    onGoHome: () -> Unit,
    onOpenSpace: (SpaceRoute) -> Unit,
    onOpenDownloadCache: () -> Unit,
    onStartDownload: (VideoDownloadRequest) -> Unit,
) {
    composable(
        route = VIDEO_ROUTE_PATTERN,
        arguments = videoNavArguments,
    ) { entry ->
        val target = entry.toVideoTarget()
        val currentTarget by playbackHostViewModel.currentTarget.collectAsStateWithLifecycle()
        val initialized = entry.savedStateHandle[VIDEO_INITIALIZED_KEY] ?: false

        DisposableEffect(entry) {
            val observer = LifecycleEventObserver { _, event ->
                if (
                    event == Lifecycle.Event.ON_RESUME &&
                    entry.savedStateHandle.remove<Boolean>(VIDEO_RESUME_PLAYBACK_KEY) == true
                ) {
                    videoViewModel.resume()
                }
            }
            entry.lifecycle.addObserver(observer)
            onDispose {
                entry.lifecycle.removeObserver(observer)
            }
        }

        LaunchedEffect(target, currentTarget, initialized) {
            val playingTarget = (currentTarget as? StreamPlaybackTarget.Video)?.target
            if (!initialized || playingTarget == null) {
                if (!target.isSameEntry(playingTarget)) {
                    videoViewModel.openRoot(target)
                }
                entry.savedStateHandle[VIDEO_INITIALIZED_KEY] = true
            }
        }

        VideoScreen(
            onBack = onBack,
            onGoHome = onGoHome,
            onOpenSpace = { route ->
                entry.savedStateHandle[VIDEO_RESUME_PLAYBACK_KEY] = videoViewModel.pause()
                onOpenSpace(route)
            },
            onOpenDownloadCache = onOpenDownloadCache,
            onStartDownload = onStartDownload,
            viewModel = videoViewModel,
        )
    }
}

private const val VIDEO_INITIALIZED_KEY = "video_initialized"
private const val VIDEO_RESUME_PLAYBACK_KEY = "video_resume_playback"

private val videoNavArguments = listOf(
    navArgument(VIDEO_KIND_ARG) { type = NavType.StringType },
    navArgument(VIDEO_AID_ARG) { type = NavType.LongType; defaultValue = 0L },
    navArgument(VIDEO_CID_ARG) { type = NavType.LongType; defaultValue = 0L },
    navArgument(VIDEO_BVID_ARG) { type = NavType.StringType; defaultValue = "" },
    navArgument(VIDEO_EP_ID_ARG) { type = NavType.LongType; defaultValue = 0L },
    navArgument(VIDEO_SEASON_ID_ARG) { type = NavType.LongType; defaultValue = -1L },
    navArgument(VIDEO_SUB_TYPE_ARG) { type = NavType.IntType; defaultValue = -1 },
    navArgument(VIDEO_FROM_ARG) { type = NavType.StringType; defaultValue = VideoTargetTool.FROM_FEED },
    navArgument(VIDEO_FROM_SPMID_ARG) { type = NavType.StringType; defaultValue = VideoTargetTool.FROM_SPMID_FEED },
    navArgument(VIDEO_TRACK_ID_ARG) { type = NavType.StringType; defaultValue = "" },
    navArgument(VIDEO_REPORT_FLOW_DATA_ARG) { type = NavType.StringType; defaultValue = "" },
)

private fun VideoTarget.toRoute(): String {
    val values = when (this) {
        is VideoTarget.Ugc -> VideoRouteValues("ugc", aid, cid, bvid.orEmpty(), 0L, -1L, -1)
        is VideoTarget.Pgc -> VideoRouteValues("pgc", aid, cid, "", epId, seasonId ?: -1L, subType ?: -1)
        is VideoTarget.Pugv -> VideoRouteValues("pugv", aid, 0L, "", epId, seasonId ?: -1L, -1)
    }
    return "$VIDEO_ROUTE?$VIDEO_KIND_ARG=${values.kind}" +
        "&$VIDEO_AID_ARG=${values.aid}" +
        "&$VIDEO_CID_ARG=${values.cid}" +
        "&$VIDEO_BVID_ARG=${values.bvid.encode()}" +
        "&$VIDEO_EP_ID_ARG=${values.epId}" +
        "&$VIDEO_SEASON_ID_ARG=${values.seasonId}" +
        "&$VIDEO_SUB_TYPE_ARG=${values.subType}" +
        "&$VIDEO_FROM_ARG=${src.from.encode()}" +
        "&$VIDEO_FROM_SPMID_ARG=${src.fromSpmid.encode()}" +
        "&$VIDEO_TRACK_ID_ARG=${src.trackId.orEmpty().encode()}" +
        "&$VIDEO_REPORT_FLOW_DATA_ARG=${src.reportFlowData.orEmpty().encode()}"
}

private data class VideoRouteValues(
    val kind: String,
    val aid: Long,
    val cid: Long,
    val bvid: String,
    val epId: Long,
    val seasonId: Long,
    val subType: Int,
)

private fun NavBackStackEntry.toVideoTarget(): VideoTarget {
    val args = arguments ?: error("Missing video route arguments")
    val src = VideoSrc(
        from = args.getString(VIDEO_FROM_ARG).orEmpty(),
        fromSpmid = args.getString(VIDEO_FROM_SPMID_ARG).orEmpty(),
        trackId = args.getString(VIDEO_TRACK_ID_ARG).orEmpty().ifBlank { null },
        reportFlowData = args.getString(VIDEO_REPORT_FLOW_DATA_ARG).orEmpty().ifBlank { null },
    )
    val aid = args.getLong(VIDEO_AID_ARG)
    val cid = args.getLong(VIDEO_CID_ARG)
    val epId = args.getLong(VIDEO_EP_ID_ARG)
    val seasonId = args.getLong(VIDEO_SEASON_ID_ARG).takeIf { it >= 0L }
    return when (args.getString(VIDEO_KIND_ARG)) {
        "ugc" -> VideoTarget.Ugc(
            aid = aid,
            cid = cid,
            bvid = args.getString(VIDEO_BVID_ARG).orEmpty().ifBlank { null },
            src = src,
        )

        "pgc" -> VideoTarget.Pgc(
            aid = aid,
            cid = cid,
            epId = epId,
            seasonId = seasonId,
            subType = args.getInt(VIDEO_SUB_TYPE_ARG).takeIf { it >= 0 },
            src = src,
        )

        "pugv" -> VideoTarget.Pugv(
            aid = aid,
            epId = epId,
            seasonId = seasonId,
            src = src,
        )

        else -> error("Unsupported video route")
    }
}

private fun String.encode(): String = Uri.encode(this)
