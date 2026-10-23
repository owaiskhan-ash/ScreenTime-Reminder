package online.justsimpleapps.screentime.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

enum class Textile { FELT, LEATHER, SUEDE }

/** Logo-inspired smooth panels. No repeating bitmap grain or random texture. */
fun Modifier.textile(base: Color, material: Textile = Textile.FELT, stitched: Boolean = false): Modifier =
    drawWithCache {
        val wash = Brush.linearGradient(listOf(Color.White.copy(alpha = if (material == Textile.LEATHER) .035f else .015f), Color.Transparent, Color.Black.copy(alpha = .07f)))
        val seam = Stroke(width = .75.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())))
        onDrawBehind {
            drawRect(base)
            drawRect(wash)
            if (stitched && size.minDimension > 32.dp.toPx()) {
                val inset = 9.dp.toPx()
                val area = Size(size.width - 2 * inset, size.height - 2 * inset)
                val radius = CornerRadius(13.dp.toPx())
                // Recess under the dark teal thread.
                drawRoundRect(Color.Black.copy(alpha = .38f), Offset(inset, inset + 1.dp.toPx()), area, radius, style = seam)
                drawRoundRect(Color(0xFF102C2C).copy(alpha = .65f), Offset(inset, inset), area, radius, style = seam)
                drawRoundRect(Color.White.copy(alpha = .13f), Offset(.5.dp.toPx(), .5.dp.toPx()), Size(size.width - 1.dp.toPx(), size.height - 1.dp.toPx()), CornerRadius(21.dp.toPx()), style = Stroke(1.dp.toPx()))
            }
        }
    }
