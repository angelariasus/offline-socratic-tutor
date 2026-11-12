package pe.edu.unmsm.yachaytext.ui.ajustes

import androidx.compose.foundation.background
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.School
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import pe.edu.unmsm.yachaytext.ui.theme.BrandSoft
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.OfflinePin
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import pe.edu.unmsm.yachaytext.ui.common.DialogoAyuda
import pe.edu.unmsm.yachaytext.ui.common.SelectorGrado
import pe.edu.unmsm.yachaytext.ui.common.SeccionTarjeta
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import pe.edu.unmsm.yachaytext.YachayApplication
import pe.edu.unmsm.yachaytext.data.GRADOS
import pe.edu.unmsm.yachaytext.data.Preferencias
import pe.edu.unmsm.yachaytext.data.PreferenciasRepository
import pe.edu.unmsm.yachaytext.data.TamanoTexto
import pe.edu.unmsm.yachaytext.data.VelocidadVoz
import pe.edu.unmsm.yachaytext.data.voz.Narrador
import pe.edu.unmsm.yachaytext.domain.glosario.GlosarioQuechua
import pe.edu.unmsm.yachaytext.domain.glosario.VarianteQuechua
import pe.edu.unmsm.yachaytext.domain.tutor.NivelAyuda
import pe.edu.unmsm.yachaytext.ui.common.BotonSecundario
import pe.edu.unmsm.yachaytext.ui.common.CampoTexto
import pe.edu.unmsm.yachaytext.ui.common.ChipSeleccion
import pe.edu.unmsm.yachaytext.ui.theme.Ink
import pe.edu.unmsm.yachaytext.ui.theme.Mist
import pe.edu.unmsm.yachaytext.ui.theme.Paper
import pe.edu.unmsm.yachaytext.ui.theme.Quechua
import pe.edu.unmsm.yachaytext.ui.theme.TextSecondary
import pe.edu.unmsm.yachaytext.ui.theme.Brand
import pe.edu.unmsm.yachaytext.ui.theme.Linen

data class AjustesUiState(val preferencias: Preferencias = Preferencias(), val nombre: String = "")

class AjustesViewModel(private val repo: PreferenciasRepository, private val narrador: Narrador) : ViewModel() {

    // El nombre se edita en memoria para que el cursor no salte mientras se guarda.
    private val nombre = MutableStateFlow(repo.actual().nombre.orEmpty())

    val estado: StateFlow<AjustesUiState> = combine(repo.observar(), nombre) { p, n -> AjustesUiState(p, n) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AjustesUiState(repo.actual(), nombre.value))

    fun alCambiarNombre(texto: String) {
        nombre.value = texto
        repo.actualizar { it.copy(nombre = texto) }
    }

    fun alElegirGrado(grado: String) = repo.actualizar { it.copy(grado = grado) }
    fun alElegirVariante(v: VarianteQuechua) = repo.actualizar { it.copy(variante = v) }
    fun alElegirTamano(t: TamanoTexto) = repo.actualizar { it.copy(tamanoTexto = t) }
    fun alElegirAyuda(a: NivelAyuda) = repo.actualizar { it.copy(nivelAyuda = a) }

    fun alElegirVelocidad(v: VelocidadVoz) {
        repo.actualizar { it.copy(velocidadVoz = v) }
        probarVoz(v)
    }

    fun probarVoz(v: VelocidadVoz = repo.actual().velocidadVoz) {
        narrador.velocidad = v.factor
        narrador.leer(ID_PRUEBA, "Hola, soy Yachay. Así sonará mi voz cuando te lea las respuestas.")
    }

    override fun onCleared() = narrador.detener()

    companion object {
        private const val ID_PRUEBA = -1L

        val Factory = viewModelFactory {
            initializer {
                with((this[APPLICATION_KEY] as YachayApplication).container) { AjustesViewModel(preferencias, narrador) }
            }
        }
    }
}

data class AccionesAjustes(
    val onNombre: (String) -> Unit = {},
    val onGrado: (String) -> Unit = {},
    val onVariante: (VarianteQuechua) -> Unit = {},
    val onTamano: (TamanoTexto) -> Unit = {},
    val onVelocidad: (VelocidadVoz) -> Unit = {},
    val onProbarVoz: () -> Unit = {},
    val onAyuda: (NivelAyuda) -> Unit = {},
)

