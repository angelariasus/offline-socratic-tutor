package pe.edu.unmsm.yachaytext.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pe.edu.unmsm.yachaytext.ui.biblioteca.LibroUi
import pe.edu.unmsm.yachaytext.ui.theme.TextSecondary

/** Lomo a la izquierda (esquinas casi rectas) y canto a la derecha: se lee como un libro. */
val FormaPortada = RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp, topEnd = 14.dp, bottomEnd = 14.dp)

/**
 * Portada vertical (3:4) con el color y el ícono del área. Se dibuja con formas simples:
 * no añade imágenes al APK y funciona sin internet.
 */
@Composable
fun PortadaLibro(area: String, modifier: Modifier = Modifier) {
    val color = colorArea(area)
    BoxWithConstraints(
        modifier
            .aspectRatio(3f / 4f)
            .shadow(3.dp, FormaPortada)
            .clip(FormaPortada)
            .background(color.fondo),
    ) {
        val ancho = maxWidth
        // Lomo.
        Box(Modifier.fillMaxHeight().width(ancho * 0.11f).background(color.tinta.copy(alpha = 0.12f)))
        Icon(
            iconoArea(area),
            contentDescription = null,
            tint = color.tinta,
            modifier = Modifier.align(Alignment.Center).offset(x = ancho * 0.05f, y = -ancho * 0.08f).size(ancho * 0.38f),
        )
        // Líneas que sugieren el título impreso en la tapa.
        Column(
            Modifier.align(Alignment.BottomStart).padding(start = ancho * 0.22f, bottom = ancho * 0.16f),
            verticalArrangement = Arrangement.spacedBy(ancho * 0.05f),
        ) {
            Box(Modifier.width(ancho * 0.56f).height(ancho * 0.045f).clip(RoundedCornerShape(50)).background(color.tinta.copy(alpha = 0.22f)))
            Box(Modifier.width(ancho * 0.36f).height(ancho * 0.045f).clip(RoundedCornerShape(50)).background(color.tinta.copy(alpha = 0.22f)))
        }
    }
}

/** Libro como portada con su título debajo, para cuadrículas y carruseles. */
@Composable
fun TarjetaPortada(libro: LibroUi, ahora: Long, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaccion = remember { MutableInteractionSource() }
    Column(
        modifier
            .encogerAlPresionar(interaccion)
            .clip(RoundedCornerShape(12.dp))
            .clickable(interactionSource = interaccion, indication = null, role = Role.Button, onClick = onClick),
    ) {
        PortadaLibro(libro.area, Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Text(libro.titulo, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            "${textoPaginas(libro.paginas)} · ${tiempoRelativo(libro.fechaIngesta, ahora).replaceFirstChar { it.lowercase() }}",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
