package pe.edu.unmsm.yachaytext.domain.tutor

import pe.edu.unmsm.yachaytext.data.ChatRepository
import pe.edu.unmsm.yachaytext.data.LibroRepository

/** Crea una conversación nueva con el saludo del tutor y devuelve su id. */
class IniciarConsulta(
    private val chat: ChatRepository,
    private val libros: LibroRepository,
    private val tutor: Tutor,
) {
    suspend operator fun invoke(libroId: Long? = null): Long {
        val libro = libroId?.let { libros.obtener(it) }
        val (titulo, tema) = when {
            libro != null -> libro.titulo to Tema.Libro(libro.titulo)
            libros.hayLibros() -> TITULO_PROVISIONAL to Tema.General
            else -> "La fotosíntesis" to Tema.Ejemplo
        }
        chat.borrarVacias()
        return chat.crearConversacion(titulo, tutor.saludo(tema), libro?.id)
    }

    companion object {
        /** Se reemplaza por la primera pregunta del estudiante. */
        const val TITULO_PROVISIONAL = "Nueva pregunta"
    }
}
