package pe.edu.unmsm.yachaytext.domain.rag

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import pe.edu.unmsm.yachaytext.data.local.EmbeddingCodec
import pe.edu.unmsm.yachaytext.data.local.FragmentoDao
import pe.edu.unmsm.yachaytext.data.local.VersionFragmentos
import pe.edu.unmsm.yachaytext.domain.ingesta.Vectorizador

data class FragmentoRecuperado(
    val fragmentoId: Long,
    val libroId: Long,
    val libroTitulo: String,
    val nroPagina: Int,
    val texto: String,
    val puntaje: Float,
)

/**
 * Recuperación del RAG (Figura 3.6): compara la pregunta con todos los
 * fragmentos por similitud de coseno. Los vectores se mantienen en memoria y
 * solo se recargan cuando cambian los fragmentos, para responder en menos de
 * 150 ms (RNF04).
 */
class BuscadorFragmentos(
    private val dao: FragmentoDao,
    private val vectorizador: Vectorizador,
    private val umbral: Float = UMBRAL,
) {
    private class Entrada(val id: Long, val libroId: Long, val titulo: String, val pagina: Int, val texto: String, val vector: FloatArray)

    private val candado = Mutex()
    private var version: VersionFragmentos? = null
    private var indice: List<Entrada> = emptyList()

    suspend fun buscar(consulta: String, libroId: Long? = null, k: Int = 3): List<FragmentoRecuperado> {
        val entradas = cargar()
        if (entradas.isEmpty() || consulta.isBlank()) return emptyList()
        val q = vectorizador.vectorizar(consulta)
        return withContext(Dispatchers.Default) {
            entradas.asSequence()
                .filter { libroId == null || it.libroId == libroId }
                .map { it to EmbeddingCodec.dot(q, it.vector) }
                .filter { (_, puntaje) -> puntaje >= umbral }
                .sortedByDescending { (_, puntaje) -> puntaje }
                .take(k)
                .map { (e, puntaje) -> FragmentoRecuperado(e.id, e.libroId, e.titulo, e.pagina, e.texto, puntaje) }
                .toList()
        }
    }

    private suspend fun cargar(): List<Entrada> = candado.withLock {
        val actual = dao.version()
        if (actual != version) {
            indice = dao.getAllConLibro()
                .map { Entrada(it.id, it.libroId, it.titulo, it.nroPagina, it.texto, EmbeddingCodec.decode(it.embedding)) }
                .filter { it.vector.size == vectorizador.dimension }
            version = actual
        }
        indice
    }

    companion object {
        /** Por debajo de este coseno, el fragmento no se considera relacionado con la pregunta. */
        const val UMBRAL = 0.12f
    }
}
