package pe.edu.unmsm.yachaytext.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import pe.edu.unmsm.yachaytext.ui.biblioteca.LibroUi
import pe.edu.unmsm.yachaytext.ui.theme.Brand
import pe.edu.unmsm.yachaytext.ui.theme.BrandSoft
import pe.edu.unmsm.yachaytext.ui.theme.Ink
import pe.edu.unmsm.yachaytext.ui.theme.Line
import pe.edu.unmsm.yachaytext.ui.theme.Paper
import pe.edu.unmsm.yachaytext.ui.theme.TextDisabled
import pe.edu.unmsm.yachaytext.ui.theme.TextSecondary

/** Título de sección: más grande que el de las tarjetas y marcado como encabezado para TalkBack. */
@Composable
fun TituloSeccion(texto: String, modifier: Modifier = Modifier, accion: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(texto, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).semantics { heading() })
        accion?.invoke()
    }
}

/** Opción de un menú «Más opciones». */
data class Opcion(val texto: String, val icono: ImageVector, val destructiva: Boolean = false, val onClick: () -> Unit)

@Composable
fun MenuOpciones(descripcion: String, opciones: List<Opcion>, tinte: Color = TextSecondary) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { abierto = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = descripcion, tint = tinte) }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { o ->
                val color = if (o.destructiva) MaterialTheme.colorScheme.error else Ink
                DropdownMenuItem(
                    text = { Text(o.texto, style = MaterialTheme.typography.bodyLarge, color = color) },
                    leadingIcon = { Icon(o.icono, contentDescription = null, tint = color) },
                    onClick = { abierto = false; o.onClick() },
                )
            }
        }
    }
}

/** Ícono del área sobre fondo verde suave: la «portada» del libro. */
@Composable
fun PortadaArea(area: String, tamano: Dp = 72.dp) {
    Surface(color = BrandSoft, shape = RoundedCornerShape(14.dp), modifier = Modifier.size(tamano)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(iconoArea(area), contentDescription = null, tint = Brand, modifier = Modifier.size(tamano * 0.45f))
        }
    }
}

fun textoPaginas(n: Int) = "$n ${if (n == 1) "página" else "páginas"}"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TarjetaLibro(libro: LibroUi, ahora: Long, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Line), color = Paper, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            PortadaArea(libro.area)
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(libro.titulo, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(libro.area, style = MaterialTheme.typography.labelMedium, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                // Los datos bajan de línea si la tarjeta es angosta (cuadrícula en tableta horizontal).
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Dato(Icons.Outlined.Description, textoPaginas(libro.paginas))
                    Dato(Icons.Outlined.Schedule, tiempoRelativo(libro.fechaIngesta, ahora))
                }
            }
        }
    }
}

@Composable
fun Dato(icono: ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(texto, style = MaterialTheme.typography.labelMedium, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Conversación en Inicio y en la pestaña Tutor (mismo aspecto en ambas).
 * @param detalle libro y antigüedad, por ejemplo «Ciencia y Tecnología 5 · Hace 5 min».
 */
@Composable
fun TarjetaConversacion(
    titulo: String,
    detalle: String,
    onClick: () -> Unit,
    area: String? = null,
    opciones: List<Opcion> = emptyList(),
) {
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Line), color = Paper, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = BrandSoft, shape = CircleShape, modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(area?.let(::iconoArea) ?: Icons.Outlined.Forum, contentDescription = null, tint = Brand, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(detalle, style = MaterialTheme.typography.labelMedium, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (opciones.isNotEmpty()) MenuOpciones("Opciones de «$titulo»", opciones) else Spacer(Modifier.width(8.dp))
        }
    }
}

/** Estado vacío con ícono, explicación y una acción principal. */
@Composable
fun EstadoVacio(icono: ImageVector, titulo: String, texto: String, accion: String, onAccion: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(color = BrandSoft, shape = RoundedCornerShape(28.dp), modifier = Modifier.size(96.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icono, contentDescription = null, tint = Brand, modifier = Modifier.size(44.dp)) }
        }
        Spacer(Modifier.height(20.dp))
        Text(titulo, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        BotonPrincipal(accion, onAccion)
    }
}

@Composable
fun BotonPrincipal(
    texto: String,
    onClick: () -> Unit,
    icono: ImageVector? = null,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    alto: Dp = 56.dp,
) {
    val fondo = if (habilitado) Brand else Line
    val contenido = if (habilitado) Paper else TextDisabled
    Surface(onClick = onClick, enabled = habilitado, shape = RoundedCornerShape(50), color = fondo, modifier = modifier.height(alto)) {
        Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icono != null) {
                Icon(icono, contentDescription = null, tint = contenido)
                Spacer(Modifier.width(10.dp))
            }
            Text(texto, style = MaterialTheme.typography.labelLarge, color = contenido)
        }
    }
}

@Composable
fun BotonSecundario(texto: String, onClick: () -> Unit, icono: ImageVector? = null, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, shape = RoundedCornerShape(50), color = Paper, border = BorderStroke(1.dp, Line), modifier = modifier.height(56.dp)) {
        Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icono != null) {
                Icon(icono, contentDescription = null, tint = Ink)
                Spacer(Modifier.width(10.dp))
            }
            Text(texto, style = MaterialTheme.typography.labelLarge, color = Ink)
        }
    }
}

@Composable
fun ChipSeleccion(texto: String, activo: Boolean, onClick: () -> Unit, icono: ImageVector? = null) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (activo) Brand else Paper,
        border = if (activo) null else BorderStroke(1.dp, Line),
        modifier = Modifier.height(44.dp),
    ) {
        Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            if (icono != null) {
                Icon(icono, contentDescription = null, tint = if (activo) Paper else Ink, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(texto, style = MaterialTheme.typography.labelLarge, color = if (activo) Paper else Ink, maxLines = 1)
        }
    }
}

/** Campo de texto con el estilo de la app (borde suave, sin subrayado de Material). */
@Composable
fun CampoTexto(
    valor: String,
    onCambio: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    unaLinea: Boolean = true,
    alturaMinima: Dp = 56.dp,
    teclado: KeyboardOptions = KeyboardOptions.Default,
    foco: FocusRequester? = null,
) {
    Surface(shape = RoundedCornerShape(if (unaLinea) 28.dp else 20.dp), color = Paper, border = BorderStroke(1.dp, Line), modifier = modifier) {
        BasicTextField(
            value = valor,
            onValueChange = onCambio,
            singleLine = unaLinea,
            keyboardOptions = teclado,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink),
            cursorBrush = SolidColor(Brand),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = alturaMinima)
                .then(if (foco != null) Modifier.focusRequester(foco) else Modifier)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            decorationBox = { campo ->
                if (valor.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                campo()
            },
        )
    }
}

/** Etiqueta visible sobre un campo o grupo de opciones (no depende del texto de ayuda). */
@Composable
fun EtiquetaCampo(texto: String, modifier: Modifier = Modifier) {
    Text(texto, style = MaterialTheme.typography.titleMedium, modifier = modifier.padding(top = 8.dp))
}
