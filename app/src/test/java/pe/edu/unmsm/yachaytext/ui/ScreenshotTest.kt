package pe.edu.unmsm.yachaytext.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import pe.edu.unmsm.yachaytext.ui.biblioteca.BibliotecaScreen
import pe.edu.unmsm.yachaytext.ui.biblioteca.BibliotecaUiState
import pe.edu.unmsm.yachaytext.ui.biblioteca.LibroUi
import pe.edu.unmsm.yachaytext.ui.chat.ChatPreviewData
import pe.edu.unmsm.yachaytext.ui.inicio.ConversacionReciente
import pe.edu.unmsm.yachaytext.ui.inicio.InicioScreen
import pe.edu.unmsm.yachaytext.ui.inicio.InicioUiState
import pe.edu.unmsm.yachaytext.ui.chat.ChatScreen
import pe.edu.unmsm.yachaytext.ui.chat.ChatUiState
import pe.edu.unmsm.yachaytext.ui.theme.YachayTheme
import pe.edu.unmsm.yachaytext.ui.tutor.ConversacionResumen
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

    private fun capturar(nombre: String, contenido: @Composable () -> Unit) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { YachayTheme { contenido() } }
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
                conversaciones = listOf(
                    ConversacionReciente(1, "La fotosíntesis", AHORA - 5 * MIN),
                    ConversacionReciente(2, "Fracciones equivalentes", AHORA - 26 * HORA),
                ),
                libros = LIBROS.take(2),
                cargando = false,
            ),
            {}, {}, {}, {}, ahora = AHORA,
        )
    }

    @Test
    fun inicioVacio() = capturar("inicio_vacio") {
        InicioScreen(InicioUiState(cargando = false), {}, {}, {}, {}, ahora = AHORA)
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

    @Test
    fun tutorLista() = capturar("tutor_lista") {
        TutorScreen(
            listOf(ConversacionResumen(1, "La fotosíntesis"), ConversacionResumen(2, "Fracciones equivalentes"), ConversacionResumen(3, "Las partes de la célula")),
            onAbrirChat = {},
            onNueva = {},
        )
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

private const val TABLETA = "w600dp-h960dp-port-mdpi"
private const val TELEFONO = "w400dp-h860dp-port-xhdpi"
