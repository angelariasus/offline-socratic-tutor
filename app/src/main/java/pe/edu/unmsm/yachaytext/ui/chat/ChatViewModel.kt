package pe.edu.unmsm.yachaytext.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.unmsm.yachaytext.YachayApplication
import pe.edu.unmsm.yachaytext.data.ChatRepository
import pe.edu.unmsm.yachaytext.data.local.RolMensaje
import pe.edu.unmsm.yachaytext.data.voz.Narrador
import pe.edu.unmsm.yachaytext.domain.glosario.GlosarioQuechua
import pe.edu.unmsm.yachaytext.domain.tutor.Tutor
import pe.edu.unmsm.yachaytext.domain.tutor.Turno

class ChatViewModel(
    private val conversacionId: Long,
    private val repositorio: ChatRepository,
    private val tutor: Tutor,
    private val glosario: GlosarioQuechua,
    private val narrador: Narrador,
) : ViewModel() {

    private val _estado = MutableStateFlow(ChatUiState())
    val estado: StateFlow<ChatUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            repositorio.conversacion(conversacionId)?.let { c -> _estado.update { it.copy(titulo = c.titulo) } }
        }
        viewModelScope.launch {
            repositorio.observarMensajes(conversacionId).collect { mensajes ->
                _estado.update { estado ->
                    estado.copy(
                        mensajes = mensajes.map { m ->
                            MensajeUi(
                                id = m.id,
                                rol = m.rol,
                                texto = m.contenido,
                                porVoz = m.porVoz,
                                coincidencias = if (m.rol == RolMensaje.TUTOR) glosario.buscar(m.contenido) else emptyList(),
                            )
                        },
                    )
                }
            }
        }
    }

    fun alCambiarEntrada(texto: String) = _estado.update { it.copy(entrada = texto) }

    fun enviar(consulta: String = _estado.value.entrada, porVoz: Boolean = false) {
        val texto = consulta.trim()
        if (texto.isEmpty() || _estado.value.pensando) return
        val historial = _estado.value.mensajes.map { Turno(it.rol, it.texto) }
        _estado.update { it.copy(entrada = "", pensando = true) }
        viewModelScope.launch {
            try {
                repositorio.guardarMensaje(conversacionId, RolMensaje.ESTUDIANTE, texto, porVoz)
                val respuesta = tutor.responder(historial, texto)
                repositorio.guardarMensaje(conversacionId, RolMensaje.TUTOR, respuesta)
            } finally {
                _estado.update { it.copy(pensando = false) }
            }
        }
    }

    fun usarAtajo(atajo: Atajo) = enviar(atajo.consulta)

    fun alternarLectura(mensaje: MensajeUi) {
        if (_estado.value.leyendoId == mensaje.id) {
            narrador.detener()
            _estado.update { it.copy(leyendoId = null) }
        } else {
            narrador.leer(mensaje.id, mensaje.texto)
            _estado.update { it.copy(leyendoId = mensaje.id) }
        }
    }

    override fun onCleared() {
        narrador.detener()
    }

    companion object {
        const val ARG_CONVERSACION = "conversacionId"

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as YachayApplication
                val id = createSavedStateHandle().get<Long>(ARG_CONVERSACION) ?: error("Falta $ARG_CONVERSACION")
                with(app.container) { ChatViewModel(id, chatRepository, tutor, glosario, narrador) }
            }
        }
    }
}
