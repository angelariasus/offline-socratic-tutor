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
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import pe.edu.unmsm.yachaytext.YachayApplication
import pe.edu.unmsm.yachaytext.domain.ingesta.DestinoLibro
import pe.edu.unmsm.yachaytext.ui.theme.YachayTheme

/**
 * RAG de extremo a extremo en la app real: una página guardada en la biblioteca
 * → consulta desde el libro → el tutor cita la página → se muestra la fuente.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w600dp-h960dp-port-mdpi")
class FlujoRagTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun elTutorRespondeCitandoLaPaginaEscaneada() {
        val app = ApplicationProvider.getApplicationContext<YachayApplication>()
        runBlocking {
            app.container.ingestarPagina(
                DestinoLibro.Nuevo("Ciencia y Tecnología 5", "Ciencia y Tecnología"), 42,
                "Las plantas fabrican su propio alimento mediante la fotosíntesis. Para ello necesitan luz del sol, " +
                    "agua y dióxido de carbono. Las raíces absorben el agua y los minerales del suelo.",
            )
        }

        composeRule.setContent { YachayTheme { YachayApp() } }
        composeRule.onNodeWithText("Biblioteca").performClick()
        composeRule.onNodeWithText("Ciencia y Tecnología 5").performClick()
        // Detalle del libro: la página escaneada y la acción de preguntar.
        composeRule.waitUntilAtLeastOneExists(hasText("Página 42"), 5_000)
        composeRule.onNodeWithText("Preguntar sobre este libro").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Vamos a estudiar", substring = true), 5_000)

        composeRule.onNode(hasSetTextAction()).performTextInput("¿Qué absorben las raíces?")
        composeRule.onNodeWithContentDescription("Enviar").performClick()

        composeRule.waitUntilAtLeastOneExists(hasText("página 42", substring = true), 10_000)
        composeRule.waitUntilAtLeastOneExists(hasText("Ciencia y Tecnología 5 · pág. 42"), 5_000)
        // La primera pregunta da nombre a la conversación; el libro queda como subtítulo.
        composeRule.waitUntilAtLeastOneExists(hasText("Qué absorben las raíces"), 5_000)
        composeRule.onRoot().captureRoboImage("screenshots/flujo_rag.png")

        // La cita abre el texto completo de la página (transparencia de la fuente).
        composeRule.onNodeWithContentDescription("Ver Ciencia y Tecnología 5 · pág. 42").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("dióxido de carbono", substring = true), 5_000)
    }
}
