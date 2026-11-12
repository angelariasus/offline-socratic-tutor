package pe.edu.unmsm.yachaytext.ui.bienvenida

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pe.edu.unmsm.yachaytext.YachayApplication
import pe.edu.unmsm.yachaytext.data.GRADOS
import pe.edu.unmsm.yachaytext.ui.common.BotonPrincipal
import pe.edu.unmsm.yachaytext.ui.common.CampoTexto
import pe.edu.unmsm.yachaytext.ui.common.ChipSeleccion
import pe.edu.unmsm.yachaytext.ui.theme.Ink
import pe.edu.unmsm.yachaytext.ui.theme.Paper
import pe.edu.unmsm.yachaytext.ui.theme.TextSecondary
import pe.edu.unmsm.yachaytext.ui.theme.Linen
import pe.edu.unmsm.yachaytext.ui.common.LogoYachay

/** Primer uso: registra el perfil local (nombre y grado). Se puede omitir y completar luego en Ajustes. */
@Composable
fun BienvenidaRoute(onTerminar: () -> Unit) {
    val preferencias = (LocalContext.current.applicationContext as YachayApplication).container.preferencias
    var nombre by rememberSaveable { mutableStateOf("") }
    var grado by rememberSaveable { mutableStateOf<String?>(null) }
    fun terminar(guardarPerfil: Boolean) {
        preferencias.actualizar {
            if (guardarPerfil) it.copy(nombre = nombre, grado = grado, primerUsoCompletado = true) else it.copy(primerUsoCompletado = true)
        }
        onTerminar()
    }
    BienvenidaScreen(nombre, grado, { nombre = it }, { grado = it }, onEmpezar = { terminar(true) }, onOmitir = { terminar(false) })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BienvenidaScreen(
    nombre: String,
    grado: String?,
    onNombre: (String) -> Unit,
    onGrado: (String) -> Unit,
    onEmpezar: () -> Unit,
    onOmitir: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Linen).safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 600.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LogoYachay()
            Spacer(Modifier.height(24.dp))
            Text("Hola, soy Yachay", style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "Te ayudo a entender tus libros haciéndote preguntas. Funciono sin internet.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("¿Cómo te llamas?", style = MaterialTheme.typography.titleMedium)
                CampoTexto(nombre, onNombre, "Tu nombre", modifier = Modifier.fillMaxWidth())
                Text("¿En qué grado estás?", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GRADOS.forEach { g -> ChipSeleccion(g, grado == g, { onGrado(g) }) }
                }
            }

            Spacer(Modifier.height(32.dp))
            BotonPrincipal("Empezar", onEmpezar, habilitado = nombre.isNotBlank(), modifier = Modifier.fillMaxWidth())
            TextButton(onClick = onOmitir, modifier = Modifier.padding(top = 8.dp)) {
                Text("Omitir por ahora", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            }
        }
    }
}
