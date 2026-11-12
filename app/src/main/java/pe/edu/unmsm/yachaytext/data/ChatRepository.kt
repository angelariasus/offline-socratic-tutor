package pe.edu.unmsm.yachaytext.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import pe.edu.unmsm.yachaytext.data.local.ContextoRecuperadoEntity
import pe.edu.unmsm.yachaytext.data.local.ConversacionEntity
import pe.edu.unmsm.yachaytext.data.local.FuenteMensaje
import pe.edu.unmsm.yachaytext.data.local.MensajeEntity
import pe.edu.unmsm.yachaytext.data.local.RolMensaje
import pe.edu.unmsm.yachaytext.data.local.YachayDatabase
import pe.edu.unmsm.yachaytext.domain.rag.FragmentoRecuperado

/** Copia de una conversación borrada, para poder deshacer el borrado. */
class CopiaConversacion internal constructor(
    internal val conversacion: ConversacionEntity,
    internal val mensajes: List<MensajeEntity>,
    internal val contexto: List<ContextoRecuperadoEntity>,
)

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

    /** Borra la conversación con sus mensajes y devuelve una copia para [restaurar]. */
    suspend fun borrar(conversacionId: Long): CopiaConversacion? = db.withTransaction {
        val conversacion = db.conversacionDao().getById(conversacionId) ?: return@withTransaction null
        val copia = CopiaConversacion(
            conversacion,
            db.mensajeDao().getByConversacion(conversacionId),
            db.contextoRecuperadoDao().getByConversacion(conversacionId),
        )
        db.conversacionDao().delete(conversacionId)
        copia
    }

    suspend fun restaurar(copia: CopiaConversacion) = db.withTransaction {
        db.conversacionDao().insert(copia.conversacion)
        db.mensajeDao().insertAll(copia.mensajes)
        // Si mientras tanto se borró la página citada, ese contexto ya no existe.
        val fragmentos = db.fragmentoDao().existentes(copia.contexto.map { it.fragmentoId })
        db.contextoRecuperadoDao().insertAll(copia.contexto.filter { it.fragmentoId in fragmentos })
    }

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
