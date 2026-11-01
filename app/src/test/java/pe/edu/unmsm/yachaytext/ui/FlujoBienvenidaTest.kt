package pe.edu.unmsm.yachaytext.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pe.edu.unmsm.yachaytext.YachayApplication
import pe.edu.unmsm.yachaytext.domain.glosario.VarianteQuechua
import pe.edu.unmsm.yachaytext.ui.theme.YachayTheme

/** Primer uso → Inicio saluda por el nombre → Ajustes muestra y guarda las preferencias. */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w600dp-h960dp-port-mdpi")
class FlujoBienvenidaTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun elPerfilDelPrimerUsoLlegaAInicioYAjustes() {
        val preferencias = ApplicationProvider.getApplicationContext<YachayApplication>().container.preferencias
        composeRule.setContent { YachayTheme { YachayApp(rutaInicial = RUTA_BIENVENIDA) } }

        composeRule.onNode(hasSetTextAction()).performTextInput("Ana")
        composeRule.onNodeWithText("3.º de primaria").performClick()
        composeRule.onNodeWithText("Empezar").performScrollTo().performClick()

        composeRule.waitUntilAtLeastOneExists(hasText("Ana"), 5_000)
        assertTrue(preferencias.actual().primerUsoCompletado)

        composeRule.onAllNodesWithText("Ajustes").onFirst().performClick()
        composeRule.waitUntilAtLeastOneExists(hasText("3.º de primaria"), 5_000)
        composeRule.onNodeWithText("Chanka").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(VarianteQuechua.CHANKA, preferencias.actual().variante)
    }
}
