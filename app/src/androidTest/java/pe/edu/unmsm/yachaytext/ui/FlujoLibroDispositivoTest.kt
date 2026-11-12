package pe.edu.unmsm.yachaytext.ui

import android.view.KeyEvent
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.edu.unmsm.yachaytext.MainActivity
import pe.edu.unmsm.yachaytext.YachayApplication
import pe.edu.unmsm.yachaytext.domain.ingesta.DestinoLibro

/**
 * En el dispositivo: libro guardado → detalle → página → pregunta → la cita abre la página.
 * Deja el libro guardado para poder revisar la app a mano después.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class FlujoLibroDispositivoTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    @Test
    fun delDetalleDelLibroALaCitaDeLaPagina() {
        val app = ApplicationProvider.getApplicationContext<YachayApplication>()
        app.container.preferencias.actualizar { it.copy(nombre = "Ana", primerUsoCompletado = true) }
        runBlocking {
            if (!app.container.libroRepository.hayLibros()) {
                app.container.ingestarPagina(
                    DestinoLibro.Nuevo("Ciencia y Tecnología 5", "Ciencia y Tecnología"), 42,
                    "Las plantas fabrican su propio alimento mediante la fotosíntesis. Para ello necesitan luz del sol, " +
                        "agua y dióxido de carbono. Las raíces absorben el agua y los minerales del suelo.",
                )
            }
        }

        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.onNodeWithText("Biblioteca").performClick()
            composeRule.onNodeWithText("Ciencia y Tecnología 5").performClick()
            composeRule.waitUntilAtLeastOneExists(hasText("Página 42"), 10_000)

            composeRule.onNodeWithText("Página 42").performClick()
            composeRule.waitUntilAtLeastOneExists(hasText("Borrar página"), 10_000)
            InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
            composeRule.waitUntilDoesNotExist(hasText("Borrar página"), 10_000)

            composeRule.onNodeWithText("Preguntar sobre este libro").performClick()
            composeRule.waitUntilAtLeastOneExists(hasText("Ponme a prueba"), 10_000)
            composeRule.onNode(hasSetTextAction()).performTextInput("¿Qué absorben las raíces?")
            composeRule.onNodeWithContentDescription("Enviar").performClick()
            composeRule.waitUntilAtLeastOneExists(hasText("Ciencia y Tecnología 5 · pág. 42"), 15_000)

            composeRule.onNodeWithContentDescription("Ver Ciencia y Tecnología 5 · pág. 42").performClick()
            composeRule.waitUntilAtLeastOneExists(hasText("dióxido de carbono", substring = true), 10_000)
        }
    }
}
