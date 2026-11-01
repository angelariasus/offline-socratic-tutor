package pe.edu.unmsm.yachaytext.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class YachayDatabaseTest {

    private lateinit var db: YachayDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            YachayDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun fragmentoConservaSuEmbedding() = runTest {
        val libroId = db.libroDao().insert(libro())
        val vector = EmbeddingCodec.normalize(floatArrayOf(3f, 4f, 0f))
        db.fragmentoDao().insertAll(
            listOf(FragmentoEntity(libroId = libroId, nroPagina = 12, orden = 0, texto = "La fotosíntesis…", embedding = EmbeddingCodec.encode(vector))),
        )

        val guardado = db.fragmentoDao().getByLibro(libroId).single()
        assertArrayEquals(vector, EmbeddingCodec.decode(guardado.embedding), 1e-6f)
    }

    @Test
    fun borrarLibroBorraSusFragmentosYDesligaConversaciones() = runTest {
        val libroId = db.libroDao().insert(libro())
        db.fragmentoDao().insertAll(
            listOf(FragmentoEntity(libroId = libroId, nroPagina = 1, orden = 0, texto = "x", embedding = EmbeddingCodec.encode(floatArrayOf(1f)))),
        )
        val conversacionId = db.conversacionDao().insert(ConversacionEntity(libroId = libroId, titulo = "La fotosíntesis", fechaInicio = 0))

        db.libroDao().delete(libroId)

        assertEquals(0, db.fragmentoDao().count())
        assertNull(db.conversacionDao().getById(conversacionId)!!.libroId)
    }

    @Test
    fun mensajesSeOrdenanPorFecha() = runTest {
        val conversacionId = db.conversacionDao().insert(ConversacionEntity(titulo = "Fracciones", fechaInicio = 0))
        db.mensajeDao().insert(MensajeEntity(conversacionId = conversacionId, rol = RolMensaje.TUTOR, contenido = "¿Qué crees?", fechaHora = 2))
        db.mensajeDao().insert(MensajeEntity(conversacionId = conversacionId, rol = RolMensaje.ESTUDIANTE, contenido = "Hola", porVoz = true, fechaHora = 1))

        val mensajes = db.mensajeDao().observeByConversacion(conversacionId).first()
        assertEquals(listOf(RolMensaje.ESTUDIANTE, RolMensaje.TUTOR), mensajes.map { it.rol })
        assertEquals(true, mensajes.first().porVoz)
    }

    @Test
    fun cosenoDeVectoresNormalizados() {
        val u = EmbeddingCodec.normalize(floatArrayOf(1f, 1f))
        val v = EmbeddingCodec.normalize(floatArrayOf(1f, 0f))
        assertEquals(0.7071f, EmbeddingCodec.dot(u, v), 1e-3f)
    }

    private fun libro() = LibroEntity(titulo = "Ciencia y Tecnología 5", areaCurso = "Ciencia y Tecnología", numPaginas = 120, fechaIngesta = 0)
}
