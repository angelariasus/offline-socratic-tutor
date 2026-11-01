package pe.edu.unmsm.yachaytext.domain.ingesta

/**
 * Divide el texto de una página en fragmentos (chunks) para el RAG (RF02).
 * Respeta oraciones completas y repite la última oración del fragmento
 * anterior para no perder contexto en los cortes.
 */
class Fragmentador(private val maxCaracteres: Int = 500) {

    fun fragmentar(texto: String): List<String> {
        val oraciones = limpiar(texto)
            .split(Regex("\n{2,}"))
            .flatMap { parrafo -> dividirEnOraciones(parrafo) }
            .flatMap { partirSiEsLarga(it) }
        if (oraciones.isEmpty()) return emptyList()

        val fragmentos = mutableListOf<String>()
        var actual = mutableListOf<String>()
        fun largo(lista: List<String>) = lista.sumOf { it.length } + (lista.size - 1).coerceAtLeast(0)

        for (oracion in oraciones) {
            if (actual.isNotEmpty() && largo(actual + oracion) > maxCaracteres) {
                fragmentos += actual.joinToString(" ")
                // Solapamiento: la última oración abre el fragmento siguiente si cabe.
                actual = if (actual.size > 1 && largo(listOf(actual.last(), oracion)) <= maxCaracteres) {
                    mutableListOf(actual.last())
                } else {
                    mutableListOf()
                }
            }
            actual += oracion
        }
        if (actual.isNotEmpty()) fragmentos += actual.joinToString(" ")
        return fragmentos
    }

    /** Une palabras cortadas con guion al final de línea y las líneas de un mismo párrafo. */
    internal fun limpiar(texto: String): String = texto
        .replace("\r\n", "\n")
        .replace(Regex("(\\p{L})-\n(\\p{Ll})"), "$1$2")
        .replace(Regex("[ \t]+"), " ")
        .replace(Regex("(?<!\n)\n(?!\n)"), " ")
        .replace(Regex(" *\n *"), "\n")
        .trim()

    private fun dividirEnOraciones(parrafo: String): List<String> =
        parrafo.split(Regex("(?<=[.!?…])\\s+(?=[\\p{Lu}¿¡\"«(0-9])"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun partirSiEsLarga(oracion: String): List<String> {
        if (oracion.length <= maxCaracteres) return listOf(oracion)
        val partes = mutableListOf<String>()
        val actual = StringBuilder()
        for (palabra in oracion.split(' ')) {
            if (actual.isNotEmpty() && actual.length + 1 + palabra.length > maxCaracteres) {
                partes += actual.toString()
                actual.clear()
            }
            if (actual.isNotEmpty()) actual.append(' ')
            actual.append(palabra)
        }
        if (actual.isNotEmpty()) partes += actual.toString()
        return partes
    }
}
