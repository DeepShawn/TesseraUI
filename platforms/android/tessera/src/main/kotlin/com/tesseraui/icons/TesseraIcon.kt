package com.tesseraui.icons

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tesseraui.LocalContentColor
import com.tesseraui.Tessera

/** 图标里的一条路径：填充或描边（描边宽 1.5，圆头圆角） */
internal class P(val d: String, val fill: Boolean, val stroke: Boolean, val evenOdd: Boolean = false)

internal fun build(name: String, vararg parts: P): ImageVector {
    val b = ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
    for (p in parts) {
        b.addPath(
            pathData = PathParser().parsePathString(p.d).toNodes(),
            pathFillType = if (p.evenOdd) PathFillType.EvenOdd else PathFillType.NonZero,
            fill = if (p.fill) SolidColor(Color.Black) else null,
            stroke = if (p.stroke) SolidColor(Color.Black) else null,
            strokeLineWidth = if (p.stroke) 1.5f else 0f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    return b.build()
}

/**
 * 图标：单色，默认跟随当前内容颜色。
 * contentDescription 为 null 时视为装饰图标，读屏软件会跳过。
 */
@Composable
fun TesseraIcon(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    size: Dp = 20.dp,
) {
    val c = when {
        tint != Color.Unspecified -> tint
        LocalContentColor.current != Color.Unspecified -> LocalContentColor.current
        else -> Tessera.colors.textPrimary
    }
    Image(
        painter = rememberVectorPainter(icon),
        contentDescription = null,
        colorFilter = ColorFilter.tint(c),
        modifier = modifier.size(size).then(
            if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription; role = Role.Image } else Modifier,
        ),
    )
}
