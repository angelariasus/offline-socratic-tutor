package pe.edu.unmsm.yachaytext.ui.escaneo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RevisionUiStateTest {

    @Test
    fun avisaSoloSiLaPaginaYaExisteEnElLibroElegido() {
        val base = RevisionUiState(texto = "Texto", libroElegido = 1, pagina = "42", paginasGuardadas = setOf(41, 42))

        assertTrue(base.reemplaza)
        assertFalse(base.copy(pagina = "43").reemplaza)
        // Un libro nuevo no tiene páginas que reemplazar.
        assertFalse(base.copy(libroElegido = null).reemplaza)
    }
}
