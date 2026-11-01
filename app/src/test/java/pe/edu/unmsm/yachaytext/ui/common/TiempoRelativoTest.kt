package pe.edu.unmsm.yachaytext.ui.common

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class TiempoRelativoTest {

    private val lima = ZoneId.of("America/Lima")
    private val ahora = ZonedDateTime.of(2026, 10, 31, 18, 0, 0, 0, lima).toInstant().toEpochMilli()
    private fun hace(minutos: Long) = tiempoRelativo(ahora - minutos * 60_000, ahora, lima)

    @Test
    fun formatosSegunAntiguedad() {
        assertEquals("Hace un momento", hace(0))
        assertEquals("Hace 5 min", hace(5))
        assertEquals("Hace 1 hora", hace(60))
        assertEquals("Hace 3 horas", hace(3 * 60))
        assertEquals("Ayer", hace(24 * 60))
        assertEquals("Hace 4 días", hace(4 * 24 * 60))
        assertEquals("12 de octubre", hace(19L * 24 * 60))
    }
}
