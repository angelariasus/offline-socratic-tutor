package pe.edu.unmsm.yachaytext.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.util.Calendar
import pe.edu.unmsm.yachaytext.data.local.ConversacionEntity
import pe.edu.unmsm.yachaytext.data.local.LibroEntity
import pe.edu.unmsm.yachaytext.ui.theme.Brand
import pe.edu.unmsm.yachaytext.ui.theme.BrandSoft
import pe.edu.unmsm.yachaytext.ui.theme.Linen
import pe.edu.unmsm.yachaytext.ui.theme.Paper
import pe.edu.unmsm.yachaytext.ui.theme.TextSecondary

/** Conversación tal como se lista en Inicio y en Tutor, con el libro al que pertenece. */
data class ConversacionResumen(
    val id: Long,
    val titulo: String,
    val fechaInicio: Long,
    val libro: String? = null,
    val area: String? = null,
) {
    /** «Ciencia y Tecnología 5 · Hace 5 min», o solo la antigüedad si no hay libro. */
    fun detalle(ahora: Long): String = listOfNotNull(libro, tiempoRelativo(fechaInicio, ahora)).joinToString(" · ")
}

fun resumir(conversaciones: List<ConversacionEntity>, libros: List<LibroEntity>): List<ConversacionResumen> {
    val porId = libros.associateBy { it.id }
    return conversaciones.map { c ->
        val libro = c.libroId?.let(porId::get)
        ConversacionResumen(c.id, c.titulo, c.fechaInicio, libro?.titulo, libro?.areaCurso)
    }
}

/** Agrupa por «Hoy», «Esta semana» y «Antes», conservando el orden (más recientes primero). */
fun agruparPorFecha(conversaciones: List<ConversacionResumen>, ahora: Long): List<Pair<String, List<ConversacionResumen>>> {
    val inicioDeHoy = Calendar.getInstance().apply {
        timeInMillis = ahora
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val haceUnaSemana = inicioDeHoy - 6 * 24 * 60 * 60 * 1000L
    return conversaciones
        .groupBy {
            when {
                it.fechaInicio >= inicioDeHoy -> "Hoy"
                it.fechaInicio >= haceUnaSemana -> "Esta semana"
                else -> "Antes"
            }
        }
        .toList()
}

/** Varias conversaciones en un solo bloque blanco, separadas por líneas finas del color del fondo. */
@Composable
fun GrupoConversaciones(
    conversaciones: List<ConversacionResumen>,
    ahora: Long,
    onAbrir: (Long) -> Unit,
    modifier: Modifier = Modifier,
    opciones: ((ConversacionResumen) -> List<Opcion>)? = null,
) {
    if (conversaciones.isEmpty()) return
    Surface(color = Paper, shape = RoundedCornerShape(20.dp), modifier = modifier.fillMaxWidth()) {
        Column {
            conversaciones.forEachIndexed { i, c ->
                if (i > 0) HorizontalDivider(Modifier.padding(start = 72.dp), color = Linen, thickness = 2.dp)
                FilaConversacion(c, ahora, { onAbrir(c.id) }, opciones?.invoke(c).orEmpty())
            }
        }
    }
}

@Composable
fun FilaConversacion(c: ConversacionResumen, ahora: Long, onClick: () -> Unit, opciones: List<Opcion> = emptyList()) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 16.dp, end = if (opciones.isEmpty()) 16.dp else 4.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val color = c.area?.let(::colorArea)
        Box(Modifier.size(40.dp).clip(CircleShape).background(color?.fondo ?: BrandSoft), contentAlignment = Alignment.Center) {
            Icon(c.area?.let(::iconoArea) ?: Icons.Outlined.Forum, contentDescription = null, tint = color?.tinta ?: Brand, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(c.titulo, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(c.detalle(ahora), style = MaterialTheme.typography.labelMedium, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (opciones.isNotEmpty()) MenuOpciones("Opciones de «${c.titulo}»", opciones)
    }
}
