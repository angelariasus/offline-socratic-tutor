package pe.edu.unmsm.yachaytext.ui.common

import android.provider.Settings
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext

/**
 * true si el estudiante desactivó las animaciones en Android (Accesibilidad → Quitar animaciones).
 * En ese caso el movimiento decorativo se omite.
 */
@Composable
fun movimientoReducido(): Boolean {
    val contexto = LocalContext.current
    return remember {
        Settings.Global.getFloat(contexto.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/**
 * Lo que se toca responde al instante: se encoge un 3 % mientras se presiona y vuelve con un
 * resorte, que se puede interrumpir si el dedo se levanta antes de tiempo.
 */
@Composable
fun Modifier.encogerAlPresionar(interaccion: InteractionSource): Modifier {
    val presionado by interaccion.collectIsPressedAsState()
    val reducido = movimientoReducido()
    val escala by animateFloatAsState(
        targetValue = if (presionado && !reducido) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "presion",
    )
    return graphicsLayer { scaleX = escala; scaleY = escala }
}
