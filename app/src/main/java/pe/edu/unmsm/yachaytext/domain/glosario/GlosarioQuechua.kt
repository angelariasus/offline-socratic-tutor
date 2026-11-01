package pe.edu.unmsm.yachaytext.domain.glosario

/** Variantes de quechua atendidas (limitación de la Tabla 3.5). */
enum class VarianteQuechua(val etiqueta: String) { COLLAO("collao"), CHANKA("chanka") }

data class TerminoQuechua(
    val castellano: String,
    /** Formas que se reconocen en el texto (singular, plural…), en minúsculas. */
    val formas: List<String>,
    val collao: String,
    val chanka: String,
) {
    fun en(variante: VarianteQuechua) = when (variante) {
        VarianteQuechua.COLLAO -> collao
        VarianteQuechua.CHANKA -> chanka
    }
}

data class Coincidencia(val inicio: Int, val fin: Int, val termino: TerminoQuechua)

/**
 * Glosario curado castellano–quechua (RF05). Es la fuente principal de los
 * términos resaltados: no depende del modelo de lenguaje, que con 1B de
 * parámetros no traduce el quechua con fiabilidad.
 *
 * PENDIENTE: validar cada entrada con un docente bilingüe y ampliarla con los
 * vocabularios pedagógicos del MINEDU.
 */
class GlosarioQuechua(private val terminos: List<TerminoQuechua> = BASE) {

    private val patrones = terminos.map { termino ->
        val alternativas = termino.formas.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }
        termino to Regex("(?<![\\p{L}])($alternativas)(?![\\p{L}])", RegexOption.IGNORE_CASE)
    }

    /** Coincidencias en orden de aparición y sin solaparse. */
    fun buscar(texto: String): List<Coincidencia> {
        val todas = patrones.flatMap { (termino, regex) ->
            regex.findAll(texto).map { Coincidencia(it.range.first, it.range.last + 1, termino) }
        }.sortedBy { it.inicio }
        val resultado = mutableListOf<Coincidencia>()
        for (c in todas) if (resultado.lastOrNull()?.let { c.inicio < it.fin } != true) resultado += c
        return resultado
    }

    /** Términos distintos presentes en el texto, en orden de aparición. */
    fun terminosEn(texto: String): List<TerminoQuechua> = buscar(texto).map { it.termino }.distinct()

    companion object {
        // Vocabulario básico de Ciencia y Tecnología. Chanka no usa consonantes aspiradas (saphi → sapi).
        val BASE = listOf(
            TerminoQuechua("sol", listOf("sol"), collao = "inti", chanka = "inti"),
            TerminoQuechua("agua", listOf("agua", "aguas"), collao = "unu", chanka = "yaku"),
            TerminoQuechua("tierra", listOf("tierra"), collao = "allpa", chanka = "allpa"),
            TerminoQuechua("raíz", listOf("raíz", "raíces"), collao = "saphi", chanka = "sapi"),
        )
    }
}
