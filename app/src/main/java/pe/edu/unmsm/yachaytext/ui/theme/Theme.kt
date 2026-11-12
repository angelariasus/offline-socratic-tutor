package pe.edu.unmsm.yachaytext.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Brand,
    onPrimary = Paper,
    primaryContainer = BrandSoft,
    onPrimaryContainer = Brand,
    secondary = BrandDeep,
    onSecondary = Paper,
    tertiary = Quechua,
    tertiaryContainer = QuechuaContainer,
    onTertiaryContainer = Quechua,
    background = Linen,
    onBackground = Ink,
    surface = Linen,
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

/**
 * @param escalaTexto multiplica el tamaño de letra del sistema (Ajustes → Tamaño de texto).
 */
@Composable
fun YachayTheme(escalaTexto: Float = 1f, content: @Composable () -> Unit) {
    val densidad = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(densidad.density, densidad.fontScale * escalaTexto)) {
        // Solo tema claro por ahora; el tema oscuro queda pendiente.
        MaterialTheme(
            colorScheme = LightColors,
            typography = YachayTypography,
            shapes = YachayShapes,
            content = content,
        )
    }
}
