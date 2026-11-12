package pe.edu.unmsm.yachaytext.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import pe.edu.unmsm.yachaytext.ui.escaneo.AccionesRevision
import pe.edu.unmsm.yachaytext.ui.escaneo.EscaneoScreen
import pe.edu.unmsm.yachaytext.ui.escaneo.EscaneoUiState
import pe.edu.unmsm.yachaytext.ui.escaneo.EtapaEscaneo
import pe.edu.unmsm.yachaytext.ui.escaneo.FaseRevision
import pe.edu.unmsm.yachaytext.ui.escaneo.LibroOpcion
import pe.edu.unmsm.yachaytext.ui.escaneo.RevisionScreen
import pe.edu.unmsm.yachaytext.ui.escaneo.RevisionUiState
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import pe.edu.unmsm.yachaytext.data.Preferencias
import pe.edu.unmsm.yachaytext.data.TamanoTexto
import pe.edu.unmsm.yachaytext.domain.glosario.VarianteQuechua
import pe.edu.unmsm.yachaytext.domain.tutor.NivelAyuda
import pe.edu.unmsm.yachaytext.ui.ajustes.AccionesAjustes
import pe.edu.unmsm.yachaytext.ui.ajustes.AjustesScreen
import pe.edu.unmsm.yachaytext.ui.ajustes.AjustesUiState
import pe.edu.unmsm.yachaytext.ui.biblioteca.BibliotecaScreen
import pe.edu.unmsm.yachaytext.ui.bienvenida.BienvenidaScreen
import pe.edu.unmsm.yachaytext.ui.biblioteca.BibliotecaUiState
import pe.edu.unmsm.yachaytext.ui.biblioteca.LibroUi
import pe.edu.unmsm.yachaytext.ui.chat.ChatPreviewData
import pe.edu.unmsm.yachaytext.ui.inicio.AccionesInicio
import pe.edu.unmsm.yachaytext.ui.common.ConversacionResumen
import pe.edu.unmsm.yachaytext.ui.libro.AccionesLibro
import pe.edu.unmsm.yachaytext.ui.libro.LibroDetalleUiState
import pe.edu.unmsm.yachaytext.ui.libro.LibroScreen
import pe.edu.unmsm.yachaytext.data.Pagina
import pe.edu.unmsm.yachaytext.ui.tutor.AccionesTutor
import pe.edu.unmsm.yachaytext.ui.inicio.InicioScreen
import pe.edu.unmsm.yachaytext.ui.inicio.InicioUiState
import pe.edu.unmsm.yachaytext.ui.chat.ChatScreen
import pe.edu.unmsm.yachaytext.ui.chat.ChatUiState
import pe.edu.unmsm.yachaytext.ui.theme.YachayTheme
import pe.edu.unmsm.yachaytext.ui.tutor.TutorScreen

