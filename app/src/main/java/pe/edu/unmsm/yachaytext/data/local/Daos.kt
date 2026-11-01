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

    @Query("DELETE FROM libro WHERE id_libro = :id")
    suspend fun delete(id: Long)

    /** num_paginas = páginas distintas escaneadas del libro. */
    @Query("UPDATE libro SET num_paginas = (SELECT COUNT(DISTINCT nro_pagina) FROM fragmento WHERE id_libro = :id) WHERE id_libro = :id")
    suspend fun actualizarPaginas(id: Long)
}

@Dao
interface FragmentoDao {
    @Insert
    suspend fun insertAll(fragmentos: List<FragmentoEntity>): List<Long>

    @Query("SELECT * FROM fragmento WHERE id_libro = :libroId ORDER BY nro_pagina, orden")
    suspend fun getByLibro(libroId: Long): List<FragmentoEntity>

    /** Todos los fragmentos, para la búsqueda por coseno en memoria (RNF04). */
    @Query("SELECT * FROM fragmento")
    suspend fun getAll(): List<FragmentoEntity>

    @Query("SELECT COUNT(*) FROM fragmento")
    suspend fun count(): Int

    @Query("DELETE FROM fragmento WHERE id_libro = :libroId AND nro_pagina = :nroPagina")
    suspend fun deletePagina(libroId: Long, nroPagina: Int)
}

@Dao
interface ConversacionDao {
    @Insert
    suspend fun insert(conversacion: ConversacionEntity): Long

    @Query("SELECT * FROM conversacion ORDER BY fecha_inicio DESC")
    fun observeAll(): Flow<List<ConversacionEntity>>

    @Query("SELECT * FROM conversacion WHERE id_conversacion = :id")
    suspend fun getById(id: Long): ConversacionEntity?
}

@Dao
interface MensajeDao {
    @Insert
    suspend fun insert(mensaje: MensajeEntity): Long

    @Query("SELECT * FROM mensaje WHERE id_conversacion = :conversacionId ORDER BY fecha_hora, id_mensaje")
    fun observeByConversacion(conversacionId: Long): Flow<List<MensajeEntity>>
}

@Dao
interface ContextoRecuperadoDao {
    @Insert
    suspend fun insertAll(contextos: List<ContextoRecuperadoEntity>)

    @Query("SELECT * FROM contexto_recuperado WHERE id_mensaje = :mensajeId ORDER BY puntaje_coseno DESC")
    suspend fun getByMensaje(mensajeId: Long): List<ContextoRecuperadoEntity>
}
