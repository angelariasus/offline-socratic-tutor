package pe.edu.unmsm.yachaytext.ui.biblioteca

import java.text.Normalizer

data class LibroUi(
    val id: Long,
    val titulo: String,
    val area: String,
    val paginas: Int,
    val fechaIngesta: Long,
)

/** Quita tildes y mayúsculas para que «matematica» encuentre «Matemática». */
internal fun normalizar(texto: String): String =
    Normalizer.normalize(texto, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase().trim()

fun filtrarLibros(libros: List<LibroUi>, consulta: String, area: String?): List<LibroUi> {
    val q = normalizar(consulta)
    return libros.filter { libro ->
        (area == null || libro.area == area) &&
            (q.isEmpty() || normalizar(libro.titulo).contains(q) || normalizar(libro.area).contains(q))
    }
}

/** Áreas presentes en la biblioteca, en orden alfabético: solo se ofrecen filtros con resultados. */
fun areasDisponibles(libros: List<LibroUi>): List<String> = libros.map { it.area }.distinct().sortedBy(::normalizar)