/**
 * Genera PNG de las pantallas sin emulador:
 *   gradlew recordRoborazziDebug   → app/screenshots/
 * Perfil por defecto: tableta de 8" en vertical (tabletas MINEDU).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = TABLETA)
class ScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun capturar(nombre: String, escalaTexto: Float = 1f, contenido: @Composable () -> Unit) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { YachayTheme(escalaTexto) { contenido() } }
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.onRoot().captureRoboImage("screenshots/$nombre.png")
    }

    private fun chat(nombre: String, estado: ChatUiState) =
        capturar(nombre) { ChatScreen(estado, {}, {}, {}, {}, {}, {}) }

    @Test fun appShell() = capturar("app_shell") { YachayApp() }

    @Test fun chatConversacion() = chat("chat_conversacion", ChatPreviewData.conversacion)

    @Test fun chatPensando() = chat("chat_pensando", ChatPreviewData.pensandoYEscribiendo)

    @Test fun chatGrabando() = chat("chat_grabando", ChatPreviewData.grabando)

    @Test fun chatConTexto() = chat("chat_con_texto", ChatPreviewData.conTexto)

    @Test
    @Config(qualifiers = TELEFONO)
    fun chatTelefono() = chat("chat_telefono", ChatPreviewData.conversacion)

    @Test
    fun inicioConDatos() = capturar("inicio_con_datos") {
        InicioScreen(
            InicioUiState(
                nombre = "Angel",
                conversaciones = CONVERSACIONES.take(2),
                libros = LIBROS.take(3),
                totalLibros = LIBROS.size,
                cargando = false,
            ),
            AccionesInicio(),
            ahora = AHORA,
        )
    }

    @Test
    fun inicioVacio() = capturar("inicio_vacio") {
        InicioScreen(InicioUiState(cargando = false), AccionesInicio(), ahora = AHORA)
    }

    @Test
    fun bibliotecaConLibros() = capturar("biblioteca_con_libros") {
        BibliotecaScreen(BibliotecaUiState(libros = LIBROS, cargando = false), {}, {}, {}, {}, ahora = AHORA)
    }

    @Test
    fun bibliotecaFiltrada() = capturar("biblioteca_filtrada") {
        BibliotecaScreen(BibliotecaUiState(libros = LIBROS, area = "Matemática", cargando = false), {}, {}, {}, {}, ahora = AHORA)
    }

    @Test
    fun bibliotecaSinResultados() = capturar("biblioteca_sin_resultados") {
        BibliotecaScreen(BibliotecaUiState(libros = LIBROS, consulta = "geografía", cargando = false), {}, {}, {}, {}, ahora = AHORA)
    }

    @Test
    fun bibliotecaVacia() = capturar("biblioteca_vacia") {
        BibliotecaScreen(BibliotecaUiState(cargando = false), {}, {}, {}, {}, ahora = AHORA)
    }

    private fun escaneo(nombre: String, estado: EscaneoUiState, permiso: Boolean = true) = capturar(nombre) {
        // Sin cámara en la JVM: un fondo que simula la página fotografiada.
        EscaneoScreen(estado, permiso, {}, {}, {}, {}, vistaPrevia = { Box(Modifier.fillMaxSize().background(Color(0xFF6B6157))) })
    }

    @Test fun escaneoCamara() = escaneo("escaneo_camara", EscaneoUiState())

    @Test fun escaneoLeyendo() = escaneo("escaneo_leyendo", EscaneoUiState(etapa = EtapaEscaneo.Leyendo))

    @Test fun escaneoSinTexto() = escaneo("escaneo_sin_texto", EscaneoUiState(aviso = "No encontramos texto. Acerca la página y busca más luz."))

    @Test fun escaneoSinPermiso() = escaneo("escaneo_sin_permiso", EscaneoUiState(), permiso = false)

    private val textoOcr = "La fotosíntesis\n\nLas plantas fabrican su propio alimento mediante la fotosíntesis. Para ello necesitan luz del sol, agua y dióxido de carbono. Las raíces absorben el agua del suelo y las hojas captan la luz."

    @Test
    fun revisionLibroNuevo() = capturar("revision_libro_nuevo") {
        RevisionScreen(RevisionUiState(texto = textoOcr, tituloNuevo = "Ciencia y Tecnología 5", areaNueva = "Ciencia y Tecnología", pagina = "42"), AccionesRevision())
    }

    @Test
    fun revisionLibroExistente() = capturar("revision_libro_existente") {
        RevisionScreen(
            RevisionUiState(
                texto = textoOcr,
                libros = listOf(LibroOpcion(1, "Ciencia y Tecnología 5"), LibroOpcion(2, "Matemática 5")),
                libroElegido = 1,
                pagina = "43",
            ),
            AccionesRevision(),
        )
    }

    @Test
    fun revisionIndexando() = capturar("revision_indexando") {
        RevisionScreen(RevisionUiState(texto = textoOcr, fase = FaseRevision.Indexando(2, 3)), AccionesRevision())
    }

    @Test
    fun revisionListo() = capturar("revision_listo") {
        RevisionScreen(RevisionUiState(texto = textoOcr, fase = FaseRevision.Listo(1, 3)), AccionesRevision())
    }

    private val ajustes = AjustesUiState(
        Preferencias(nombre = "Ana", grado = "5.º de primaria", variante = VarianteQuechua.CHANKA, nivelAyuda = NivelAyuda.Normal),
        nombre = "Ana",
    )

    @Test fun ajustesPantalla() = capturar("ajustes") { AjustesScreen(ajustes, AccionesAjustes()) }

    @Test
    fun ajustesTextoMuyGrande() = capturar("ajustes_texto_muy_grande", TamanoTexto.MuyGrande.escala) {
        AjustesScreen(ajustes.copy(preferencias = ajustes.preferencias.copy(tamanoTexto = TamanoTexto.MuyGrande)), AccionesAjustes())
    }

    @Test fun bienvenidaVacia() = capturar("bienvenida") { BienvenidaScreen("", null, {}, {}, {}, {}) }

    @Test fun bienvenidaCompleta() = capturar("bienvenida_completa") { BienvenidaScreen("Ana", "5.º de primaria", {}, {}, {}, {}) }

    @Test fun chatChanka() = chat("chat_chanka", ChatPreviewData.conversacion.copy(variante = VarianteQuechua.CHANKA))

    @Test
    fun tutorLista() = capturar("tutor_lista") {
        TutorScreen(CONVERSACIONES, AccionesTutor(), ahora = AHORA)
    }

    private val paginas = listOf(
        Pagina(42, textoOcr.substringAfter("\n\n")),
        Pagina(43, "La clorofila es el pigmento verde de las hojas. Gracias a ella, las plantas captan la energía de la luz del sol."),
        Pagina(44, "Durante la fotosíntesis, las plantas liberan oxígeno al aire. Por eso decimos que los bosques ayudan a respirar."),
    )

    @Test
    fun libroDetalle() = capturar("libro_detalle") {
        LibroScreen(LibroDetalleUiState(LIBROS[0], paginas, cargando = false), AccionesLibro(), ahora = AHORA)
    }

    @Test
    fun libroSinPaginas() = capturar("libro_sin_paginas") {
        LibroScreen(LibroDetalleUiState(LIBROS[0].copy(paginas = 0), emptyList(), cargando = false), AccionesLibro(), ahora = AHORA)
    }

    @Test
    fun revisionReemplaza() = capturar("revision_reemplaza") {
        RevisionScreen(
            RevisionUiState(
                texto = textoOcr,
                libros = listOf(LibroOpcion(1, "Ciencia y Tecnología 5"), LibroOpcion(2, "Matemática 5")),
                libroElegido = 1,
                pagina = "42",
                paginasGuardadas = setOf(42),
            ),
            AccionesRevision(),
        )
    }

    // Tableta en horizontal: cuadrícula de libros y revisión en dos columnas.
    @Test
    @Config(qualifiers = HORIZONTAL)
    fun bibliotecaHorizontal() = capturar("biblioteca_horizontal") {
        BibliotecaScreen(BibliotecaUiState(libros = LIBROS, cargando = false), {}, {}, {}, {}, ahora = AHORA)
    }

    @Test
    @Config(qualifiers = HORIZONTAL)
    fun revisionHorizontal() = capturar("revision_horizontal") {
        RevisionScreen(RevisionUiState(texto = textoOcr, tituloNuevo = "Ciencia y Tecnología 5", areaNueva = "Ciencia y Tecnología", pagina = "42"), AccionesRevision())
    }

    @Test
    @Config(qualifiers = HORIZONTAL)
    fun inicioHorizontal() = capturar("inicio_horizontal") {
        InicioScreen(
            InicioUiState(nombre = "Angel", conversaciones = CONVERSACIONES, libros = LIBROS.take(3), totalLibros = LIBROS.size, cargando = false),
            AccionesInicio(),
            ahora = AHORA,
        )
    }

    @Test
    @Config(qualifiers = HORIZONTAL)
    fun libroDetalleHorizontal() = capturar("libro_detalle_horizontal") {
        LibroScreen(LibroDetalleUiState(LIBROS[0], paginas, cargando = false), AccionesLibro(), ahora = AHORA)
    }
}

private const val MIN = 60_000L
private const val HORA = 60 * MIN
private const val AHORA = 1_793_487_600_000L // 31/10/2026 18:00 (Lima)
private val LIBROS = listOf(
    LibroUi(1, "Ciencia y Tecnología 5", "Ciencia y Tecnología", 12, AHORA - 2 * HORA),
    LibroUi(2, "Matemática 5", "Matemática", 8, AHORA - 50 * HORA),
    LibroUi(3, "Leyendas de mi comunidad", "Comunicación", 4, AHORA - 6 * 24 * HORA),
    LibroUi(4, "Matemática 4", "Matemática", 1, AHORA - 20 * 24 * HORA),
)

private val CONVERSACIONES = listOf(
    ConversacionResumen(1, "La fotosíntesis", AHORA - 5 * MIN, "Ciencia y Tecnología 5", "Ciencia y Tecnología"),
    ConversacionResumen(2, "Fracciones equivalentes", AHORA - 26 * HORA, "Matemática 5", "Matemática"),
    ConversacionResumen(3, "¿Por qué llueve?", AHORA - 3 * 24 * HORA),
)

private const val TABLETA = "w600dp-h960dp-port-mdpi"
private const val HORIZONTAL = "w960dp-h600dp-land-mdpi"
private const val TELEFONO = "w400dp-h860dp-port-xhdpi"
