package pe.edu.unmsm.yachaytext.domain.ingesta

import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pe.edu.unmsm.yachaytext.data.local.EmbeddingCodec

class VectorizadorLexicoTest {

    private val v = VectorizadorLexico()
    private fun sim(a: String, b: String) = EmbeddingCodec.dot(v.vectorizarSync(a), v.vectorizarSync(b))

    @Test
    fun produceVectoresDeNormaUno() {
        val vector = v.vectorizarSync("Las raíces absorben agua del suelo.")
        assertEquals(512, vector.size)
        assertEquals(1f, sqrt(vector.sumOf { (it * it).toDouble() }).toFloat(), 1e-4f)
    }

    @Test
    fun ignoraTildesMayusculasYPlurales() {
        assertEquals(1f, sim("Fotosíntesis de las PLANTAS", "fotosintesis de la planta"), 1e-4f)
    }

    @Test
    fun recuperaElFragmentoQueRespondeLaPregunta() {
        val fragmentos = listOf(
            "La fotosíntesis es el proceso por el cual las plantas fabrican su alimento usando la luz del sol.",
            "Las raíces absorben el agua y los minerales del suelo y los llevan hacia las hojas.",
            "Las fracciones equivalentes representan la misma parte de un entero.",
        )
        val pregunta = "¿Qué absorben las raíces de la planta?"
        val mejor = fragmentos.maxBy { sim(pregunta, it) }
        assertEquals(fragmentos[1], mejor)
        assertTrue(sim(pregunta, fragmentos[2]) < sim(pregunta, fragmentos[1]))
    }
}
