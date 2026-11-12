package pe.edu.unmsm.yachaytext.ui.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pe.edu.unmsm.yachaytext.data.local.FuenteMensaje

class ChatFormatoTest {

    @Test
    fun describeLaFuenteConElLibroMasRelevante() {
        assertNull(describirFuente(emptyList()))
        assertEquals("Ciencia 5 · pág. 42", describirFuente(listOf(FuenteMensaje(1, 1, "Ciencia 5", 42, 0.5f))))
        assertEquals(
            "Ciencia 5 · págs. 41 y 42",
            describirFuente(listOf(FuenteMensaje(1, 1, "Ciencia 5", 42, 0.5f), FuenteMensaje(1, 1, "Ciencia 5", 41, 0.3f), FuenteMensaje(1, 2, "Matemática 5", 9, 0.2f))),
        )
    }

    @Test
    fun usaLaPrimeraPreguntaComoTitulo() {
        assertEquals("Qué absorben las raíces", tituloDesde("¿qué absorben las raíces?"))
        assertEquals("Por qué las hojas de algunas plantas se…", tituloDesde("¿Por qué las hojas de algunas plantas se ponen amarillas en invierno?"))
    }
}
