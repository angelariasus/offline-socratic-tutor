package pe.edu.unmsm.yachaytext

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import pe.edu.unmsm.yachaytext.ui.YachayApp
import pe.edu.unmsm.yachaytext.ui.theme.YachayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YachayTheme {
                YachayApp()
            }
        }
    }
}
