package pe.edu.unmsm.yachaytext.ui.tutor

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import pe.edu.unmsm.yachaytext.YachayApplication
import pe.edu.unmsm.yachaytext.data.ChatRepository
import pe.edu.unmsm.yachaytext.domain.tutor.IniciarConsulta
import pe.edu.unmsm.yachaytext.ui.theme.Line
import pe.edu.unmsm.yachaytext.ui.theme.Paper
import pe.edu.unmsm.yachaytext.ui.theme.TextSecondary
import pe.edu.unmsm.yachaytext.ui.theme.Linen

data class ConversacionResumen(val id: Long, val titulo: String)

class TutorViewModel(repositorio: ChatRepository, private val iniciarConsulta: IniciarConsulta) : ViewModel() {

    val conversaciones: StateFlow<List<ConversacionResumen>> = repositorio.observarConversaciones()
        .map { lista -> lista.map { ConversacionResumen(it.id, it.titulo) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun nuevaConsulta(alCrear: (Long) -> Unit) {
        viewModelScope.launch { alCrear(iniciarConsulta()) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as YachayApplication
                TutorViewModel(app.container.chatRepository, app.container.iniciarConsulta)
            }
        }
    }
}

@Composable
fun TutorRoute(onAbrirChat: (Long) -> Unit, viewModel: TutorViewModel = viewModel(factory = TutorViewModel.Factory)) {
    val conversaciones by viewModel.conversaciones.collectAsStateWithLifecycle()
    TutorScreen(conversaciones, onAbrirChat, onNueva = { viewModel.nuevaConsulta(onAbrirChat) })
}

@Composable
fun TutorScreen(conversaciones: List<ConversacionResumen>, onAbrirChat: (Long) -> Unit, onNueva: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Linen).statusBarsPadding(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 720.dp).fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Tutor", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(20.dp))
                Surface(
                    onClick = onNueva,
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                ) {
                    Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Add, contentDescription = null, tint = Paper)
                        Spacer(Modifier.width(12.dp))
                        Text("Nueva consulta", style = MaterialTheme.typography.titleMedium, color = Paper)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text("Conversaciones recientes", style = MaterialTheme.typography.titleMedium)
            }
            if (conversaciones.isEmpty()) {
                item {
                    Text(
                        "Aún no tienes conversaciones. Toca «Nueva consulta» para empezar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                }
            }
            items(conversaciones, key = { it.id }) { c ->
                Surface(
                    onClick = { onAbrirChat(c.id) },
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Line),
                    color = Paper,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(c.titulo, style = MaterialTheme.typography.titleMedium)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}
