package pe.edu.unmsm.yachaytext.ui.biblioteca

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.unmsm.yachaytext.YachayApplication
import pe.edu.unmsm.yachaytext.data.LibroRepository
import pe.edu.unmsm.yachaytext.domain.tutor.IniciarConsulta
import pe.edu.unmsm.yachaytext.ui.common.BotonPrincipal
import pe.edu.unmsm.yachaytext.ui.common.EstadoVacio
import pe.edu.unmsm.yachaytext.ui.common.TarjetaLibro
import pe.edu.unmsm.yachaytext.ui.theme.Ink
import pe.edu.unmsm.yachaytext.ui.theme.Line
import pe.edu.unmsm.yachaytext.ui.theme.Mist
import pe.edu.unmsm.yachaytext.ui.theme.Paper
import pe.edu.unmsm.yachaytext.ui.theme.TextSecondary

data class BibliotecaUiState(
    val libros: List<LibroUi> = emptyList(),
    val consulta: String = "",
    val area: String? = null,
    val cargando: Boolean = true,
) {
    val areas: List<String> get() = areasDisponibles(libros)
    val visibles: List<LibroUi> get() = filtrarLibros(libros, consulta, area)
}

class BibliotecaViewModel(repositorio: LibroRepository, private val iniciarConsulta: IniciarConsulta) : ViewModel() {
    private val consulta = MutableStateFlow("")
    private val area = MutableStateFlow<String?>(null)

    val estado: StateFlow<BibliotecaUiState> = combine(repositorio.observarLibros(), consulta, area) { libros, q, a ->
        BibliotecaUiState(
            libros = libros.map { LibroUi(it.id, it.titulo, it.areaCurso, it.numPaginas, it.fechaIngesta) },
            consulta = q,
            area = a,
            cargando = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BibliotecaUiState())

    fun alBuscar(texto: String) = consulta.update { texto }

    /** Tocar el área activa la desmarca y vuelve a «Todas». */
    fun alElegirArea(nueva: String?) = area.update { actual -> if (actual == nueva) null else nueva }

    /** Mientras no exista la pantalla Detalle de libro, tocar un libro abre una consulta sobre él. */
    fun preguntarSobre(libroId: Long, alCrear: (Long) -> Unit) {
        viewModelScope.launch { alCrear(iniciarConsulta(libroId)) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                with((this[APPLICATION_KEY] as YachayApplication).container) { BibliotecaViewModel(libroRepository, iniciarConsulta) }
            }
        }
    }
}

@Composable
fun BibliotecaRoute(onEscanear: () -> Unit, onAbrirChat: (Long) -> Unit, viewModel: BibliotecaViewModel = viewModel(factory = BibliotecaViewModel.Factory)) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    BibliotecaScreen(estado, viewModel::alBuscar, viewModel::alElegirArea, onEscanear, onAbrirLibro = { viewModel.preguntarSobre(it, onAbrirChat) })
}

@Composable
fun BibliotecaScreen(
    estado: BibliotecaUiState,
    onBuscar: (String) -> Unit,
    onArea: (String?) -> Unit,
    onEscanear: () -> Unit,
    onAbrirLibro: (Long) -> Unit,
    ahora: Long = System.currentTimeMillis(),
) {
    Box(Modifier.fillMaxSize().background(Paper).statusBarsPadding(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 720.dp).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Biblioteca", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                if (estado.libros.isNotEmpty()) BotonPrincipal("Escanear", onEscanear, icono = Icons.Outlined.CameraAlt)
            }

            when {
                estado.cargando -> Unit
                estado.libros.isEmpty() -> EstadoVacio(
                    icono = Icons.AutoMirrored.Outlined.LibraryBooks,
                    titulo = "Aún no tienes libros",
                    texto = "Escanea una página de tu libro y Yachay te ayudará a entenderla, sin internet.",
                    accion = "Escanear página",
                    onAccion = onEscanear,
                    modifier = Modifier.padding(top = 48.dp),
                )
                else -> {
                    Buscador(estado.consulta, onBuscar, Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp))
                    FiltroAreas(estado.areas, estado.area, onArea)
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (estado.visibles.isEmpty()) {
                            item {
                                EstadoVacio(
                                    icono = Icons.Outlined.SearchOff,
                                    titulo = "No encontramos ese libro",
                                    texto = "Prueba con otra palabra o elige «Todas».",
                                    accion = "Ver todos",
                                    onAccion = { onBuscar(""); onArea(null) },
                                )
                            }
                        }
                        items(estado.visibles, key = { it.id }) { libro ->
                            TarjetaLibro(libro, ahora, onClick = { onAbrirLibro(libro.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Buscador(texto: String, onCambio: (String) -> Unit, modifier: Modifier = Modifier) {
    Surface(color = Mist, shape = RoundedCornerShape(50), modifier = modifier.fillMaxWidth().height(56.dp)) {
        Row(Modifier.padding(start = 20.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = TextSecondary)
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = texto,
                onValueChange = onCambio,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink),
                cursorBrush = SolidColor(Ink),
                modifier = Modifier.weight(1f),
                decorationBox = { campo ->
                    if (texto.isEmpty()) Text("Buscar libro o área", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                    campo()
                },
            )
            if (texto.isNotEmpty()) {
                IconButton(onClick = { onCambio("") }) { Icon(Icons.Outlined.Close, contentDescription = "Borrar búsqueda") }
            }
        }
    }
}

@Composable
private fun FiltroAreas(areas: List<String>, seleccionada: String?, onArea: (String?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Chip("Todas", seleccionada == null) { onArea(null) } }
        items(areas) { area -> Chip(area, seleccionada == area) { onArea(area) } }
    }
}

@Composable
private fun Chip(texto: String, activo: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (activo) Ink else Paper,
        border = if (activo) null else BorderStroke(1.dp, Line),
        modifier = Modifier.height(44.dp),
    ) {
        Box(Modifier.padding(horizontal = 18.dp), contentAlignment = Alignment.Center) {
            Text(texto, style = MaterialTheme.typography.labelLarge, color = if (activo) Paper else Ink)
        }
    }
}