@Composable
fun AjustesRoute(viewModel: AjustesViewModel = viewModel(factory = AjustesViewModel.Factory)) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    AjustesScreen(
        estado,
        AccionesAjustes(
            onNombre = viewModel::alCambiarNombre,
            onGrado = viewModel::alElegirGrado,
            onVariante = viewModel::alElegirVariante,
            onTamano = viewModel::alElegirTamano,
            onVelocidad = viewModel::alElegirVelocidad,
            onProbarVoz = { viewModel.probarVoz() },
            onAyuda = viewModel::alElegirAyuda,
        ),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AjustesScreen(estado: AjustesUiState, acciones: AccionesAjustes) {
    val p = estado.preferencias
    var ayuda by rememberSaveable { mutableStateOf(false) }
    if (ayuda) DialogoAyuda(onCerrar = { ayuda = false })
    Box(Modifier.fillMaxSize().background(Linen).statusBarsPadding(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 720.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Cabecera(estado.nombre, p.grado)

            Seccion("Perfil", Icons.Outlined.Person) {
                Subtitulo("Nombre")
                CampoTexto(estado.nombre, acciones.onNombre, "Tu nombre", modifier = Modifier.fillMaxWidth())
                SelectorGrado(p.grado, acciones.onGrado)
            }

            Seccion("Quechua", Icons.Outlined.Translate) {
                Subtitulo("Variante para las traducciones")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VarianteQuechua.entries.forEach { v ->
                        ChipSeleccion(v.etiqueta.replaceFirstChar { it.uppercase() }, p.variante == v, { acciones.onVariante(v) })
                    }
                }
                val agua = GlosarioQuechua.BASE.first { it.castellano == "agua" }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Ejemplo: agua  →  ", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text(agua.en(p.variante), style = MaterialTheme.typography.bodyMedium, color = Quechua)
                }
            }

            Seccion("Presentación", Icons.Outlined.TextFields) {
                Subtitulo("Tamaño de texto")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TamanoTexto.entries.forEach { t -> ChipSeleccion(t.etiqueta, p.tamanoTexto == t, { acciones.onTamano(t) }) }
                }
                Text("Así se verá el texto en Yachay.", style = MaterialTheme.typography.bodyLarge)
            }

            Seccion("Voz", Icons.Outlined.RecordVoiceOver) {
                Subtitulo("Velocidad de lectura")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VelocidadVoz.entries.forEach { v -> ChipSeleccion(v.etiqueta, p.velocidadVoz == v, { acciones.onVelocidad(v) }) }
                }
                BotonSecundario("Probar voz", acciones.onProbarVoz, icono = Icons.AutoMirrored.Outlined.VolumeUp)
            }

            Seccion("Aprendizaje", Icons.Outlined.School) {
                Subtitulo("Nivel de ayuda del tutor")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NivelAyuda.entries.forEach { a -> ChipSeleccion(a.etiqueta, p.nivelAyuda == a, { acciones.onAyuda(a) }) }
                }
                Text(p.nivelAyuda.descripcion, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }

            Seccion("Ayuda", Icons.AutoMirrored.Outlined.HelpOutline) {
                Text("Repasa los pasos para usar Yachay con tus libros.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                BotonSecundario("Cómo usar Yachay", { ayuda = true }, icono = Icons.AutoMirrored.Outlined.HelpOutline)
            }

            Row(Modifier.padding(top = 8.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.OfflinePin, contentDescription = null, tint = Brand, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "YachayText 0.1.0 · Funciona sin internet. Tus datos se guardan solo en este dispositivo.",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun Cabecera(nombre: String, grado: String?) {
    // Mismo bloque verde que la portada de Inicio: el perfil es el ancla de Ajustes.
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(28.dp)).background(Brand).padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(color = Paper, shape = CircleShape, modifier = Modifier.size(64.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text(nombre.trim().firstOrNull()?.uppercase() ?: "Y", style = MaterialTheme.typography.headlineSmall, color = Brand)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(nombre.trim().ifEmpty { "Ajustes" }, style = MaterialTheme.typography.headlineMedium, color = Paper, modifier = Modifier.semantics { heading() })
            Text(grado ?: "Elige tu grado abajo", style = MaterialTheme.typography.bodyMedium, color = Paper.copy(alpha = 0.85f))
        }
    }
}

@Composable
private fun Seccion(titulo: String, icono: ImageVector, contenido: @Composable () -> Unit) =
    SeccionTarjeta(titulo, icono, contenido = contenido)

@Composable
private fun Subtitulo(texto: String) {
    Text(texto, style = MaterialTheme.typography.labelLarge)
}
