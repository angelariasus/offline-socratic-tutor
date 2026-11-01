package pe.edu.unmsm.yachaytext.domain.tutor

import pe.edu.unmsm.yachaytext.data.local.RolMensaje

data class Turno(val rol: RolMensaje, val texto: String)

/** Contrato del motor socrático. La implementación final usará RAG + LiteRT-LM (Sprint 3). */
interface Tutor {
    /** Mensaje con el que el tutor abre una conversación nueva. */
    fun saludo(tema: String): String

    suspend fun responder(historial: List<Turno>, consulta: String): String
}
