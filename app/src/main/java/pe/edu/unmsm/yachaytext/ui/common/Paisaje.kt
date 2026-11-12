package pe.edu.unmsm.yachaytext.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pe.edu.unmsm.yachaytext.R
import pe.edu.unmsm.yachaytext.ui.theme.Brand
import pe.edu.unmsm.yachaytext.ui.theme.BrandDeep
import pe.edu.unmsm.yachaytext.ui.theme.Sun

private val MontanaLejana = Color(0xFF3C8F72)

/**
 * Sol saliendo detrás de dos cerros con andenes, en la mitad derecha del área.
 * Se dibuja sobre un fondo [Brand]; es decorativo (sin descripción para TalkBack).
 */
@Composable
fun PaisajeAndino(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val sol = Offset(w * 0.78f, h * 0.46f)
        drawCircle(Sun.copy(alpha = 0.18f), radius = h * 0.22f, center = sol)
        drawCircle(Sun, radius = h * 0.13f, center = sol)

        drawPath(cerro(listOf(0.40f to 1f, 0.62f to 0.44f, 0.74f to 0.58f, 0.88f to 0.36f, 1f to 0.50f, 1f to 1f)), MontanaLejana)
        val cercano = cerro(listOf(0.50f to 1f, 0.72f to 0.62f, 0.84f to 0.72f, 1f to 0.56f, 1f to 1f))
        drawPath(cercano, BrandDeep)
        // Andenes: líneas suaves dentro del cerro cercano.
        clipPath(cercano) {
            for (i in 1..4) {
                val y = h * (0.66f + i * 0.075f)
                drawLine(Brand, Offset(w * 0.5f, y + h * 0.03f), Offset(w, y - h * 0.03f), strokeWidth = 2.dp.toPx())
            }
        }
    }
}

private fun DrawScope.cerro(puntos: List<Pair<Float, Float>>) = Path().apply {
    puntos.forEachIndexed { i, (x, y) ->
        if (i == 0) moveTo(size.width * x, size.height * y) else lineTo(size.width * x, size.height * y)
    }
    close()
}

/** Logo de Yachay (el mismo del ícono de la app) dentro de un círculo verde. */
@Composable
fun LogoYachay(tamano: Dp = 96.dp, modifier: Modifier = Modifier) {
    Surface(color = Brand, shape = CircleShape, modifier = modifier.size(tamano)) {
        Box {
            Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.fillMaxSize())
        }
    }
}
