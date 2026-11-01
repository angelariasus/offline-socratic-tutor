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
import pe.edu.unmsm.yachaytext.ui.chat.ChatPreviewData
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
    fun tutorLista() = capturar("tutor_lista") {
        TutorScreen(
            listOf(ConversacionResumen(1, "La fotosíntesis"), ConversacionResumen(2, "Fracciones equivalentes"), ConversacionResumen(3, "Las partes de la célula")),
            onAbrirChat = {},
            onNueva = {},
        )
    }
}

private const val TABLETA = "w600dp-h960dp-port-mdpi"
private const val TELEFONO = "w400dp-h860dp-port-xhdpi"
