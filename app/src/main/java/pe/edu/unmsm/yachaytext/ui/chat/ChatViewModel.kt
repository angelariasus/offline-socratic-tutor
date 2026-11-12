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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.unmsm.yachaytext.YachayApplication
import pe.edu.unmsm.yachaytext.data.ChatRepository
import pe.edu.unmsm.yachaytext.data.LibroRepository
import pe.edu.unmsm.yachaytext.data.PreferenciasRepository
import pe.edu.unmsm.yachaytext.data.local.FuenteMensaje
import pe.edu.unmsm.yachaytext.data.local.RolMensaje
import pe.edu.unmsm.yachaytext.data.voz.Narrador
import pe.edu.unmsm.yachaytext.domain.glosario.GlosarioQuechua
import pe.edu.unmsm.yachaytext.domain.rag.BuscadorFragmentos
import pe.edu.unmsm.yachaytext.domain.tutor.IniciarConsulta
import pe.edu.unmsm.yachaytext.domain.tutor.Tutor
import pe.edu.unmsm.yachaytext.domain.tutor.Turno

class ChatViewModel(
    private val conversacionId: Long,
    private val repositorio: ChatRepository,
    private val libros: LibroRepository,
    private val buscador: BuscadorFragmentos,
    private val tutor: Tutor,
    private val glosario: GlosarioQuechua,
    private val narrador: Narrador,
    private val preferencias: PreferenciasRepository,
) : ViewModel() {

    private val _estado = MutableStateFlow(ChatUiState())
    val estado: StateFlow<ChatUiState> = _estado.asStateFlow()

    /** Libro de la conversación: limita la búsqueda a sus páginas. null = busca en toda la biblioteca. */
    private var libroId: Long? = null
    private var tituloLibro: String? = null

    init {
        viewModelScope.launch {
            preferencias.observar().collect { p -> _estado.update { it.copy(variante = p.variante) } }
        }
        viewModelScope.launch {
            repositorio.conversacion(conversacionId)?.let { c ->
                libroId = c.libroId
                val libro = c.libroId?.let { libros.obtener(it) }
                tituloLibro = libro?.titulo
                // El subtítulo muestra el libro cuando el título de la conversación es otro.
                _estado.update { it.copy(titulo = c.titulo, subtitulo = libro?.titulo?.takeIf { t -> t != c.titulo }, area = libro?.areaCurso) }
            }
        }
        viewModelScope.launch {
            combine(repositorio.observarMensajes(conversacionId), repositorio.observarFuentes(conversacionId)) { mensajes, fuentes ->
                val porMensaje = fuentes.groupBy { it.mensajeId }
                mensajes.map { m ->
                    val citadas = porMensaje[m.id].orEmpty()
                    val principal = citadas.maxByOrNull { it.puntaje }
                    MensajeUi(
                        id = m.id,
                        rol = m.rol,
                        texto = m.contenido,
                        porVoz = m.porVoz,
                        coincidencias = if (m.rol == RolMensaje.TUTOR) glosario.buscar(m.contenido) else emptyList(),
                        fuente = describirFuente(citadas),
                        fuenteLibroId = principal?.libroId,
                        fuentePaginas = citadas.filter { it.libroId == principal?.libroId }.map { it.nroPagina }.distinct().sorted(),
                    )
                }
            }.collect { mensajes -> _estado.update { it.copy(mensajes = mensajes) } }
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
                // La primera pregunta da nombre a la conversación (también si se abrió desde un libro,
                // que si no repetiría el título del libro en las listas).
                val provisional = _estado.value.titulo.let { it == IniciarConsulta.TITULO_PROVISIONAL || it == tituloLibro }
                if (provisional && historial.none { it.rol == RolMensaje.ESTUDIANTE }) {
                    val titulo = tituloDesde(texto)
                    repositorio.renombrar(conversacionId, titulo)
                    _estado.update { it.copy(titulo = titulo, subtitulo = tituloLibro) }
                }
                val contexto = buscador.buscar(texto, libroId)
                val respuesta = tutor.responder(historial, texto, contexto, preferencias.actual().nivelAyuda)
                val mensajeId = repositorio.guardarMensaje(conversacionId, RolMensaje.TUTOR, respuesta)
                repositorio.guardarContexto(mensajeId, contexto)
            } finally {
                _estado.update { it.copy(pensando = false) }
            }
        }
    }

    fun usarAtajo(atajo: Atajo) = enviar(atajo.consulta)

    /** Abre las páginas citadas en una respuesta, para leer la fuente completa. */
    fun abrirFuente(mensaje: MensajeUi) {
        val libroId = mensaje.fuenteLibroId ?: return
        viewModelScope.launch {
            val titulo = libros.obtener(libroId)?.titulo ?: return@launch
            val paginas = mensaje.fuentePaginas.mapNotNull { libros.pagina(libroId, it) }
            if (paginas.isNotEmpty()) _estado.update { it.copy(fuenteAbierta = FuenteAbierta(titulo, paginas)) }
        }
    }

    fun cerrarFuente() = _estado.update { it.copy(fuenteAbierta = null) }

    fun alternarLectura(mensaje: MensajeUi) {
        if (_estado.value.leyendoId == mensaje.id) {
            narrador.detener()
            _estado.update { it.copy(leyendoId = null) }
        } else {
            narrador.velocidad = preferencias.actual().velocidadVoz.factor
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
                with(app.container) { ChatViewModel(id, chatRepository, libroRepository, buscadorFragmentos, tutor, glosario, narrador, preferencias) }
            }
        }
    }
}

/** «Ciencia y Tecnología 5 · pág. 42» o «… · págs. 42 y 43», con el libro más relevante. */
internal fun describirFuente(fuentes: List<FuenteMensaje>): String? {
    val principal = fuentes.maxByOrNull { it.puntaje } ?: return null
    val paginas = fuentes.filter { it.titulo == principal.titulo }.map { it.nroPagina }.distinct().sorted()
    val texto = when (paginas.size) {
        1 -> "pág. ${paginas[0]}"
        else -> "págs. ${paginas.dropLast(1).joinToString(", ")} y ${paginas.last()}"
    }
    return "${principal.titulo} · $texto"
}

/** Primera pregunta como título: hasta 40 caracteres sin cortar palabras. */
internal fun tituloDesde(pregunta: String): String {
    val limpio = pregunta.trim().trimEnd('?', '.', '!').trimStart('¿', '¡').replaceFirstChar { it.uppercase() }
    if (limpio.length <= 40) return limpio
    return limpio.take(40).substringBeforeLast(' ').trimEnd(',', ' ') + "…"
}
