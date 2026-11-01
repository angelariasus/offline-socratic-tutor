package pe.edu.unmsm.yachaytext.data

import kotlinx.coroutines.flow.Flow
import pe.edu.unmsm.yachaytext.data.local.LibroEntity
import pe.edu.unmsm.yachaytext.data.local.YachayDatabase

class LibroRepository(private val db: YachayDatabase) {
    fun observarLibros(): Flow<List<LibroEntity>> = db.libroDao().observeAll()
}
