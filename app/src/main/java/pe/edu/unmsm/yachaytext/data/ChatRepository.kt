package pe.edu.unmsm.yachaytext.data

import kotlinx.coroutines.flow.Flow
import pe.edu.unmsm.yachaytext.data.local.ContextoRecuperadoEntity
import pe.edu.unmsm.yachaytext.data.local.ConversacionEntity
import pe.edu.unmsm.yachaytext.data.local.FuenteMensaje
import pe.edu.unmsm.yachaytext.data.local.MensajeEntity
import pe.edu.unmsm.yachaytext.data.local.RolMensaje
import pe.edu.unmsm.yachaytext.data.local.YachayDatabase
import pe.edu.unmsm.yachaytext.domain.rag.FragmentoRecuperado

class ChatRepository(
    private val db: YachayDatabase,
    private val ahora: () -> Long = System::currentTimeMillis,
) {
    fun observarConversaciones(): Flow<List<ConversacionEntity>> = db.conversacionDao().observeAll()

    fun observarMensajes(conversacionId: Long): Flow<List<MensajeEntity>> =
        db.mensajeDao().observeByConversacion(conversacionId)

    fun observarFuentes(conversacionId: Long): Flow<List<FuenteMensaje>> =
        db.contextoRecuperadoDao().observeFuentes(conversacionId)

    suspend fun conversacion(id: Long): ConversacionEntity? = db.conversacionDao().getById(id)

    suspend fun crearConversacion(titulo: String, saludo: String, libroId: Long? = null): Long {
        val id = db.conversacionDao().insert(ConversacionEntity(libroId = libroId, titulo = titulo, fechaInicio = ahora()))
        guardarMensaje(id, RolMensaje.TUTOR, saludo)
        return id
    }

    suspend fun renombrar(conversacionId: Long, titulo: String) = db.conversacionDao().renombrar(conversacionId, titulo)

    suspend fun guardarMensaje(conversacionId: Long, rol: RolMensaje, contenido: String, porVoz: Boolean = false): Long =
        db.mensajeDao().insert(
            MensajeEntity(conversacionId = conversacionId, rol = rol, contenido = contenido, porVoz = porVoz, fechaHora = ahora()),
        )

    /** Registra qué fragmentos usó el tutor para responder (CONTEXTO_RECUPERADO). */
    suspend fun guardarContexto(mensajeId: Long, contexto: List<FragmentoRecuperado>) {
        if (contexto.isEmpty()) return
        db.contextoRecuperadoDao().insertAll(contexto.map { ContextoRecuperadoEntity(mensajeId, it.fragmentoId, it.puntaje) })
    }
}
