package pe.edu.unmsm.yachaytext.domain.ingesta

import org.junit.Assert.assertEquals
import org.junit.Test

class UnirFragmentosTest {

    @Test
    fun reconstruyeLaPaginaSinRepetirLaOracionSolapada() {
        val oraciones = (1..12).map { "Esta es la oración número $it del texto de prueba." }
        val texto = oraciones.joinToString(" ")
        val fragmentos = Fragmentador(maxCaracteres = 160).fragmentar(texto)

        assertEquals(texto, unirFragmentos(fragmentos))
    }

    @Test
    fun uneSinSolapamientoCuandoNoHayOracionRepetida() {
        assertEquals("Primera parte. Segunda parte.", unirFragmentos(listOf("Primera parte.", "Segunda parte.")))
        assertEquals("", unirFragmentos(emptyList()))
    }
}
