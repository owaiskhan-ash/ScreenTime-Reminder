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
import kotlin.random.Random

enum class Textile { FELT, LEATHER, SUEDE }

/** Cached, deterministic fibres: no bitmap downloads and no per-tick random grain. */
fun Modifier.textile(base: Color, material: Textile = Textile.FELT, stitched: Boolean = false): Modifier =
    drawWithCache {
        val random = Random(42)
        val count = ((size.width * size.height / 100f).toInt()).coerceIn(100, 14000)
        val fibres = List(count) {
            val start = Offset(random.nextFloat() * size.width, random.nextFloat() * size.height)
            val end = start + Offset(random.nextFloat() * 2.dp.toPx() - 1.dp.toPx(), random.nextFloat() * 3.dp.toPx())
            Triple(start, end, random.nextBoolean())
        }
        val wash = Brush.linearGradient(listOf(Color.White.copy(alpha = 0.055f), Color.Transparent, Color.Black.copy(alpha = 0.15f)))
        val seam = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx())))
        onDrawBehind {
            drawRect(base)
            drawRect(wash)
            fibres.forEach { (start, end, light) ->
                val ink = if (light) Color.White else Color.Black
                if (material == Textile.LEATHER) {
                    drawCircle(ink.copy(alpha = 0.065f), 0.65.dp.toPx(), start)
                } else {
                    drawLine(ink.copy(alpha = if (material == Textile.SUEDE) 0.055f else 0.085f), start, end, 0.45.dp.toPx())
                }
            }
            if (stitched && size.minDimension > 24.dp.toPx()) {
                val inset = 8.dp.toPx()
                drawRoundRect(Color.White.copy(alpha = 0.22f), Offset(inset, inset), Size(size.width - inset * 2, size.height - inset * 2), CornerRadius(13.dp.toPx()), style = seam)
                drawRoundRect(Color.White.copy(alpha = 0.12f), Offset(0.5.dp.toPx(), 0.5.dp.toPx()), Size(size.width - 1.dp.toPx(), size.height - 1.dp.toPx()), CornerRadius(21.dp.toPx()), style = Stroke(1.dp.toPx()))
            }
        }
    }
