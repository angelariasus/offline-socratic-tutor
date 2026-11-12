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
import androidx.compose.ui.graphics.Color
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

/** Fondo suave y tinta de cada área: la tinta sobre el fondo supera 5:1 (WCAG AA). */
data class ColorArea(val fondo: Color, val tinta: Color)

fun colorArea(area: String): ColorArea = when (area) {
    "Comunicación" -> ColorArea(Color(0xFFFBE3CF), Color(0xFF8F4310))
    "Matemática" -> ColorArea(Color(0xFFDCE8F7), Color(0xFF24548F))
    "Ciencia y Tecnología" -> ColorArea(Color(0xFFDCEFE4), Color(0xFF1E6B52))
    "Personal Social", "Ciencias Sociales" -> ColorArea(Color(0xFFD7ECEF), Color(0xFF1D5F68))
    "Desarrollo Personal, Ciudadanía y Cívica" -> ColorArea(Color(0xFFE9E0F3), Color(0xFF5B3E8A))
    "Arte y Cultura" -> ColorArea(Color(0xFFF7DDE3), Color(0xFF93294A))
    "Educación Religiosa" -> ColorArea(Color(0xFFF6EBC8), Color(0xFF6E5108))
    "Educación para el Trabajo" -> ColorArea(Color(0xFFEEDFD3), Color(0xFF6A432D))
    else -> ColorArea(Color(0xFFF1EDE6), Color(0xFF4A463F))
}
