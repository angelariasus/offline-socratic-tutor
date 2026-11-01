package pe.edu.unmsm.yachaytext.domain.ingesta

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pe.edu.unmsm.yachaytext.data.local.EmbeddingCodec
import pe.edu.unmsm.yachaytext.data.local.FragmentoEntity
import pe.edu.unmsm.yachaytext.data.local.LibroEntity
import pe.edu.unmsm.yachaytext.data.local.YachayDatabase

sealed interface DestinoLibro {
    data class Existente(val libroId: Long) : DestinoLibro
    data class Nuevo(val titulo: String, val area: String) : DestinoLibro
}

data class ResultadoIngesta(val libroId: Long, val fragmentos: Int)

/**
 * Flujo de ingesta documental de la Figura 3.6: fragmenta el texto revisado,
 * lo vectoriza y lo guarda en Room. Volver a escanear una página reemplaza sus
 * fragmentos anteriores.
 */
class IngestarPagina(
    private val db: YachayDatabase,
    private val fragmentador: Fragmentador,
    private val vectorizador: Vectorizador,
    private val ahora: () -> Long = System::currentTimeMillis,
) {
    suspend operator fun invoke(
        destino: DestinoLibro,
        nroPagina: Int,
        texto: String,
        progreso: (hechos: Int, total: Int) -> Unit = { _, _ -> },
    ): ResultadoIngesta {
        val trozos = fragmentador.fragmentar(texto)
        require(trozos.isNotEmpty()) { "La página no tiene texto" }

        val vectores = withContext(Dispatchers.Default) {
            trozos.mapIndexed { i, trozo -> vectorizador.vectorizar(trozo).also { progreso(i + 1, trozos.size) } }
        }

        return db.withTransaction {
            val libroId = when (destino) {
                is DestinoLibro.Existente -> destino.libroId
                is DestinoLibro.Nuevo -> db.libroDao().insert(
                    LibroEntity(titulo = destino.titulo.trim(), areaCurso = destino.area, fechaIngesta = ahora()),
                )
            }
            db.fragmentoDao().deletePagina(libroId, nroPagina)
            db.fragmentoDao().insertAll(
                trozos.mapIndexed { i, trozo ->
                    FragmentoEntity(libroId = libroId, nroPagina = nroPagina, orden = i, texto = trozo, embedding = EmbeddingCodec.encode(vectores[i]))
                },
            )
            db.libroDao().actualizarPaginas(libroId)
            ResultadoIngesta(libroId, trozos.size)
        }
    }
}
