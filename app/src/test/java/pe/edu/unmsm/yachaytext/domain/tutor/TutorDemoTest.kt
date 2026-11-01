package pe.edu.unmsm.yachaytext.domain.tutor

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.unmsm.yachaytext.domain.rag.FragmentoRecuperado

class TutorDemoTest {

    private val tutor = TutorDemo(demora = 0)
    private val fragmento = FragmentoRecuperado(
        fragmentoId = 1, libroId = 1, libroTitulo = "Ciencia y Tecnología 5", nroPagina = 42,
        texto = "Las plantas fabrican su alimento. Las raíces absorben el agua y los minerales del suelo. Las hojas captan la luz.",
        puntaje = 0.6f,
    )

    @Test
    fun citaLaOracionMasRelacionadaConLaPregunta() {
        assertEquals(
            "Las raíces absorben el agua y los minerales del suelo",
            tutor.oracionMasRelacionada("¿Qué absorben las raíces?", fragmento.texto),
        )
    }

    @Test
    fun conContextoCitaLaPaginaYRespondeConUnaPregunta() = runTest {
        val r = tutor.responder(emptyList(), "¿Qué absorben las raíces?", listOf(fragmento))
        assertTrue(r.contains("página 42"))
        assertTrue(r.contains("«Las raíces absorben el agua y los minerales del suelo»"))
        assertTrue(r.trimEnd().endsWith("?") || r.contains("?"))
    }

    @Test
    fun sinContextoSigueElGuionDelPrototipo() = runTest {
        val r = tutor.responder(emptyList(), "¿luz?")
        assertTrue(r.startsWith("La luz es una pieza importante"))
        assertFalse(r.contains("página"))
    }

    @Test
    fun saludaSegunElTema() {
        assertTrue(tutor.saludo(Tema.Libro("Matemática 5")).contains("«Matemática 5»"))
        assertTrue(tutor.saludo(Tema.Ejemplo).contains("plantas"))
    }
}
