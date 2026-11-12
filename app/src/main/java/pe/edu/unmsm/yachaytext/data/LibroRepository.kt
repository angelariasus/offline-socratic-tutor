package pe.edu.unmsm.yachaytext.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pe.edu.unmsm.yachaytext.data.local.LibroEntity
import pe.edu.unmsm.yachaytext.data.local.YachayDatabase
import pe.edu.unmsm.yachaytext.domain.ingesta.unirFragmentos

/** Página escaneada de un libro, con su texto reconstruido a partir de los fragmentos. */
data class Pagina(val numero: Int, val texto: String)

class LibroRepository(private val db: YachayDatabase) {
    fun observarLibros(): Flow<List<LibroEntity>> = db.libroDao().observeAll()

    fun observarLibro(id: Long): Flow<LibroEntity?> = db.libroDao().observe(id)

    fun observarPaginas(libroId: Long): Flow<List<Pagina>> = db.fragmentoDao().observeTextos(libroId).map { fragmentos ->
        fragmentos.groupBy { it.nroPagina }.map { (numero, partes) -> Pagina(numero, unirFragmentos(partes.sortedBy { it.orden }.map { it.texto })) }
    }

    suspend fun pagina(libroId: Long, numero: Int): Pagina? =
        db.fragmentoDao().textosPagina(libroId, numero).takeIf { it.isNotEmpty() }?.let { Pagina(numero, unirFragmentos(it.map { f -> f.texto })) }

    suspend fun numerosDePagina(libroId: Long): Set<Int> = db.fragmentoDao().paginas(libroId).toSet()

    suspend fun obtener(id: Long): LibroEntity? = db.libroDao().getById(id)

    suspend fun hayLibros(): Boolean = db.libroDao().count() > 0

    suspend fun renombrar(id: Long, titulo: String) = db.libroDao().renombrar(id, titulo.trim())

    /** Borra el libro y sus páginas; las conversaciones se conservan sin libro (SET NULL). */
    suspend fun borrar(id: Long) = db.libroDao().delete(id)

    suspend fun borrarPagina(libroId: Long, numero: Int) = db.withTransaction {
        db.fragmentoDao().deletePagina(libroId, numero)
        db.libroDao().actualizarPaginas(libroId)
    }
}
