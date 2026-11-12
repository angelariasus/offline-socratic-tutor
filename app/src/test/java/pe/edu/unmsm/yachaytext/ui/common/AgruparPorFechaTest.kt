package pe.edu.unmsm.yachaytext.ui.common

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Test

class AgruparPorFechaTest {

    @Test
    fun separaHoyEstaSemanaYAntesConservandoElOrden() {
        val ahora = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 12, 18, 0, 0) }.timeInMillis
        val hora = 60 * 60 * 1000L
        val lista = listOf(
            ConversacionResumen(1, "Recién", ahora - hora),
            ConversacionResumen(2, "Esta mañana", ahora - 10 * hora),
            ConversacionResumen(3, "Anoche", ahora - 20 * hora),
            ConversacionResumen(4, "Hace 5 días", ahora - 5 * 24 * hora),
            ConversacionResumen(5, "El mes pasado", ahora - 30 * 24 * hora),
        )

        val grupos = agruparPorFecha(lista, ahora).map { (titulo, c) -> titulo to c.map { it.id } }

        assertEquals(listOf("Hoy" to listOf(1L, 2L), "Esta semana" to listOf(3L, 4L), "Antes" to listOf(5L)), grupos)
    }
}
