package pe.edu.unmsm.yachaytext.ui.common

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val formatoFecha = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("es-PE"))

/** «Hace un momento», «Hace 5 min», «Hace 2 horas», «Ayer», «Hace 3 días» o «12 de octubre». */
fun tiempoRelativo(epochMillis: Long, ahora: Long = System.currentTimeMillis(), zona: ZoneId = ZoneId.systemDefault()): String {
    val minutos = (ahora - epochMillis) / 60_000
    return when {
        minutos < 1 -> "Hace un momento"
        minutos < 60 -> "Hace $minutos min"
        minutos < 24 * 60 -> (minutos / 60).let { if (it == 1L) "Hace 1 hora" else "Hace $it horas" }
        else -> {
            val dias = (minutos / (24 * 60))
            when {
                dias == 1L -> "Ayer"
                dias < 7 -> "Hace $dias días"
                else -> formatoFecha.format(Instant.ofEpochMilli(epochMillis).atZone(zona))
            }
        }
    }
}
