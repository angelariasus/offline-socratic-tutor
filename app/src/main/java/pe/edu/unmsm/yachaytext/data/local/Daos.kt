package pe.edu.unmsm.yachaytext.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LibroDao {
    @Insert
    suspend fun insert(libro: LibroEntity): Long

    @Query("SELECT * FROM libro ORDER BY fecha_ingesta DESC")
    fun observeAll(): Flow<List<LibroEntity>>

    @Query("SELECT * FROM libro WHERE id_libro = :id")
    suspend fun getById(id: Long): LibroEntity?

    @Query("SELECT * FROM libro WHERE id_libro = :id")
    fun observe(id: Long): Flow<LibroEntity?>

    @Query("UPDATE libro SET titulo = :titulo WHERE id_libro = :id")
    suspend fun renombrar(id: Long, titulo: String)

    @Query("SELECT COUNT(*) FROM libro")
    suspend fun count(): Int

    @Query("DELETE FROM libro WHERE id_libro = :id")
    suspend fun delete(id: Long)

    /** num_paginas = páginas distintas escaneadas del libro. */
    @Query("UPDATE libro SET num_paginas = (SELECT COUNT(DISTINCT nro_pagina) FROM fragmento WHERE id_libro = :id) WHERE id_libro = :id")
    suspend fun actualizarPaginas(id: Long)
}

/** Fragmento con el título de su libro, para la búsqueda y para citar la fuente. */
data class FragmentoConLibro(
    val id: Long,
    val libroId: Long,
    val titulo: String,
    val nroPagina: Int,
    val texto: String,
    val embedding: ByteArray,
)

/** Texto de un fragmento sin su vector, para mostrar las páginas de un libro. */
data class TextoFragmento(val nroPagina: Int, val orden: Int, val texto: String)

/** Cambia cada vez que se insertan o borran fragmentos (los ids nuevos siempre crecen). */
data class VersionFragmentos(val total: Int, val maxId: Long?)

@Dao
interface FragmentoDao {
    @Insert
    suspend fun insertAll(fragmentos: List<FragmentoEntity>): List<Long>

    @Query("SELECT * FROM fragmento WHERE id_libro = :libroId ORDER BY nro_pagina, orden")
    suspend fun getByLibro(libroId: Long): List<FragmentoEntity>

    @Query("SELECT nro_pagina AS nroPagina, orden, texto FROM fragmento WHERE id_libro = :libroId ORDER BY nro_pagina, orden")
    fun observeTextos(libroId: Long): Flow<List<TextoFragmento>>

    @Query("SELECT nro_pagina AS nroPagina, orden, texto FROM fragmento WHERE id_libro = :libroId AND nro_pagina = :nroPagina ORDER BY orden")
    suspend fun textosPagina(libroId: Long, nroPagina: Int): List<TextoFragmento>

    @Query("SELECT id_fragmento FROM fragmento WHERE id_fragmento IN (:ids)")
    suspend fun existentes(ids: List<Long>): List<Long>

    @Query("SELECT DISTINCT nro_pagina FROM fragmento WHERE id_libro = :libroId")
    suspend fun paginas(libroId: Long): List<Int>

    /** Todos los fragmentos con su libro, para la búsqueda por coseno en memoria (RNF04). */
    @Query(
        "SELECT f.id_fragmento AS id, f.id_libro AS libroId, l.titulo AS titulo, f.nro_pagina AS nroPagina, " +
            "f.texto AS texto, f.embedding AS embedding FROM fragmento f JOIN libro l ON l.id_libro = f.id_libro",
    )
    suspend fun getAllConLibro(): List<FragmentoConLibro>

    @Query("SELECT COUNT(*) AS total, MAX(id_fragmento) AS maxId FROM fragmento")
    suspend fun version(): VersionFragmentos

    @Query("SELECT COUNT(*) FROM fragmento")
    suspend fun count(): Int

    @Query("DELETE FROM fragmento WHERE id_libro = :libroId AND nro_pagina = :nroPagina")
    suspend fun deletePagina(libroId: Long, nroPagina: Int)
}

@Dao
interface ConversacionDao {
    @Insert
    suspend fun insert(conversacion: ConversacionEntity): Long

    /** Solo las conversaciones en las que el estudiante ya preguntó algo: las vacías no se listan. */
    @Query(
        "SELECT * FROM conversacion c WHERE EXISTS (SELECT 1 FROM mensaje m " +
            "WHERE m.id_conversacion = c.id_conversacion AND m.rol = 'ESTUDIANTE') ORDER BY fecha_inicio DESC",
    )
    fun observeAll(): Flow<List<ConversacionEntity>>

    /** Borra las conversaciones abiertas y abandonadas sin ninguna pregunta del estudiante. */
    @Query(
        "DELETE FROM conversacion WHERE NOT EXISTS (SELECT 1 FROM mensaje m " +
            "WHERE m.id_conversacion = conversacion.id_conversacion AND m.rol = 'ESTUDIANTE')",
    )
    suspend fun borrarVacias()

    @Query("SELECT * FROM conversacion WHERE id_conversacion = :id")
    suspend fun getById(id: Long): ConversacionEntity?

    @Query("UPDATE conversacion SET titulo = :titulo WHERE id_conversacion = :id")
    suspend fun renombrar(id: Long, titulo: String)

    @Query("DELETE FROM conversacion WHERE id_conversacion = :id")
    suspend fun delete(id: Long)
}

@Dao
interface MensajeDao {
    @Insert
    suspend fun insert(mensaje: MensajeEntity): Long

    @Insert
    suspend fun insertAll(mensajes: List<MensajeEntity>)

    @Query("SELECT * FROM mensaje WHERE id_conversacion = :conversacionId ORDER BY fecha_hora, id_mensaje")
    suspend fun getByConversacion(conversacionId: Long): List<MensajeEntity>

    @Query("SELECT * FROM mensaje WHERE id_conversacion = :conversacionId ORDER BY fecha_hora, id_mensaje")
    fun observeByConversacion(conversacionId: Long): Flow<List<MensajeEntity>>
}

/** Libro y página de un fragmento usado para responder un mensaje. */
data class FuenteMensaje(val mensajeId: Long, val libroId: Long, val titulo: String, val nroPagina: Int, val puntaje: Float)

@Dao
interface ContextoRecuperadoDao {
    @Insert
    suspend fun insertAll(contextos: List<ContextoRecuperadoEntity>)

    @Query(
        "SELECT cr.* FROM contexto_recuperado cr JOIN mensaje m ON m.id_mensaje = cr.id_mensaje WHERE m.id_conversacion = :conversacionId",
    )
    suspend fun getByConversacion(conversacionId: Long): List<ContextoRecuperadoEntity>

    @Query("SELECT * FROM contexto_recuperado WHERE id_mensaje = :mensajeId ORDER BY puntaje_coseno DESC")
    suspend fun getByMensaje(mensajeId: Long): List<ContextoRecuperadoEntity>

    @Query(
        "SELECT cr.id_mensaje AS mensajeId, l.id_libro AS libroId, l.titulo AS titulo, f.nro_pagina AS nroPagina, cr.puntaje_coseno AS puntaje " +
            "FROM contexto_recuperado cr " +
            "JOIN fragmento f ON f.id_fragmento = cr.id_fragmento " +
            "JOIN libro l ON l.id_libro = f.id_libro " +
            "JOIN mensaje m ON m.id_mensaje = cr.id_mensaje " +
            "WHERE m.id_conversacion = :conversacionId ORDER BY cr.puntaje_coseno DESC",
    )
    fun observeFuentes(conversacionId: Long): Flow<List<FuenteMensaje>>
}
