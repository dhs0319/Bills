package com.dhs0319.bills.core.designsystem.icon

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlined.Backspace
import com.composables.icons.materialsymbols.outlined.Open_in_new
import com.composables.icons.materialsymbols.outlinedfilled.Arrow_back
import com.composables.icons.materialsymbols.outlinedfilled.Chat
import com.composables.icons.materialsymbols.outlinedfilled.Exit_to_app
import com.composables.icons.materialsymbols.outlinedfilled.Keyboard_arrow_right
import com.composables.icons.materialsymbols.outlinedfilled.Send
import com.composables.icons.materialsymbols.outlinedfilled.Volume_up

object MirroredSymbols {
    val ArrowBack by lazy { MaterialSymbols.OutlinedFilled.Arrow_back.withAutoMirroring() }
    val Backspace by lazy { MaterialSymbols.Outlined.Backspace.withAutoMirroring() }
    val Chat by lazy { MaterialSymbols.OutlinedFilled.Chat.withAutoMirroring() }
    val ExitToApp by lazy { MaterialSymbols.OutlinedFilled.Exit_to_app.withAutoMirroring() }
    val KeyboardArrowRight by lazy { MaterialSymbols.OutlinedFilled.Keyboard_arrow_right.withAutoMirroring() }
    val OpenInNew by lazy { MaterialSymbols.Outlined.Open_in_new.withAutoMirroring() }
    val Send by lazy { MaterialSymbols.OutlinedFilled.Send.withAutoMirroring() }
    val VolumeUp by lazy { MaterialSymbols.OutlinedFilled.Volume_up.withAutoMirroring() }
}

// The Symbols library does not mark directional vectors as auto-mirrored.
private fun ImageVector.withAutoMirroring(): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = defaultWidth,
        defaultHeight = defaultHeight,
        viewportWidth = viewportWidth,
        viewportHeight = viewportHeight,
        tintColor = tintColor,
        tintBlendMode = tintBlendMode,
        autoMirror = true
    ).apply { addVectorGroup(root) }.build()

private fun ImageVector.Builder.addVectorGroup(group: VectorGroup) {
    addGroup(
        name = group.name,
        rotate = group.rotation,
        pivotX = group.pivotX,
        pivotY = group.pivotY,
        scaleX = group.scaleX,
        scaleY = group.scaleY,
        translationX = group.translationX,
        translationY = group.translationY,
        clipPathData = group.clipPathData
    )
    group.forEach { node ->
        when (node) {
            is VectorGroup -> addVectorGroup(node)
            is VectorPath -> addPath(
                pathData = node.pathData,
                pathFillType = node.pathFillType,
                name = node.name,
                fill = node.fill,
                fillAlpha = node.fillAlpha,
                stroke = node.stroke,
                strokeAlpha = node.strokeAlpha,
                strokeLineWidth = node.strokeLineWidth,
                strokeLineCap = node.strokeLineCap,
                strokeLineJoin = node.strokeLineJoin,
                strokeLineMiter = node.strokeLineMiter,
                trimPathStart = node.trimPathStart,
                trimPathEnd = node.trimPathEnd,
                trimPathOffset = node.trimPathOffset
            )
        }
    }
    clearGroup()
}
