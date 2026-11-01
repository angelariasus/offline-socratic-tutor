package pe.edu.unmsm.yachaytext.ui.escaneo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.edu.unmsm.yachaytext.ui.common.EstadoVacio
import pe.edu.unmsm.yachaytext.ui.theme.Paper

/** Provisional: la cámara (CameraX) y el OCR (ML Kit) se implementan en el Sprint 2 (RF01). */
@Composable
fun EscaneoPendienteScreen(onVolver: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Paper).safeDrawingPadding()) {
        IconButton(onClick = onVolver, modifier = Modifier.padding(4.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EstadoVacio(
                icono = Icons.Outlined.DocumentScanner,
                titulo = "Escanear página",
                texto = "Muy pronto podrás fotografiar una página de tu libro y Yachay leerá el texto sin internet.",
                accion = "Volver",
                onAccion = onVolver,
            )
        }
    }
}
