package pe.edu.unmsm.yachaytext.ui.libro

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.edu.unmsm.yachaytext.YachayApplication
import pe.edu.unmsm.yachaytext.data.LibroRepository
import pe.edu.unmsm.yachaytext.data.Pagina
import pe.edu.unmsm.yachaytext.domain.ingesta.BorradorEscaneo
import pe.edu.unmsm.yachaytext.domain.tutor.IniciarConsulta
import pe.edu.unmsm.yachaytext.ui.biblioteca.LibroUi
import pe.edu.unmsm.yachaytext.ui.common.BotonPrincipal
import pe.edu.unmsm.yachaytext.ui.common.BotonSecundario
import pe.edu.unmsm.yachaytext.ui.common.Dato
import pe.edu.unmsm.yachaytext.ui.common.DialogoConfirmar
import pe.edu.unmsm.yachaytext.ui.common.DialogoTexto
import pe.edu.unmsm.yachaytext.ui.common.EstadoVacio
import pe.edu.unmsm.yachaytext.ui.common.MenuOpciones
import pe.edu.unmsm.yachaytext.ui.common.Opcion
import pe.edu.unmsm.yachaytext.ui.common.PortadaArea
import pe.edu.unmsm.yachaytext.ui.common.TituloSeccion
import pe.edu.unmsm.yachaytext.ui.common.textoPaginas
import pe.edu.unmsm.yachaytext.ui.common.tiempoRelativo
import pe.edu.unmsm.yachaytext.ui.theme.Line
import pe.edu.unmsm.yachaytext.ui.theme.Linen
import pe.edu.unmsm.yachaytext.ui.theme.Paper
import pe.edu.unmsm.yachaytext.ui.theme.TextSecondary

data class LibroDetalleUiState(
    val libro: LibroUi? = null,
    val paginas: List<Pagina> = emptyList(),
    val cargando: Boolean = true,
)

class LibroViewModel(
    private val libroId: Long,
    private val libros: LibroRepository,
    private val iniciarConsulta: IniciarConsulta,
    private val borrador: BorradorEscaneo,
) : ViewModel() {

    val estado: StateFlow<LibroDetalleUiState> = combine(libros.observarLibro(libroId), libros.observarPaginas(libroId)) { l, p ->
        LibroDetalleUiState(
            libro = l?.let { LibroUi(it.id, it.titulo, it.areaCurso, it.numPaginas, it.fechaIngesta) },
            paginas = p.sortedBy { it.numero },
            cargando = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibroDetalleUiState())

    fun preguntar(alCrear: (Long) -> Unit) {
        viewModelScope.launch { alCrear(iniciarConsulta(libroId)) }
    }

    /** La revisión propondrá este libro al guardar la página escaneada. */
    fun prepararEscaneo() {
        borrador.libroPreferido = libroId
    }

    fun renombrar(titulo: String) {
        if (titulo.isNotBlank()) viewModelScope.launch { libros.renombrar(libroId, titulo) }
    }

    fun borrarLibro(alTerminar: () -> Unit) {
        viewModelScope.launch {
            libros.borrar(libroId)
            alTerminar()
        }
    }

    fun borrarPagina(numero: Int) {
        viewModelScope.launch { libros.borrarPagina(libroId, numero) }
    }

    companion object {
        const val ARG_LIBRO = "libroId"

        val Factory = viewModelFactory {
            initializer {
                val id = createSavedStateHandle().get<Long>(ARG_LIBRO) ?: error("Falta $ARG_LIBRO")
                with((this[APPLICATION_KEY] as YachayApplication).container) {
                    LibroViewModel(id, libroRepository, iniciarConsulta, borradorEscaneo)
                }
            }
        }
    }
}

data class AccionesLibro(
    val onVolver: () -> Unit = {},
    val onPreguntar: () -> Unit = {},
    val onEscanear: () -> Unit = {},
    val onRenombrar: (String) -> Unit = {},
    val onBorrarLibro: () -> Unit = {},
    val onBorrarPagina: (Int) -> Unit = {},
)

@Composable
fun LibroRoute(
    onVolver: () -> Unit,
    onAbrirChat: (Long) -> Unit,
    onEscanear: () -> Unit,
    viewModel: LibroViewModel = viewModel(factory = LibroViewModel.Factory),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    LibroScreen(
        estado,
        AccionesLibro(
            onVolver = onVolver,
            onPreguntar = { viewModel.preguntar(onAbrirChat) },
            onEscanear = { viewModel.prepararEscaneo(); onEscanear() },
            onRenombrar = viewModel::renombrar,
            onBorrarLibro = { viewModel.borrarLibro(alTerminar = onVolver) },
            onBorrarPagina = viewModel::borrarPagina,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibroScreen(estado: LibroDetalleUiState, acciones: AccionesLibro, ahora: Long = System.currentTimeMillis()) {
    var renombrando by rememberSaveable { mutableStateOf(false) }
    var borrandoLibro by rememberSaveable { mutableStateOf(false) }
    var paginaAbierta by rememberSaveable { mutableStateOf<Int?>(null) }
    var borrandoPagina by rememberSaveable { mutableStateOf<Int?>(null) }
    val libro = estado.libro

    Box(Modifier.fillMaxSize().background(Linen).safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 1080.dp).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = acciones.onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                Spacer(Modifier.weight(1f))
                if (libro != null) {
                    MenuOpciones(
                        "Más opciones del libro",
                        listOf(
                            Opcion("Cambiar nombre", Icons.Outlined.Edit) { renombrando = true },
                            Opcion("Borrar libro", Icons.Outlined.Delete, destructiva = true) { borrandoLibro = true },
                        ),
                    )
                }
            }
            if (libro == null) return@Column

            LazyVerticalGrid(
                columns = GridCells.Adaptive(300.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) { Cabecera(libro, ahora, acciones) }
                item(span = { GridItemSpan(maxLineSpan) }) { TituloSeccion("Páginas escaneadas") }
                if (estado.paginas.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EstadoVacio(
                            icono = Icons.Outlined.Description,
                            titulo = "Este libro no tiene páginas",
                            texto = "Escanea una página para que Yachay pueda ayudarte con este libro.",
                            accion = "Escanear página",
                            onAccion = acciones.onEscanear,
                        )
                    }
                }
                items(estado.paginas, key = { it.numero }) { p -> TarjetaPagina(p) { paginaAbierta = p.numero } }
            }
        }
    }

    if (renombrando && libro != null) {
        DialogoTexto("Cambiar nombre del libro", libro.titulo, onGuardar = { acciones.onRenombrar(it); renombrando = false }, onCancelar = { renombrando = false })
    }
    if (borrandoLibro && libro != null) {
        DialogoConfirmar(
            titulo = "¿Borrar «${libro.titulo}»?",
            texto = "Se borrarán sus ${textoPaginas(estado.paginas.size)}. Tus conversaciones se conservan.",
            confirmar = "Borrar libro",
            onConfirmar = { borrandoLibro = false; acciones.onBorrarLibro() },
            onCancelar = { borrandoLibro = false },
        )
    }
    val abierta = estado.paginas.firstOrNull { it.numero == paginaAbierta }
    if (abierta != null) {
        ModalBottomSheet(onDismissRequest = { paginaAbierta = null }, containerColor = Paper) {
            HojaPagina(abierta, onBorrar = { borrandoPagina = abierta.numero })
        }
    }
    borrandoPagina?.let { n ->
        DialogoConfirmar(
            titulo = "¿Borrar la página $n?",
            texto = "Yachay ya no podrá usarla para ayudarte. Puedes volver a escanearla cuando quieras.",
            confirmar = "Borrar página",
            onConfirmar = { borrandoPagina = null; paginaAbierta = null; acciones.onBorrarPagina(n) },
            onCancelar = { borrandoPagina = null },
        )
    }
}

