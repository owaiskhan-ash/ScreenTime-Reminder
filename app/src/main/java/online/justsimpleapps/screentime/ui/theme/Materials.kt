package online.justsimpleapps.screentime.ui.theme

import android.graphics.BitmapFactory
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import online.justsimpleapps.screentime.R

enum class Textile { FELT, LEATHER, SUEDE }

/** Embedded material samples, cached once per surface; no random-dot approximation. */
fun Modifier.textile(base: Color, material: Textile = Textile.FELT, stitched: Boolean = false): Modifier = composed {
    val resources = LocalContext.current.resources
    val resource = when (material) {
        Textile.LEATHER -> R.drawable.texture_leather
        Textile.FELT -> R.drawable.texture_felt
        Textile.SUEDE -> R.drawable.texture_suede
    }
    val density = resources.displayMetrics.density
    val grain = remember(resource, resources, density) {
        // A 128dp physical repeat stays consistent across Android screen densities.
        val bitmap = BitmapFactory.decodeResource(resources, resource)
        val edge = (128 * density).toInt().coerceAtLeast(1)
        val scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, edge, edge, true)
        if (bitmap !== scaled) bitmap.recycle()
        scaled.asImageBitmap()
    }
    this.drawWithCache {
        val texture = ShaderBrush(ImageShader(grain, TileMode.Repeated, TileMode.Repeated))
        val wash = Brush.linearGradient(listOf(Color.White.copy(alpha = .09f), Color.Transparent, Color.Black.copy(alpha = .22f)))
        val seam = Stroke(width = .75.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())))
        onDrawBehind {
            drawRect(base)
            drawRect(texture, alpha = when(material) { Textile.LEATHER -> .55f; Textile.FELT -> .22f; Textile.SUEDE -> .32f }, blendMode = BlendMode.Softlight)
            drawRect(wash)
            if (stitched && size.minDimension > 32.dp.toPx()) {
                val inset = 9.dp.toPx()
                val area = Size(size.width - 2 * inset, size.height - 2 * inset)
                val radius = CornerRadius(13.dp.toPx())
                // Recess under the thread, then a quiet warm thread highlight.
                drawRoundRect(Color.Black.copy(alpha = .38f), Offset(inset, inset + 1.dp.toPx()), area, radius, style = seam)
                drawRoundRect(Color(0xFFD8C2A0).copy(alpha = .48f), Offset(inset, inset), area, radius, style = seam)
                drawRoundRect(Color.White.copy(alpha = .13f), Offset(.5.dp.toPx(), .5.dp.toPx()), Size(size.width - 1.dp.toPx(), size.height - 1.dp.toPx()), CornerRadius(21.dp.toPx()), style = Stroke(1.dp.toPx()))
            }
        }
    }
}
