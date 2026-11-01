package pe.edu.unmsm.yachaytext.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import pe.edu.unmsm.yachaytext.ui.theme.TextDisabled
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pe.edu.unmsm.yachaytext.ui.biblioteca.LibroUi
import pe.edu.unmsm.yachaytext.ui.theme.Ink
import pe.edu.unmsm.yachaytext.ui.theme.Line
import pe.edu.unmsm.yachaytext.ui.theme.Mist
import pe.edu.unmsm.yachaytext.ui.theme.Paper
import pe.edu.unmsm.yachaytext.ui.theme.TextSecondary

@Composable
fun TituloSeccion(texto: String, modifier: Modifier = Modifier) {
    Text(texto, style = MaterialTheme.typography.titleMedium, modifier = modifier.padding(top = 8.dp, bottom = 4.dp))
}

@Composable
fun TarjetaLibro(libro: LibroUi, ahora: Long, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Line),
        color = Paper,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Mist, shape = RoundedCornerShape(14.dp), modifier = Modifier.size(88.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null, tint = Ink, modifier = Modifier.size(36.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(libro.titulo, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Dato(Icons.Outlined.Description, "${libro.area} · ${libro.paginas} ${if (libro.paginas == 1) "página" else "páginas"}")
                Dato(Icons.Outlined.CalendarToday, tiempoRelativo(libro.fechaIngesta, ahora))
            }
        }
    }
}

@Composable
private fun Dato(icono: ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(texto, style = MaterialTheme.typography.labelMedium, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun TarjetaConversacion(titulo: String, detalle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Line),
        color = Paper,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detalle, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
    }
}

/** Estado vacío con ícono, explicación y una acción principal. */
@Composable
fun EstadoVacio(icono: ImageVector, titulo: String, texto: String, accion: String, onAccion: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(color = Mist, shape = RoundedCornerShape(28.dp), modifier = Modifier.size(96.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icono, contentDescription = null, tint = Ink, modifier = Modifier.size(44.dp)) }
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
) {
    val fondo = if (habilitado) Ink else Line
    val contenido = if (habilitado) Paper else TextDisabled
    Surface(onClick = onClick, enabled = habilitado, shape = RoundedCornerShape(50), color = fondo, modifier = modifier.height(56.dp)) {
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
        color = if (activo) Ink else Paper,
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
) {
    Surface(shape = RoundedCornerShape(if (unaLinea) 28.dp else 20.dp), color = Paper, border = BorderStroke(1.dp, Line), modifier = modifier) {
        BasicTextField(
            value = valor,
            onValueChange = onCambio,
            singleLine = unaLinea,
            keyboardOptions = teclado,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink),
            cursorBrush = SolidColor(Ink),
            modifier = Modifier.fillMaxWidth().heightIn(min = alturaMinima).padding(horizontal = 20.dp, vertical = 16.dp),
            decorationBox = { campo ->
                if (valor.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                campo()
            },
        )
    }
}
