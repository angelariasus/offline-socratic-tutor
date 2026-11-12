package pe.edu.unmsm.yachaytext.ui.escaneo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.unmsm.yachaytext.YachayApplication
import pe.edu.unmsm.yachaytext.data.LibroRepository
import pe.edu.unmsm.yachaytext.domain.ingesta.AREAS_CURRICULARES
import pe.edu.unmsm.yachaytext.domain.ingesta.BorradorEscaneo
import pe.edu.unmsm.yachaytext.domain.ingesta.DestinoLibro
import pe.edu.unmsm.yachaytext.domain.ingesta.IngestarPagina
import pe.edu.unmsm.yachaytext.domain.tutor.IniciarConsulta
import pe.edu.unmsm.yachaytext.ui.common.BotonPrincipal
import pe.edu.unmsm.yachaytext.ui.common.BotonSecundario
import pe.edu.unmsm.yachaytext.ui.common.CampoTexto
import pe.edu.unmsm.yachaytext.ui.common.ChipSeleccion
import pe.edu.unmsm.yachaytext.ui.theme.Ink
import pe.edu.unmsm.yachaytext.ui.theme.Mist
import pe.edu.unmsm.yachaytext.ui.theme.Paper
import pe.edu.unmsm.yachaytext.ui.theme.TextSecondary
import pe.edu.unmsm.yachaytext.ui.theme.Brand
import pe.edu.unmsm.yachaytext.ui.theme.BrandSoft
import pe.edu.unmsm.yachaytext.ui.theme.Linen

data class LibroOpcion(val id: Long, val titulo: String)

sealed interface FaseRevision {
    data object Editando : FaseRevision
    data class Indexando(val hechos: Int, val total: Int) : FaseRevision
    data class Listo(val libroId: Long, val fragmentos: Int) : FaseRevision
}

data class RevisionUiState(
    val texto: String = "",
    val libros: List<LibroOpcion> = emptyList(),
    /** null = libro nuevo. */
    val libroElegido: Long? = null,
    val tituloNuevo: String = "",
    val areaNueva: String? = null,
    val pagina: String = "",
    val fase: FaseRevision = FaseRevision.Editando,
    val error: String? = null,
) {
    val paginaValida: Boolean get() = pagina.toIntOrNull()?.let { it in 1..9999 } == true
    val puedeGuardar: Boolean
        get() = texto.isNotBlank() && paginaValida && (libroElegido != null || (tituloNuevo.isNotBlank() && areaNueva != null))
}

class RevisionViewModel(
    private val borrador: BorradorEscaneo,
    libros: LibroRepository,
    private val ingestar: IngestarPagina,
    private val iniciarConsulta: IniciarConsulta,
) : ViewModel() {

    private val _estado = MutableStateFlow(RevisionUiState(texto = borrador.texto.value.orEmpty()))
    val estado: StateFlow<RevisionUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            var primeraVez = true
            libros.observarLibros().collect { lista ->
                val opciones = lista.map { LibroOpcion(it.id, it.titulo) }
                _estado.update { e ->
                    // La primera vez se propone el libro más reciente: lo habitual es seguir escaneando el mismo.
                    val elegido = if (primeraVez) opciones.firstOrNull()?.id else e.libroElegido
                    e.copy(libros = opciones, libroElegido = elegido)
                }
                primeraVez = false
            }
        }
    }

    fun alCambiarTexto(texto: String) = _estado.update { it.copy(texto = texto) }
    fun alElegirLibro(id: Long?) = _estado.update { it.copy(libroElegido = id) }
    fun alCambiarTitulo(titulo: String) = _estado.update { it.copy(tituloNuevo = titulo) }
    fun alElegirArea(area: String) = _estado.update { it.copy(areaNueva = area) }
    fun alCambiarPagina(pagina: String) = _estado.update { it.copy(pagina = pagina.filter(Char::isDigit).take(4)) }

    fun guardar() {
        val e = _estado.value
        if (!e.puedeGuardar) return
        val destino = e.libroElegido?.let { DestinoLibro.Existente(it) } ?: DestinoLibro.Nuevo(e.tituloNuevo, e.areaNueva!!)
        _estado.update { it.copy(fase = FaseRevision.Indexando(0, 0), error = null) }
        viewModelScope.launch {
            runCatching {
                ingestar(destino, e.pagina.toInt(), e.texto) { hechos, total ->
                    _estado.update { it.copy(fase = FaseRevision.Indexando(hechos, total)) }
                }
            }.onSuccess { r ->
                borrador.limpiar()
                _estado.update { it.copy(fase = FaseRevision.Listo(r.libroId, r.fragmentos)) }
            }.onFailure {
                _estado.update { it.copy(fase = FaseRevision.Editando, error = "No se pudo guardar la página. Inténtalo otra vez.") }
            }
        }
    }

    fun preguntar(alCrear: (Long) -> Unit) {
        val listo = _estado.value.fase as? FaseRevision.Listo ?: return
        viewModelScope.launch { alCrear(iniciarConsulta(listo.libroId)) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                with((this[APPLICATION_KEY] as YachayApplication).container) {
                    RevisionViewModel(borradorEscaneo, libroRepository, ingestarPagina, iniciarConsulta)
                }
            }
        }
    }
}

