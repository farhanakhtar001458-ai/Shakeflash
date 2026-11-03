package com.shakeflash.app.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * The handful of glyphs Material's *core* icon set does not ship. Drawn as vectors so the
 * app does not need the multi-megabyte material-icons-extended artifact.
 * Every icon is a white 24dp glyph; callers tint it with `Icon(tint = ...)`.
 */
object SFIcons {

    private fun icon(name: String, builder: ImageVector.Builder.() -> ImageVector.Builder): ImageVector =
        builder(
            ImageVector.Builder(
                name = name,
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f,
            ),
        ).build()

    val Flashlight: ImageVector by lazy {
        icon("Flashlight") { b ->
            b.path(fill = SolidColor(Color.White)) {
                moveTo(7f, 2f)
                verticalLineToRelative(11f)
                horizontalLineToRelative(3f)
                verticalLineToRelative(9f)
                lineToRelative(7f, -12f)
                horizontalLineToRelative(-4f)
                lineToRelative(4f, -8f)
                close()
            }
        }
    }

    val Power: ImageVector by lazy {
        icon("Power") { b ->
            b.path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(12f, 3.5f)
                lineTo(12f, 11f)
            }
            b.path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(7.4f, 6.3f)
                curveTo(5.3f, 7.7f, 4f, 10f, 4f, 12.6f)
                curveTo(4f, 17f, 7.6f, 20.6f, 12f, 20.6f)
                curveTo(16.4f, 20.6f, 20f, 17f, 20f, 12.6f)
                curveTo(20f, 10f, 18.7f, 7.7f, 16.6f, 6.3f)
            }
        }
    }

    val Wave: ImageVector by lazy {
        icon("Wave") { b ->
            b.path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(2.5f, 12f)
                lineTo(6f, 12f)
                lineTo(8.5f, 6.5f)
                lineTo(11.5f, 17.5f)
                lineTo(14f, 10f)
                lineTo(16f, 14f)
                lineTo(21.5f, 14f)
            }
        }
    }

    val Shield: ImageVector by lazy {
        icon("Shield") { b ->
            b.path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 2.6f)
                lineTo(20f, 5.6f)
                lineTo(20f, 11.2f)
                curveTo(20f, 16f, 16.6f, 20f, 12f, 21.6f)
                curveTo(7.4f, 20f, 4f, 16f, 4f, 11.2f)
                lineTo(4f, 5.6f)
                close()
            }
        }
    }

    val Battery: ImageVector by lazy {
        icon("Battery") { b ->
            b.path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.7f,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3f, 7.5f)
                lineTo(17f, 7.5f)
                lineTo(17f, 16.5f)
                lineTo(3f, 16.5f)
                close()
                moveTo(19.5f, 10f)
                lineTo(19.5f, 14f)
            }
            b.path(fill = SolidColor(Color.White)) {
                moveTo(5.5f, 10.5f)
                lineTo(11f, 10.5f)
                lineTo(11f, 13.5f)
                lineTo(5.5f, 13.5f)
                close()
            }
        }
    }

    val ChevronRight: ImageVector by lazy {
        icon("ChevronRight") { b ->
            b.path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(9f, 5f)
                lineTo(16f, 12f)
                lineTo(9f, 19f)
            }
        }
    }

    val Bolt: ImageVector by lazy {
        icon("Bolt") { b ->
            b.path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(13f, 2.5f)
                lineTo(4.5f, 13.5f)
                lineTo(11f, 13.5f)
                lineTo(10f, 21.5f)
                lineTo(19.5f, 10.5f)
                lineTo(13f, 10.5f)
                close()
            }
        }
    }
}
