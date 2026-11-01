package pe.edu.unmsm.yachaytext.domain.ingesta

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pe.edu.unmsm.yachaytext.data.local.YachayDatabase

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class IngestarPaginaTest {

    private lateinit var db: YachayDatabase
    private lateinit var ingestar: IngestarPagina

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), YachayDatabase::class.java)
            .allowMainThreadQueries().build()
        ingestar = IngestarPagina(db, Fragmentador(maxCaracteres = 120), VectorizadorLexico(), ahora = { 1_000L })
    }

    @After
    fun tearDown() = db.close()

    private val pagina = (1..6).joinToString(" ") { "Las plantas necesitan luz, agua y aire para vivir número $it." }

    @Test
    fun libroNuevoGuardaFragmentosYCuentaPaginas() = runTest {
        val avances = mutableListOf<Pair<Int, Int>>()
        val r = ingestar(DestinoLibro.Nuevo("Ciencia y Tecnología 5", "Ciencia y Tecnología"), 42, pagina) { h, t -> avances += h to t }

        val libro = db.libroDao().getById(r.libroId)!!
        assertEquals("Ciencia y Tecnología 5", libro.titulo)
        assertEquals(1, libro.numPaginas)
        assertEquals(r.fragmentos, db.fragmentoDao().getByLibro(r.libroId).size)
        assertEquals(r.fragmentos to r.fragmentos, avances.last())
    }

    @Test
    fun reescanearUnaPaginaLaReemplazaYOtraPaginaSuma() = runTest {
        val r = ingestar(DestinoLibro.Nuevo("Matemática 5", "Matemática"), 10, pagina)
        ingestar(DestinoLibro.Existente(r.libroId), 10, "Texto corregido de la página diez.")
        assertEquals(listOf("Texto corregido de la página diez."), db.fragmentoDao().getByLibro(r.libroId).map { it.texto })

        ingestar(DestinoLibro.Existente(r.libroId), 11, "Página once.")
        assertEquals(2, db.libroDao().getById(r.libroId)!!.numPaginas)
    }
}
