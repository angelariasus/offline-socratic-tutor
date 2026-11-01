package pe.edu.unmsm.yachaytext.ui.biblioteca

import org.junit.Assert.assertEquals
import org.junit.Test

class FiltroLibrosTest {

    private val libros = listOf(
        LibroUi(1, "Ciencia y Tecnología 5", "Ciencia y Tecnología", 12, 0),
        LibroUi(2, "Matemática 5", "Matemática", 8, 0),
        LibroUi(3, "Leyendas de mi comunidad", "Comunicación", 4, 0),
    )

    @Test
    fun buscaSinImportarTildesNiMayusculas() {
        assertEquals(listOf(2L), filtrarLibros(libros, "MATEMATICA", null).map { it.id })
        assertEquals(listOf(1L), filtrarLibros(libros, "tecnologia", null).map { it.id })
    }

    @Test
    fun combinaBusquedaYArea() {
        assertEquals(listOf(3L), filtrarLibros(libros, "", "Comunicación").map { it.id })
        assertEquals(emptyList<Long>(), filtrarLibros(libros, "leyendas", "Matemática").map { it.id })
    }

    @Test
    fun soloOfreceAreasConLibros() {
        assertEquals(listOf("Ciencia y Tecnología", "Comunicación", "Matemática"), areasDisponibles(libros))
    }
}
