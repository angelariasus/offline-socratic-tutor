package pe.edu.unmsm.yachaytext.domain.tutor

import pe.edu.unmsm.yachaytext.data.local.RolMensaje
import pe.edu.unmsm.yachaytext.domain.rag.FragmentoRecuperado

data class Turno(val rol: RolMensaje, val texto: String)

/** Cuánto revela el tutor al apoyarse en el libro (ajustable por el estudiante). */
enum class NivelAyuda(val etiqueta: String, val descripcion: String) {
    Mas("Más pistas", "Te muestra la oración del libro y la palabra clave."),
    Normal("Normal", "Te muestra la oración del libro y te pregunta qué significa."),
    Menos("Menos pistas", "Solo te dice en qué página buscar."),
}

/** Sobre qué trata una conversación nueva. */
sealed interface Tema {
    /** Consulta abierta desde un libro escaneado. */
    data class Libro(val titulo: String) : Tema

    /** Consulta libre cuando ya hay libros en la biblioteca. */
    data object General : Tema

    /** Biblioteca vacía: se usa el pasaje de ejemplo del prototipo (fotosíntesis). */
    data object Ejemplo : Tema
}

/** Contrato del motor socrático. La implementación final usará LiteRT-LM (Sprint 3). */
interface Tutor {
    /** Mensaje con el que el tutor abre una conversación nueva. */
    fun saludo(tema: Tema): String

    /**
     * Responde a [consulta] con una pregunta que guíe al estudiante (RF03).
     * [contexto] son los fragmentos del libro recuperados por el RAG, del más al menos relevante.
     */
    suspend fun responder(
        historial: List<Turno>,
        consulta: String,
        contexto: List<FragmentoRecuperado> = emptyList(),
        ayuda: NivelAyuda = NivelAyuda.Normal,
    ): String
}
