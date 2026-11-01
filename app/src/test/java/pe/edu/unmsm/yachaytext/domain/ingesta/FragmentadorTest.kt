package pe.edu.unmsm.yachaytext.domain.ingesta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FragmentadorTest {

    @Test
    fun uneLineasDelOcrYPalabrasCortadasConGuion() {
        val ocr = "Las plantas fabrican su propio ali-\nmento mediante la\nfotosíntesis.\n\nNecesitan luz."
        assertEquals(
            "Las plantas fabrican su propio alimento mediante la fotosíntesis.\nNecesitan luz.".replace("\n", "\n\n"),
            Fragmentador().limpiar(ocr),
        )
    }

    @Test
    fun textoCortoEsUnSoloFragmento() {
        assertEquals(listOf("Las plantas necesitan luz. También necesitan agua."), Fragmentador().fragmentar("Las plantas necesitan luz. También necesitan agua."))
    }

    @Test
    fun respetaElTamanoMaximoYSolapaUnaOracion() {
        val oraciones = (1..12).map { "Esta es la oración número $it del texto de prueba." }
        val fragmentos = Fragmentador(maxCaracteres = 160).fragmentar(oraciones.joinToString(" "))

        assertTrue(fragmentos.size > 1)
        assertTrue(fragmentos.all { it.length <= 160 })
        // La última oración de cada fragmento abre el siguiente.
        fragmentos.zipWithNext().forEach { (a, b) ->
            val ultima = oraciones.last { a.endsWith(it) }
            assertTrue(b.startsWith(ultima))
        }
        // No se pierde ninguna oración.
        assertTrue(oraciones.all { o -> fragmentos.any { it.contains(o) } })
    }

    @Test
    fun parteOracionesMasLargasQueElMaximo() {
        val larga = (1..80).joinToString(" ") { "palabra$it" }
        val fragmentos = Fragmentador(maxCaracteres = 100).fragmentar(larga)
        assertTrue(fragmentos.all { it.length <= 100 })
        assertEquals(larga, fragmentos.joinToString(" "))
    }

    @Test
    fun textoVacioNoGeneraFragmentos() {
        assertEquals(emptyList<String>(), Fragmentador().fragmentar("  \n\n "))
    }
}
