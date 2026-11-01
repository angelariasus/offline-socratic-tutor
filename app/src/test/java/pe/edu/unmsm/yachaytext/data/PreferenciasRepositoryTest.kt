package pe.edu.unmsm.yachaytext.data

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pe.edu.unmsm.yachaytext.domain.glosario.VarianteQuechua
import pe.edu.unmsm.yachaytext.domain.tutor.NivelAyuda

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PreferenciasRepositoryTest {

    private val repo = PreferenciasRepository(ApplicationProvider.getApplicationContext())

    @Test
    fun valoresPorDefecto() {
        val p = repo.actual()
        assertNull(p.nombre)
        assertEquals(VarianteQuechua.COLLAO, p.variante)
        assertEquals(TamanoTexto.Normal, p.tamanoTexto)
        assertFalse(p.primerUsoCompletado)
    }

    @Test
    fun guardaYLeeTodasLasPreferencias() = runTest {
        repo.actualizar {
            it.copy(
                nombre = "  Ana  ", grado = "3.º de primaria", variante = VarianteQuechua.CHANKA,
                tamanoTexto = TamanoTexto.MuyGrande, velocidadVoz = VelocidadVoz.Lenta, nivelAyuda = NivelAyuda.Menos,
                primerUsoCompletado = true,
            )
        }
        val esperado = Preferencias("Ana", "3.º de primaria", VarianteQuechua.CHANKA, TamanoTexto.MuyGrande, VelocidadVoz.Lenta, NivelAyuda.Menos, true)
        assertEquals(esperado, repo.actual())
        assertEquals(esperado, repo.observar().first())
    }

    @Test
    fun nombreVacioSeGuardaComoNulo() {
        repo.actualizar { it.copy(nombre = "   ") }
        assertNull(repo.actual().nombre)
    }
}
