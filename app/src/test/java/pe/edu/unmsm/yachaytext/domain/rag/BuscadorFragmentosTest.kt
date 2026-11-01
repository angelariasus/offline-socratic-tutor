package pe.edu.unmsm.yachaytext.domain.rag

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pe.edu.unmsm.yachaytext.data.local.EmbeddingCodec
import pe.edu.unmsm.yachaytext.data.local.FragmentoEntity
import pe.edu.unmsm.yachaytext.data.local.LibroEntity
import pe.edu.unmsm.yachaytext.data.local.YachayDatabase
import pe.edu.unmsm.yachaytext.domain.ingesta.DestinoLibro
import pe.edu.unmsm.yachaytext.domain.ingesta.Fragmentador
import pe.edu.unmsm.yachaytext.domain.ingesta.IngestarPagina
import pe.edu.unmsm.yachaytext.domain.ingesta.VectorizadorLexico

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BuscadorFragmentosTest {

    private lateinit var db: YachayDatabase
    private val vectorizador = VectorizadorLexico()
    private lateinit var ingestar: IngestarPagina
    private lateinit var buscador: BuscadorFragmentos

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), YachayDatabase::class.java)
            .allowMainThreadQueries().build()
        ingestar = IngestarPagina(db, Fragmentador(maxCaracteres = 200), vectorizador)
        buscador = BuscadorFragmentos(db.fragmentoDao(), vectorizador)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun cargarLibros(): Pair<Long, Long> {
        val ciencia = ingestar(
            DestinoLibro.Nuevo("Ciencia y Tecnología 5", "Ciencia y Tecnología"), 42,
            "Las plantas fabrican su alimento mediante la fotosíntesis.\n\n" +
                "Las raíces absorben el agua y los minerales del suelo.\n\n" +
                "Las hojas captan la luz del sol.",
        ).libroId
        val mate = ingestar(
            DestinoLibro.Nuevo("Matemática 5", "Matemática"), 10,
            "Dos fracciones son equivalentes cuando representan la misma parte de un entero.",
        ).libroId
        return ciencia to mate
    }

    @Test
    fun devuelvePrimeroElFragmentoQueRespondeLaPregunta() = runTest {
        cargarLibros()
        val r = buscador.buscar("¿Qué absorben las raíces?")
        assertTrue(r.first().texto.contains("raíces absorben"))
        assertEquals(42, r.first().nroPagina)
        assertEquals("Ciencia y Tecnología 5", r.first().libroTitulo)
    }

    @Test
    fun filtraPorLibroYDescartaLoQueNoSeRelaciona() = runTest {
        val (ciencia, mate) = cargarLibros()
        assertTrue(buscador.buscar("fracciones equivalentes", libroId = ciencia).isEmpty())
        assertEquals(mate, buscador.buscar("fracciones equivalentes", libroId = mate).single().libroId)
        assertTrue(buscador.buscar("¿Quién ganó el partido de fútbol?").isEmpty())
    }

    @Test
    fun veLosCambiosAlReescanearUnaPagina() = runTest {
        val (ciencia, _) = cargarLibros()
        assertTrue(buscador.buscar("raíces").isNotEmpty())
        ingestar(DestinoLibro.Existente(ciencia), 42, "Los animales herbívoros se alimentan de plantas.")
        assertTrue(buscador.buscar("raíces absorben agua").isEmpty())
        assertTrue(buscador.buscar("animales herbívoros").isNotEmpty())
    }

    /** RNF04: búsqueda en ≤ 150 ms con los vectores ya en memoria (unas 150 páginas escaneadas). */
    @Test
    fun buscaEnTresMilFragmentosEnMenosDe150ms() = runTest {
        val libroId = db.libroDao().insert(LibroEntity(titulo = "Libro grande", areaCurso = "Comunicación", fechaIngesta = 0))
        val temas = listOf("plantas", "animales", "fracciones", "historia", "geografía", "energía", "agua", "suelo", "lectura", "números")
        db.fragmentoDao().insertAll(
            (0 until 3000).map { i ->
                val texto = "Fragmento $i sobre ${temas[i % temas.size]} y ${temas[(i / 7) % temas.size]} con ejemplos del aula."
                FragmentoEntity(libroId = libroId, nroPagina = i / 20, orden = i % 20, texto = texto, embedding = EmbeddingCodec.encode(vectorizador.vectorizarSync(texto)))
            },
        )
        buscador.buscar("calentar caché")

        val ms = measureTimeMillis { repeat(10) { buscador.buscar("¿Cómo se alimentan las plantas y los animales?") } } / 10
        println("Búsqueda en 3000 fragmentos: $ms ms promedio")
        assertTrue("Promedio: $ms ms", ms <= 150)
    }
}
