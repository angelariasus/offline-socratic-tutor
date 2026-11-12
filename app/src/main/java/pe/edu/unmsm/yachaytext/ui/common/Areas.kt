package pe.edu.unmsm.yachaytext.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Handyman
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Ícono que representa cada área curricular, para reconocer los libros de un vistazo
 * (heurística «reconocer antes que recordar»). Libro abierto si el área no se conoce.
 */
fun iconoArea(area: String): ImageVector = when (area) {
    "Comunicación" -> Icons.Outlined.AutoStories
    "Matemática" -> Icons.Outlined.Calculate
    "Ciencia y Tecnología" -> Icons.Outlined.Science
    "Personal Social", "Ciencias Sociales" -> Icons.Outlined.Public
    "Desarrollo Personal, Ciudadanía y Cívica" -> Icons.Outlined.Groups
    "Arte y Cultura" -> Icons.Outlined.Palette
    "Educación Religiosa" -> Icons.Outlined.VolunteerActivism
    "Educación para el Trabajo" -> Icons.Outlined.Handyman
    else -> Icons.AutoMirrored.Outlined.MenuBook
}