@Composable
private fun Cabecera(libro: LibroUi, ahora: Long, acciones: AccionesLibro) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PortadaArea(libro.area, tamano = 88.dp)
            Spacer(Modifier.width(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(libro.titulo, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                Text(libro.area, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Dato(Icons.Outlined.Description, textoPaginas(libro.paginas))
                    Dato(Icons.Outlined.Schedule, tiempoRelativo(libro.fechaIngesta, ahora))
                }
            }
        }
        // En el teléfono los botones se apilan para que el texto no se parta en dos líneas.
        BoxWithConstraints {
            if (maxWidth >= 520.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BotonPrincipal("Preguntar sobre este libro", acciones.onPreguntar, icono = Icons.Outlined.Forum, modifier = Modifier.weight(1f))
                    BotonSecundario("Escanear página", acciones.onEscanear, icono = Icons.Outlined.CameraAlt, modifier = Modifier.weight(1f))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BotonPrincipal("Preguntar sobre este libro", acciones.onPreguntar, icono = Icons.Outlined.Forum, modifier = Modifier.fillMaxWidth())
                    BotonSecundario("Escanear página", acciones.onEscanear, icono = Icons.Outlined.CameraAlt, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun TarjetaPagina(pagina: Pagina, onClick: () -> Unit) {
    // Altura mínima de tres líneas de vista previa: las tarjetas de una fila quedan parejas.
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Line), color = Paper, modifier = Modifier.fillMaxWidth().heightIn(min = 124.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Página ${pagina.numero}", style = MaterialTheme.typography.titleMedium)
            Text(pagina.texto, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Texto completo de una página: lo que Yachay usa para ayudar (transparencia de la fuente). */
@Composable
fun HojaPagina(pagina: Pagina, titulo: String? = null, onBorrar: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
        if (titulo != null) Text(titulo, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text("Página ${pagina.numero}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(12.dp))
        Text(pagina.texto, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()))
        if (onBorrar != null) {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onBorrar) {
                Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text("Borrar página", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
