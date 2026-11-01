package pe.edu.unmsm.yachaytext.domain.tutor

import pe.edu.unmsm.yachaytext.data.ChatRepository

/** Crea una conversación nueva con el saludo del tutor y devuelve su id. */
class IniciarConsulta(private val repositorio: ChatRepository, private val tutor: Tutor) {
    suspend operator fun invoke(libroId: Long? = null): Long {
        // Mientras no exista la ingesta de libros, la consulta usa el pasaje de ejemplo del tutor demo.
        val tema = "La fotosíntesis"
        return repositorio.crearConversacion(tema, tutor.saludo(tema), libroId)
    }
}
