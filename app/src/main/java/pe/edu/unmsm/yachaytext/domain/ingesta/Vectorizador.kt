package pe.edu.unmsm.yachaytext.domain.ingesta

import java.text.Normalizer
import kotlin.math.ln
import kotlin.math.sqrt

/** Convierte texto en un vector normalizado (norma 1) para la búsqueda por coseno. */
interface Vectorizador {
    val dimension: Int
    suspend fun vectorizar(texto: String): FloatArray
}

/**
 * Vectorizador léxico provisional basado en hashing de palabras: no necesita
 * modelo, ocupa memoria despreciable y recupera fragmentos que comparten
 * vocabulario con la pregunta. Se reemplazará por el Text Embedder de
 * MediaPipe con un modelo multilingüe, que además captura sinónimos.
 */
class VectorizadorLexico(override val dimension: Int = 512) : Vectorizador {

    override suspend fun vectorizar(texto: String): FloatArray = vectorizarSync(texto)

    fun vectorizarSync(texto: String): FloatArray {
        val raices = tokens(texto).map { raiz(it) }
        val pesos = HashMap<Int, Float>()
        fun sumar(termino: String, peso: Float) {
            val h = fnv1a(termino)
            val indice = Math.floorMod(h, dimension)
            val signo = if ((h ushr 31) == 0) 1f else -1f
            pesos[indice] = (pesos[indice] ?: 0f) + signo * peso
        }
        raices.groupingBy { it }.eachCount().forEach { (r, n) -> sumar(r, 1f + ln(n.toFloat())) }
        raices.zipWithNext().forEach { (a, b) -> sumar("$a $b", 0.5f) }

        val vector = FloatArray(dimension)
        pesos.forEach { (i, p) -> vector[i] = p }
        var norma = 0f
        for (v in vector) norma += v * v
        norma = sqrt(norma)
        if (norma > 0f) for (i in vector.indices) vector[i] /= norma
        return vector
    }

    internal fun tokens(texto: String): List<String> =
        Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length > 1 && it !in VACIAS }

    /** Raíz aproximada: recorta plurales y conserva los primeros 6 caracteres («plantas» → «planta»). */
    internal fun raiz(palabra: String): String {
        val singular = when {
            palabra.length > 4 && palabra.endsWith("es") -> palabra.dropLast(2)
            palabra.length > 3 && palabra.endsWith("s") -> palabra.dropLast(1)
            else -> palabra
        }
        return singular.take(6)
    }

    private fun fnv1a(texto: String): Int {
        var h = 0x811C9DC5.toInt()
        for (c in texto) {
            h = h xor c.code
            h *= 0x01000193
        }
        return h
    }

    private companion object {
        val VACIAS = setOf(
            "el", "la", "los", "las", "un", "una", "unos", "unas", "de", "del", "al", "a", "en", "y", "o", "u", "que",
            "se", "su", "sus", "por", "para", "con", "sin", "es", "son", "lo", "le", "les", "como", "mas", "pero",
            "este", "esta", "estos", "estas", "ese", "esa", "eso", "muy", "ya", "si", "no", "me", "mi", "tu", "te",
        )
    }
}
