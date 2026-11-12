package pe.edu.unmsm.yachaytext.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import pe.edu.unmsm.yachaytext.ui.theme.Brand
import pe.edu.unmsm.yachaytext.ui.theme.BrandSoft
import pe.edu.unmsm.yachaytext.ui.theme.TextSecondary

/** Confirmación de una acción que no se puede deshacer (prevención de errores). */
@Composable
fun DialogoConfirmar(
    titulo: String,
    texto: String,
    confirmar: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
    cancelar: String = "Cancelar",
    destructiva: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo, style = MaterialTheme.typography.titleLarge) },
        text = { Text(texto, style = MaterialTheme.typography.bodyLarge) },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text(confirmar, style = MaterialTheme.typography.labelLarge, color = if (destructiva) MaterialTheme.colorScheme.error else Brand)
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text(cancelar, style = MaterialTheme.typography.labelLarge, color = Brand) } },
    )
}

/** Pide un texto corto, por ejemplo el nuevo nombre de un libro o una conversación. */
@Composable
fun DialogoTexto(titulo: String, inicial: String, onGuardar: (String) -> Unit, onCancelar: () -> Unit) {
    var valor by rememberSaveable { mutableStateOf(inicial) }
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { foco.requestFocus() } }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo, style = MaterialTheme.typography.titleLarge) },
        text = { CampoTexto(valor, { valor = it.take(60) }, "Escribe el nombre", modifier = Modifier.fillMaxWidth(), foco = foco) },
        confirmButton = {
            TextButton(onClick = { onGuardar(valor.trim()) }, enabled = valor.isNotBlank()) {
                Text("Guardar", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar", style = MaterialTheme.typography.labelLarge, color = Brand) } },
    )
}

private val NIVELES = listOf("primaria" to 6, "secundaria" to 5)

/**
 * Grado en dos pasos cortos: nivel (2 opciones) y número (5 o 6), en lugar de 11 opciones a la vez.
 * Guarda el mismo texto de siempre, por ejemplo «5.º de primaria».
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SelectorGrado(grado: String?, onGrado: (String) -> Unit) {
    val nivelGuardado = grado?.substringAfter(" de ", "")?.takeIf { it.isNotEmpty() }
    var nivel by rememberSaveable(nivelGuardado) { mutableStateOf(nivelGuardado ?: "primaria") }
    val numero = grado?.substringBefore(".º")?.toIntOrNull()?.takeIf { nivelGuardado == nivel }
    Column {
        EtiquetaCampo("Nivel")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            NIVELES.forEach { (n, _) -> ChipSeleccion(n.replaceFirstChar { it.uppercase() }, nivel == n, { nivel = n }) }
        }
        EtiquetaCampo("Grado", Modifier.padding(top = 4.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            val maximo = NIVELES.first { it.first == nivel }.second
            (1..maximo).forEach { n -> ChipSeleccion("$n.º", numero == n, { onGrado("$n.º de $nivel") }) }
        }
    }
}

/** Los tres pasos para usar Yachay (ayuda y documentación). */
@Composable
fun PasosYachay(modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Paso(1, Icons.Outlined.CameraAlt, "Escanea una página", "Toma una foto de tu libro y corrige lo que la cámara leyó mal.")
        Paso(2, Icons.Outlined.Forum, "Pregúntale a Yachay", "Escribe o dicta tu duda sobre lo que leíste.")
        Paso(3, Icons.Outlined.Lightbulb, "Descubre la respuesta", "Yachay no te da la respuesta: te hace preguntas y te muestra la página para que la encuentres tú.")
    }
}

@Composable
private fun Paso(numero: Int, icono: ImageVector, titulo: String, texto: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(color = BrandSoft, shape = CircleShape, modifier = Modifier.size(44.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icono, contentDescription = null, tint = Brand, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text("$numero. $titulo", style = MaterialTheme.typography.titleMedium)
            Text(texto, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

@Composable
fun DialogoAyuda(onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Cómo usar Yachay", style = MaterialTheme.typography.titleLarge) },
        text = { PasosYachay() },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Entendido", style = MaterialTheme.typography.labelLarge, color = Brand) } },
    )
}
