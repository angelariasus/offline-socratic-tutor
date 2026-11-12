package pe.edu.unmsm.yachaytext.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pe.edu.unmsm.yachaytext.R
import pe.edu.unmsm.yachaytext.ui.theme.Brand

/** Logo de Yachay (el mismo del ícono de la app) dentro de un círculo verde. */
@Composable
fun LogoYachay(tamano: Dp = 96.dp, modifier: Modifier = Modifier) {
    Surface(color = Brand, shape = CircleShape, modifier = modifier.size(tamano)) {
        Box {
            Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.fillMaxSize())
        }
    }
}
