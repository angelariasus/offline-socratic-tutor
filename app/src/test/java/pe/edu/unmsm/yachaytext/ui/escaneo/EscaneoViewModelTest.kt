package pe.edu.unmsm.yachaytext.ui.escaneo

import androidx.camera.core.ImageProxy
import java.lang.reflect.Proxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pe.edu.unmsm.yachaytext.data.ocr.LectorTexto
import pe.edu.unmsm.yachaytext.domain.ingesta.BorradorEscaneo

@OptIn(ExperimentalCoroutinesApi::class)
class EscaneoViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val borrador = BorradorEscaneo()
    private var textoLeido = ""
    private val vm by lazy { EscaneoViewModel(object : LectorTexto { override suspend fun leer(imagen: ImageProxy) = textoLeido }, borrador) }

    // El lector falso no toca la imagen: basta un proxy vacío.
    private val imagen = Proxy.newProxyInstance(javaClass.classLoader, arrayOf(ImageProxy::class.java)) { _, _, _ -> null } as ImageProxy

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun siLaCamaraNoEntregaLaFotoAvisaTrasElTiempoLimite() = runTest(dispatcher) {
        vm.alDisparar()
        assertEquals(EtapaEscaneo.Tomando, vm.estado.value.etapa)

        advanceTimeBy(EscaneoViewModel.ESPERA_MAXIMA_MS + 1)
        assertNull(vm.estado.value.etapa)
        assertTrue(vm.estado.value.aviso!!.startsWith("No se pudo tomar la foto"))
    }

    @Test
    fun fotoSinTextoMuestraAvisoYNoAvanza() = runTest(dispatcher) {
        textoLeido = "  "
        var avanzo = false
        vm.alDisparar()
        vm.alCapturar(imagen) { avanzo = true }
        advanceUntilIdle()

        assertTrue(!avanzo)
        assertTrue(vm.estado.value.aviso!!.startsWith("No encontramos texto"))
        assertNull(borrador.texto.value)
    }

    @Test
    fun fotoConTextoGuardaElBorradorYAvanza() = runTest(dispatcher) {
        textoLeido = "Las plantas necesitan luz."
        var avanzo = false
        vm.alDisparar()
        vm.alCapturar(imagen) { avanzo = true }
        advanceUntilIdle()

        assertTrue(avanzo)
        assertEquals("Las plantas necesitan luz.", borrador.texto.value)
        // El tiempo límite se canceló: no aparece un aviso tardío.
        advanceTimeBy(EscaneoViewModel.ESPERA_MAXIMA_MS + 1)
        assertNull(vm.estado.value.aviso)
    }
}
