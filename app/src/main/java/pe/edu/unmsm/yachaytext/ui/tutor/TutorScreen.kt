package pe.edu.unmsm.yachaytext.ui.tutor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.edu.unmsm.yachaytext.YachayApplication
import pe.edu.unmsm.yachaytext.data.ChatRepository
import pe.edu.unmsm.yachaytext.data.CopiaConversacion
import pe.edu.unmsm.yachaytext.data.LibroRepository
import pe.edu.unmsm.yachaytext.domain.tutor.IniciarConsulta
import pe.edu.unmsm.yachaytext.ui.common.BotonPrincipal
import pe.edu.unmsm.yachaytext.ui.common.ConversacionResumen
import pe.edu.unmsm.yachaytext.ui.common.DialogoTexto
import pe.edu.unmsm.yachaytext.ui.common.Opcion
import pe.edu.unmsm.yachaytext.ui.common.TarjetaConversacion
import pe.edu.unmsm.yachaytext.ui.common.TituloSeccion
import pe.edu.unmsm.yachaytext.ui.common.resumir
import pe.edu.unmsm.yachaytext.ui.theme.Linen
import pe.edu.unmsm.yachaytext.ui.theme.TextSecondary

class TutorViewModel(
    private val repositorio: ChatRepository,
    libros: LibroRepository,
    private val iniciarConsulta: IniciarConsulta,
) : ViewModel() {

    val conversaciones: StateFlow<List<ConversacionResumen>> = combine(repositorio.observarConversaciones(), libros.observarLibros(), ::resumir)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Última conversación borrada, para ofrecer «Deshacer» (control y libertad del usuario). */
    private val _borrada = MutableStateFlow<CopiaConversacion?>(null)
    val borrada: StateFlow<CopiaConversacion?> = _borrada.asStateFlow()

    fun nuevaConsulta(alCrear: (Long) -> Unit) {
        viewModelScope.launch { alCrear(iniciarConsulta()) }
    }

    fun renombrar(id: Long, titulo: String) {
        if (titulo.isNotBlank()) viewModelScope.launch { repositorio.renombrar(id, titulo.trim()) }
    }

    fun borrar(id: Long) {
        viewModelScope.launch { _borrada.value = repositorio.borrar(id) }
    }

    fun deshacerBorrado() {
        val copia = _borrada.value ?: return
        _borrada.value = null
        viewModelScope.launch { repositorio.restaurar(copia) }
    }

    fun olvidarBorrado() {
        _borrada.value = null
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                with((this[APPLICATION_KEY] as YachayApplication).container) { TutorViewModel(chatRepository, libroRepository, iniciarConsulta) }
            }
        }
    }
}

data class AccionesTutor(
    val onAbrirChat: (Long) -> Unit = {},
    val onNueva: () -> Unit = {},
    val onRenombrar: (Long, String) -> Unit = { _, _ -> },
    val onBorrar: (Long) -> Unit = {},
)

@Composable
fun TutorRoute(onAbrirChat: (Long) -> Unit, viewModel: TutorViewModel = viewModel(factory = TutorViewModel.Factory)) {
    val conversaciones by viewModel.conversaciones.collectAsStateWithLifecycle()
    val borrada by viewModel.borrada.collectAsStateWithLifecycle()
    val avisos = remember { SnackbarHostState() }
    LaunchedEffect(borrada) {
        if (borrada == null) return@LaunchedEffect
        val r = avisos.showSnackbar("Conversación borrada", actionLabel = "Deshacer", duration = SnackbarDuration.Long)
        if (r == SnackbarResult.ActionPerformed) viewModel.deshacerBorrado() else viewModel.olvidarBorrado()
    }
    TutorScreen(
        conversaciones,
        AccionesTutor(onAbrirChat, onNueva = { viewModel.nuevaConsulta(onAbrirChat) }, viewModel::renombrar, viewModel::borrar),
        avisos,
    )
}

@Composable
fun TutorScreen(
    conversaciones: List<ConversacionResumen>,
    acciones: AccionesTutor,
    avisos: SnackbarHostState = remember { SnackbarHostState() },
    ahora: Long = System.currentTimeMillis(),
) {
    var renombrando by rememberSaveable { mutableStateOf<Long?>(null) }
    Box(Modifier.fillMaxSize().background(Linen).statusBarsPadding(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Tutor", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                Spacer(Modifier.height(20.dp))
                BotonPrincipal("Nueva pregunta", acciones.onNueva, icono = Icons.Outlined.Add, alto = 64.dp, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                TituloSeccion("Tus conversaciones")
            }
            if (conversaciones.isEmpty()) {
                item {
                    Text(
                        "Aún no tienes conversaciones. Toca «Nueva pregunta» para empezar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }
            items(conversaciones, key = { it.id }) { c ->
                TarjetaConversacion(
                    c.titulo,
                    c.detalle(ahora),
                    onClick = { acciones.onAbrirChat(c.id) },
                    area = c.area,
                    opciones = listOf(
                        Opcion("Cambiar nombre", Icons.Outlined.Edit) { renombrando = c.id },
                        Opcion("Borrar", Icons.Outlined.Delete, destructiva = true) { acciones.onBorrar(c.id) },
                    ),
                )
            }
        }
        SnackbarHost(avisos, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
    conversaciones.firstOrNull { it.id == renombrando }?.let { c ->
        DialogoTexto("Cambiar nombre", c.titulo, onGuardar = { acciones.onRenombrar(c.id, it); renombrando = null }, onCancelar = { renombrando = null })
    }
}
