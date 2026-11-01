package pe.edu.unmsm.yachaytext.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import pe.edu.unmsm.yachaytext.ui.theme.YachayTheme

/** Recorre la app real: pestaña Tutor → nueva consulta → pregunta → respuesta guardada en Room. */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w600dp-h960dp-port-mdpi")
class FlujoChatTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun preguntarAlTutorDevuelveUnaPreguntaSocratica() {
        composeRule.setContent { YachayTheme { YachayApp() } }

        composeRule.onNodeWithText("Tutor").performClick()
        composeRule.onNodeWithText("Nueva consulta").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("¿Qué crees que necesita", substring = true), 5_000)

        composeRule.onNode(hasSetTextAction()).performTextInput("¿luz?")
        composeRule.onNodeWithContentDescription("Enviar").performClick()

        composeRule.waitUntilAtLeastOneExists(hasText("¿Y de dónde viene esa luz", substring = true), 10_000)
        composeRule.onRoot().captureRoboImage("screenshots/flujo_chat.png")
    }
}
