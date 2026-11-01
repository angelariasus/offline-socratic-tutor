package pe.edu.unmsm.yachaytext

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.edu.unmsm.yachaytext.ui.RUTA_BIENVENIDA
import pe.edu.unmsm.yachaytext.ui.TopLevelDestination
import pe.edu.unmsm.yachaytext.ui.YachayApp
import pe.edu.unmsm.yachaytext.ui.theme.YachayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preferencias = (application as YachayApplication).container.preferencias
        // La bienvenida solo aparece hasta completar (u omitir) el primer uso.
        val rutaInicial = if (preferencias.actual().primerUsoCompletado) TopLevelDestination.Inicio.route else RUTA_BIENVENIDA
        setContent {
            val flujo = remember { preferencias.observar() }
            val p by flujo.collectAsStateWithLifecycle(preferencias.actual())
            YachayTheme(escalaTexto = p.tamanoTexto.escala) {
                YachayApp(rutaInicial = rutaInicial)
            }
        }
    }
}
