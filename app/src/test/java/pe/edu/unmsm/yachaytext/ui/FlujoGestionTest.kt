package pe.edu.unmsm.yachaytext.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pe.edu.unmsm.yachaytext.YachayApplication
import pe.edu.unmsm.yachaytext.data.local.RolMensaje
import pe.edu.unmsm.yachaytext.domain.ingesta.DestinoLibro
import pe.edu.unmsm.yachaytext.ui.escaneo.RevisionRoute
import pe.edu.unmsm.yachaytext.ui.theme.YachayTheme

/** Interacciones de la evaluación heurística: borrar, deshacer, renombrar, descartar y foco. */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w600dp-h960dp-port-mdpi")
class FlujoGestionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val app get() = ApplicationProvider.getApplicationContext<YachayApplication>()

    @Test
    fun borrarUnaConversacionSePuedeDeshacerYRenombrar() {
        runBlocking {
            val chat = app.container.chatRepository
            chat.guardarMensaje(chat.crearConversacion("Las nubes", "Hola"), RolMensaje.ESTUDIANTE, "¿De qué están hechas las nubes?")
        }
        composeRule.setContent { YachayTheme { YachayApp() } }
        composeRule.onNodeWithText("Tutor").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Las nubes"), 5_000)

        composeRule.onNodeWithContentDescription("Opciones de «Las nubes»").performClick()
        composeRule.onNodeWithText("Borrar").performClick()
        composeRule.waitUntilDoesNotExist(hasText("Las nubes"), 5_000)
        composeRule.onNodeWithText("Deshacer").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Las nubes"), 5_000)

        composeRule.onNodeWithContentDescription("Opciones de «Las nubes»").performClick()
        composeRule.onNodeWithText("Cambiar nombre").performClick()
        composeRule.onNode(hasSetTextAction()).performTextReplacement("La lluvia")
        composeRule.onNodeWithText("Guardar").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("La lluvia"), 5_000)
    }

    @Test
    fun unaPreguntaNuevaAbreElTecladoYLosAtajosSeOcultanDespues() {
        composeRule.setContent { YachayTheme { YachayApp() } }
        composeRule.onNodeWithText("Tutor").performClick()
        composeRule.onNodeWithText("Nueva pregunta").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Ponme a prueba"), 5_000)
        composeRule.waitForIdle()
        composeRule.onNode(hasSetTextAction()).assertIsFocused()

        composeRule.onNode(hasSetTextAction()).performTextInput("¿Por qué llueve?")
        composeRule.onNodeWithContentDescription("Enviar").performClick()
        composeRule.waitUntilDoesNotExist(hasText("Ponme a prueba"), 5_000)

        composeRule.onNodeWithText("Atajos").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Ponme a prueba"), 5_000)
    }

    @Test
    fun borrarUnLibroDesdeSuDetallePideConfirmacion() {
        runBlocking {
            app.container.ingestarPagina(DestinoLibro.Nuevo("Matemática 5", "Matemática"), 9, "Las fracciones representan partes de un todo.")
        }
        composeRule.setContent { YachayTheme { YachayApp() } }
        composeRule.onNodeWithText("Biblioteca").performClick()
        composeRule.onNodeWithText("Matemática 5").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Página 9"), 5_000)

        composeRule.onNodeWithContentDescription("Más opciones del libro").performClick()
        composeRule.onNodeWithText("Borrar libro").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("¿Borrar «Matemática 5»?"), 5_000)
        composeRule.onNodeWithText("Cancelar").performClick()
        composeRule.waitUntilDoesNotExist(hasText("¿Borrar «Matemática 5»?"), 5_000)

        composeRule.onNodeWithContentDescription("Más opciones del libro").performClick()
        composeRule.onNodeWithText("Borrar libro").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("¿Borrar «Matemática 5»?"), 5_000)
        composeRule.onNodeWithText("Borrar libro").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("Aún no tienes libros"), 5_000)
    }

    @Test
    fun salirDeLaRevisionConTextoPideConfirmacion() {
        app.container.borradorEscaneo.guardar("Las plantas fabrican su propio alimento.")
        var volvio = false
        composeRule.setContent { YachayTheme { RevisionRoute(onVolver = { volvio = true }, onAbrirChat = {}, onEscanearOtra = {}, onIrBiblioteca = {}) } }

        composeRule.onNodeWithContentDescription("Volver a la cámara").performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("¿Descartar esta página?"), 5_000)
        composeRule.onNodeWithText("Seguir revisando").performClick()
        assertFalse(volvio)

        composeRule.onNodeWithContentDescription("Volver a la cámara").performClick()
        composeRule.onNodeWithText("Descartar").performClick()
        composeRule.waitForIdle()
        assertTrue(volvio)
    }
}
