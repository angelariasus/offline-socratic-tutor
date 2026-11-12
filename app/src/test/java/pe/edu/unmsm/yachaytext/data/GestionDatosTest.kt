package pe.edu.unmsm.yachaytext.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pe.edu.unmsm.yachaytext.data.local.FragmentoEntity
import pe.edu.unmsm.yachaytext.data.local.LibroEntity
import pe.edu.unmsm.yachaytext.data.local.RolMensaje
import pe.edu.unmsm.yachaytext.data.local.YachayDatabase
import pe.edu.unmsm.yachaytext.domain.rag.FragmentoRecuperado

/** Borrar, renombrar y deshacer: control del usuario sobre sus libros y conversaciones. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class GestionDatosTest {

    private lateinit var db: YachayDatabase
    private lateinit var libros: LibroRepository
    private lateinit var chat: ChatRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), YachayDatabase::class.java)
            .allowMainThreadQueries().build()
        libros = LibroRepository(db)
        chat = ChatRepository(db)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun libroConPaginas(vararg paginas: Int): Long {
        val id = db.libroDao().insert(LibroEntity(titulo = "Matemática 5", areaCurso = "Matemática", fechaIngesta = 0))
        db.fragmentoDao().insertAll(
            paginas.flatMap { p ->
                listOf(
                    FragmentoEntity(libroId = id, nroPagina = p, orden = 0, texto = "Inicio de la página $p.", embedding = ByteArray(0)),
                    FragmentoEntity(libroId = id, nroPagina = p, orden = 1, texto = "Final de la página $p.", embedding = ByteArray(0)),
                )
            },
        )
        db.libroDao().actualizarPaginas(id)
        return id
    }

    @Test
    fun borrarUnaPaginaActualizaElConteoYConservaLasDemas() = runTest {
        val id = libroConPaginas(10, 11)

        libros.borrarPagina(id, 10)

        assertEquals(1, libros.obtener(id)!!.numPaginas)
        assertEquals(setOf(11), libros.numerosDePagina(id))
        assertEquals(listOf(Pagina(11, "Inicio de la página 11. Final de la página 11.")), libros.observarPaginas(id).first())
    }

    @Test
    fun renombrarYBorrarUnLibroConservaSusConversaciones() = runTest {
        val id = libroConPaginas(1)
        val conversacion = chat.crearConversacion("Fracciones", "Hola", libroId = id)

        libros.renombrar(id, "  Matemática 6 ")
        assertEquals("Matemática 6", libros.obtener(id)!!.titulo)

        libros.borrar(id)
        assertNull(libros.obtener(id))
        assertNull(chat.conversacion(conversacion)!!.libroId)
    }

    @Test
    fun deshacerRestauraLaConversacionConSusMensajesYFuentes() = runTest {
        val libroId = libroConPaginas(42)
        val fragmento = db.fragmentoDao().getByLibro(libroId).first()
        val id = chat.crearConversacion("La fotosíntesis", "Hola", libroId)
        val respuesta = chat.guardarMensaje(id, RolMensaje.TUTOR, "Mira la página 42.")
        chat.guardarContexto(respuesta, listOf(FragmentoRecuperado(fragmento.id, libroId, "Matemática 5", 42, fragmento.texto, 0.8f)))

        val copia = chat.borrar(id)
        assertNotNull(copia)
        assertTrue(chat.observarConversaciones().first().isEmpty())

        chat.restaurar(copia!!)
        assertEquals("La fotosíntesis", chat.conversacion(id)!!.titulo)
        assertEquals(2, chat.observarMensajes(id).first().size)
        assertEquals(listOf(42), chat.observarFuentes(id).first().map { it.nroPagina })
    }
}
