package pe.edu.unmsm.yachaytext.domain.glosario

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlosarioQuechuaTest {

    private val glosario = GlosarioQuechua()

    @Test
    fun encuentraTerminosEnOrdenYConSuPosicion() {
        val texto = "Las raíces absorben agua. Ya tienes luz del sol y agua."
        val coincidencias = glosario.buscar(texto)

        assertEquals(listOf("raíces", "agua", "sol", "agua"), coincidencias.map { texto.substring(it.inicio, it.fin) })
        assertEquals(listOf("raíz", "agua", "sol"), glosario.terminosEn(texto).map { it.castellano })
    }

    @Test
    fun ignoraMayusculasPeroNoPartesDePalabras() {
        assertEquals(1, glosario.buscar("Agua de lluvia").size)
        // «soleado» y «aguacero» no son «sol» ni «agua».
        assertTrue(glosario.buscar("Un día soleado con aguacero").isEmpty())
    }

    @Test
    fun devuelveLaVarianteElegida() {
        val agua = glosario.terminosEn("agua").single()
        assertEquals("unu", agua.en(VarianteQuechua.COLLAO))
        assertEquals("yaku", agua.en(VarianteQuechua.CHANKA))
    }
}
