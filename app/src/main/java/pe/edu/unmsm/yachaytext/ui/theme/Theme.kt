package pe.edu.unmsm.yachaytext.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    primaryContainer = Mist,
    onPrimaryContainer = Ink,
    secondary = InkSoft,
    onSecondary = Paper,
    tertiary = Quechua,
    tertiaryContainer = QuechuaContainer,
    onTertiaryContainer = Quechua,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Mist,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = Mist,
    surfaceContainerHigh = Mist,
    outline = Line,
    outlineVariant = Line,
    error = Danger,
)

// Radios de Figma: tarjetas ~20 dp, campos ~30 dp (píldora).
private val YachayShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(40.dp),
)

@Composable
fun YachayTheme(content: @Composable () -> Unit) {
    // Solo tema claro por ahora; "Apariencia" en Ajustes podrá activar el oscuro.
    MaterialTheme(
        colorScheme = LightColors,
        typography = YachayTypography,
        shapes = YachayShapes,
        content = content,
    )
}