@Composable
fun RevisionRoute(
    onVolver: () -> Unit,
    onAbrirChat: (Long) -> Unit,
    onEscanearOtra: () -> Unit,
    onIrBiblioteca: () -> Unit,
    viewModel: RevisionViewModel = viewModel(factory = RevisionViewModel.Factory),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    RevisionScreen(
        estado = estado,
        acciones = AccionesRevision(
            onVolver = onVolver,
            onTexto = viewModel::alCambiarTexto,
            onLibro = viewModel::alElegirLibro,
            onTitulo = viewModel::alCambiarTitulo,
            onArea = viewModel::alElegirArea,
            onPagina = viewModel::alCambiarPagina,
            onGuardar = viewModel::guardar,
            onPreguntar = { viewModel.preguntar(onAbrirChat) },
            onEscanearOtra = onEscanearOtra,
            onIrBiblioteca = onIrBiblioteca,
        ),
    )
}

data class AccionesRevision(
    val onVolver: () -> Unit = {},
    val onTexto: (String) -> Unit = {},
    val onLibro: (Long?) -> Unit = {},
    val onTitulo: (String) -> Unit = {},
    val onArea: (String) -> Unit = {},
    val onPagina: (String) -> Unit = {},
    val onGuardar: () -> Unit = {},
    val onPreguntar: () -> Unit = {},
    val onEscanearOtra: () -> Unit = {},
    val onIrBiblioteca: () -> Unit = {},
)

@Composable
fun RevisionScreen(estado: RevisionUiState, acciones: AccionesRevision) {
    Box(Modifier.fillMaxSize().background(Linen).safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 720.dp).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 20.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (estado.fase !is FaseRevision.Indexando) {
                    IconButton(onClick = acciones.onVolver) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a la cámara") }
                } else {
                    Spacer(Modifier.width(48.dp))
                }
                Text("Revisar texto", style = MaterialTheme.typography.titleLarge)
            }
            when (val fase = estado.fase) {
                FaseRevision.Editando -> Formulario(estado, acciones)
                is FaseRevision.Indexando -> Indexando(fase)
                is FaseRevision.Listo -> Listo(fase, acciones)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Formulario(estado: RevisionUiState, acciones: AccionesRevision) {
    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(color = Mist, shape = RoundedCornerShape(16.dp)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = Ink)
                Spacer(Modifier.width(12.dp))
                Text("Corrige las palabras que la cámara leyó mal antes de guardar.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        CampoTexto(estado.texto, acciones.onTexto, "Escribe o pega el texto de la página", unaLinea = false, alturaMinima = 220.dp, modifier = Modifier.fillMaxWidth())

        Etiqueta("¿De qué libro es?")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            estado.libros.forEach { libro -> ChipSeleccion(libro.titulo, estado.libroElegido == libro.id, { acciones.onLibro(libro.id) }) }
            ChipSeleccion("Libro nuevo", estado.libroElegido == null, { acciones.onLibro(null) }, icono = Icons.Outlined.Add)
        }
        if (estado.libroElegido == null) {
            CampoTexto(estado.tituloNuevo, acciones.onTitulo, "Título del libro, por ejemplo «Ciencia y Tecnología 5»", modifier = Modifier.fillMaxWidth())
            Etiqueta("Área")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AREAS_CURRICULARES.forEach { area -> ChipSeleccion(area, estado.areaNueva == area, { acciones.onArea(area) }) }
            }
        }

        Etiqueta("Número de página")
        CampoTexto(
            estado.pagina,
            acciones.onPagina,
            "Ej. 42",
            teclado = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(160.dp),
        )

        if (estado.error != null) Text(estado.error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(4.dp))
        BotonPrincipal("Guardar página", acciones.onGuardar, habilitado = estado.puedeGuardar, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Etiqueta(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun Indexando(fase: FaseRevision.Indexando) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Preparando la página para Yachay…", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        val avance = if (fase.total == 0) 0f else fase.hechos.toFloat() / fase.total
        LinearProgressIndicator(progress = { avance }, modifier = Modifier.fillMaxWidth(0.7f).height(8.dp), color = Brand, trackColor = BrandSoft)
        Spacer(Modifier.height(12.dp))
        Text(
            if (fase.total == 0) "Dividiendo el texto en partes" else "Parte ${fase.hechos} de ${fase.total}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
    }
}

@Composable
private fun Listo(fase: FaseRevision.Listo, acciones: AccionesRevision) {
    Column(Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Brand, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(16.dp))
        Text("Página guardada", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Yachay ya puede usarla para ayudarte (${fase.fragmentos} ${if (fase.fragmentos == 1) "parte" else "partes"}).",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Column(Modifier.widthIn(max = 420.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BotonPrincipal("Preguntar sobre este libro", acciones.onPreguntar, icono = Icons.Outlined.Forum, modifier = Modifier.fillMaxWidth())
            BotonSecundario("Escanear otra página", acciones.onEscanearOtra, icono = Icons.Outlined.CameraAlt, modifier = Modifier.fillMaxWidth())
            BotonSecundario("Ir a la biblioteca", acciones.onIrBiblioteca, icono = Icons.AutoMirrored.Outlined.LibraryBooks, modifier = Modifier.fillMaxWidth())
        }
    }
}
