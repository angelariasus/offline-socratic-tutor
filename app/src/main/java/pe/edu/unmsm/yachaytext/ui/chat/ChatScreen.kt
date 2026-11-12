package pe.edu.unmsm.yachaytext.ui.chat

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.outlined.Abc
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.OfflinePin
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import pe.edu.unmsm.yachaytext.ui.common.ChipSeleccion
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import pe.edu.unmsm.yachaytext.ui.libro.HojaPagina
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import pe.edu.unmsm.yachaytext.ui.common.PortadaLibro
import pe.edu.unmsm.yachaytext.ui.common.movimientoReducido
import pe.edu.unmsm.yachaytext.ui.theme.BrandSoft
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pe.edu.unmsm.yachaytext.data.local.RolMensaje
import pe.edu.unmsm.yachaytext.domain.glosario.VarianteQuechua
import pe.edu.unmsm.yachaytext.ui.theme.Danger
import pe.edu.unmsm.yachaytext.ui.theme.Ink
import pe.edu.unmsm.yachaytext.ui.theme.Line
import pe.edu.unmsm.yachaytext.ui.theme.Mist
import pe.edu.unmsm.yachaytext.ui.theme.Paper
import pe.edu.unmsm.yachaytext.ui.theme.Quechua
import pe.edu.unmsm.yachaytext.ui.theme.QuechuaContainer
import pe.edu.unmsm.yachaytext.ui.theme.TextDisabled
import pe.edu.unmsm.yachaytext.ui.theme.TextSecondary
import pe.edu.unmsm.yachaytext.ui.theme.YachayTheme
import pe.edu.unmsm.yachaytext.ui.theme.Brand
import pe.edu.unmsm.yachaytext.ui.theme.Linen

private val AnchoMaximo = 720.dp

@Composable
fun ChatRoute(onVolver: () -> Unit, viewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory)) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    ChatScreen(
        estado = estado,
        onVolver = onVolver,
        onCambiarEntrada = viewModel::alCambiarEntrada,
        onEnviar = { viewModel.enviar() },
        onAtajo = viewModel::usarAtajo,
        onEscuchar = viewModel::alternarLectura,
        // El dictado con Vosk llega en el Sprint 3 (RF04).
        onMicrofono = {},
        onAbrirFuente = viewModel::abrirFuente,
        onCerrarFuente = viewModel::cerrarFuente,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    estado: ChatUiState,
    onVolver: () -> Unit,
    onCambiarEntrada: (String) -> Unit,
    onEnviar: () -> Unit,
    onAtajo: (Atajo) -> Unit,
    onEscuchar: (MensajeUi) -> Unit,
    onMicrofono: () -> Unit,
    onAbrirFuente: (MensajeUi) -> Unit = {},
    onCerrarFuente: () -> Unit = {},
) {
    val lista = rememberLazyListState()
    val reducido = movimientoReducido()
    val foco = remember { FocusRequester() }
    LaunchedEffect(estado.sinPreguntas, estado.mensajes.isNotEmpty()) {
        if (estado.sinPreguntas && estado.mensajes.isNotEmpty()) runCatching { foco.requestFocus() }
    }
    val totalItems = estado.mensajes.size + if (estado.pensando) 1 else 0
    LaunchedEffect(totalItems) {
        if (totalItems > 0) lista.animateScrollToItem(totalItems - 1)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Linen)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.widthIn(max = AnchoMaximo).fillMaxSize()) {
            BarraSuperior(estado.titulo, estado.subtitulo, estado.area, onVolver)
            LazyColumn(
                state = lista,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                items(estado.mensajes, key = { it.id }) { mensaje ->
                    Box(if (reducido) Modifier else Modifier.animateItem(fadeInSpec = tween(220), placementSpec = spring(stiffness = Spring.StiffnessMediumLow), fadeOutSpec = null)) {
                    if (mensaje.rol == RolMensaje.TUTOR) {
                        MensajeTutor(
                            mensaje,
                            estado.variante,
                            leyendo = estado.leyendoId == mensaje.id,
                            onEscuchar = { onEscuchar(mensaje) },
                            onAbrirFuente = { onAbrirFuente(mensaje) },
                        )
                    } else {
                        MensajeEstudiante(mensaje)
                    }
                    }
                }
                if (estado.pensando) item(key = "pensando") { Pensando() }
            }
            // Los atajos ayudan a empezar; después se ocultan para dar espacio a la conversación.
            var verAtajos by rememberSaveable(estado.sinPreguntas) { mutableStateOf(estado.sinPreguntas) }
            if (verAtajos) Atajos(habilitados = !estado.pensando, onAtajo = { verAtajos = estado.sinPreguntas; onAtajo(it) })
            Compositor(
                estado, onCambiarEntrada, onEnviar, onMicrofono, foco,
                atajosVisibles = verAtajos,
                onAlternarAtajos = if (estado.sinPreguntas) null else { { verAtajos = !verAtajos } },
            )
        }
    }
    estado.fuenteAbierta?.let { f ->
        ModalBottomSheet(onDismissRequest = onCerrarFuente, containerColor = Paper) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                f.paginas.forEach { HojaPagina(it, titulo = f.titulo) }
            }
        }
    }
}

@Composable
private fun BarraSuperior(titulo: String, subtitulo: String?, area: String?, onVolver: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 4.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onVolver) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
        }
        if (area != null) {
            PortadaLibro(area, Modifier.width(34.dp))
            Spacer(Modifier.width(12.dp))
        }
        Column {
            Text(titulo, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitulo != null) {
                Text(subtitulo, style = MaterialTheme.typography.labelMedium, color = TextSecondary, maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MensajeTutor(mensaje: MensajeUi, variante: VarianteQuechua, leyendo: Boolean, onEscuchar: () -> Unit, onAbrirFuente: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(end = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(textoResaltado(mensaje), style = MaterialTheme.typography.bodyLarge)

        if (mensaje.terminos.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                mensaje.terminos.forEach { termino ->
                    Surface(color = QuechuaContainer, shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(termino.castellano, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                            Text("  →  ", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                            Text(termino.en(variante), style = MaterialTheme.typography.labelLarge, color = Quechua, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Acciones del mensaje, sin bordes para no competir con el texto.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (mensaje.fuente != null) {
                AccionMensaje(Icons.AutoMirrored.Outlined.MenuBook, mensaje.fuente, "Ver ${mensaje.fuente}", onAbrirFuente)
            }
            AccionMensaje(
                icono = if (leyendo) Icons.Outlined.StopCircle else Icons.AutoMirrored.Outlined.VolumeUp,
                texto = if (leyendo) "Detener" else "Escuchar",
                onClick = onEscuchar,
            )
        }
    }
}

@Composable
private fun AccionMensaje(icono: ImageVector, texto: String, descripcion: String? = null, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(50), color = Linen, modifier = Modifier.height(40.dp)) {
        Row(
            Modifier.padding(start = 8.dp, end = 12.dp).then(if (descripcion != null) Modifier.semantics { contentDescription = descripcion } else Modifier),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icono, contentDescription = null, tint = Brand, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(texto, style = MaterialTheme.typography.labelLarge, color = Brand)
        }
    }
}

private fun textoResaltado(mensaje: MensajeUi) = buildAnnotatedString {
    var cursor = 0
    mensaje.coincidencias.forEach { c ->
        append(mensaje.texto.substring(cursor, c.inicio))
        withStyle(SpanStyle(color = Quechua, background = QuechuaContainer, fontWeight = FontWeight.Medium)) {
            append(mensaje.texto.substring(c.inicio, c.fin))
        }
        cursor = c.fin
    }
    append(mensaje.texto.substring(cursor))
}

@Composable
private fun MensajeEstudiante(mensaje: MensajeUi) {
    Row(Modifier.fillMaxWidth().padding(start = 64.dp), horizontalArrangement = Arrangement.End) {
        Surface(color = BrandSoft, shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 22.dp, bottomEnd = 6.dp)) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                if (mensaje.porVoz) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Mic, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Por voz", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    }
                }
                Text(mensaje.texto, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun Pensando() {
    val transicion = rememberInfiniteTransition(label = "pensando")
    Row(
        Modifier.semantics { contentDescription = "Yachay está pensando" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { i ->
            val alfa by transicion.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(600, delayMillis = i * 200), RepeatMode.Reverse),
                label = "punto$i",
            )
            Box(Modifier.padding(end = 6.dp).size(10.dp).alpha(alfa).background(Brand, CircleShape))
        }
        Spacer(Modifier.width(6.dp))
        Text("Yachay está pensando…", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Atajos(habilitados: Boolean, onAtajo: (Atajo) -> Unit) {
    FlowRow(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Atajo.entries.forEach { atajo ->
            BotonPildora(
                icono = when (atajo) {
                    Atajo.Explicame -> Icons.Outlined.QuestionAnswer
                    Atajo.Quiz -> Icons.Outlined.Quiz
                    Atajo.Palabras -> Icons.Outlined.Abc
                },
                texto = atajo.etiqueta,
                habilitado = habilitados,
                onClick = { onAtajo(atajo) },
            )
        }
    }
}

@Composable
private fun Compositor(
    estado: ChatUiState,
    onCambiarEntrada: (String) -> Unit,
    onEnviar: () -> Unit,
    onMicrofono: () -> Unit,
    foco: FocusRequester,
    atajosVisibles: Boolean = false,
    onAlternarAtajos: (() -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp),
        shape = RoundedCornerShape(28.dp),
        color = Paper,
        shadowElevation = 4.dp,
    ) {
        Column(Modifier.padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 12.dp)) {
            if (estado.grabando) {
                Row(Modifier.height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).background(Danger, CircleShape))
                    Spacer(Modifier.width(10.dp))
                    Text("Te escucho… habla con calma", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                BasicTextField(
                    value = estado.entrada,
                    onValueChange = onCambiarEntrada,
                    modifier = Modifier.fillMaxWidth().padding(end = 8.dp).height(52.dp).focusRequester(foco),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink),
                    cursorBrush = SolidColor(Brand),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (estado.puedeEnviar) onEnviar() }),
                    decorationBox = { campo ->
                        if (estado.entrada.isEmpty()) {
                            Text("Pregunta lo que quieras…", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                        }
                        campo()
                    },
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                BotonCircular(
                    icono = Icons.Outlined.Mic,
                    descripcion = if (estado.grabando) "Terminar de dictar" else "Dictar por voz",
                    fondo = if (estado.grabando) Danger else Paper,
                    tinte = if (estado.grabando) Paper else Ink,
                    borde = !estado.grabando,
                    onClick = onMicrofono,
                )
                Spacer(Modifier.width(8.dp))
                if (onAlternarAtajos != null) {
                    ChipSeleccion("Atajos", atajosVisibles, onAlternarAtajos, icono = Icons.Outlined.Bolt)
                    Spacer(Modifier.width(8.dp))
                }
                // En pantallas angostas queda solo el ícono (con su descripción para TalkBack).
                BoxWithConstraints(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                    val cabeTexto = maxWidth >= 170.dp
                    Row(
                        Modifier.semantics(mergeDescendants = true) { if (!cabeTexto) contentDescription = "Funciona sin internet" },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.OfflinePin, contentDescription = null, tint = Brand, modifier = Modifier.size(18.dp))
                        if (cabeTexto) {
                            Spacer(Modifier.width(6.dp))
                            Text("Funciona sin internet", style = MaterialTheme.typography.labelMedium, color = TextSecondary, maxLines = 1)
                        }
                    }
                }
                BotonCircular(
                    icono = Icons.Filled.ArrowUpward,
                    descripcion = "Enviar",
                    fondo = if (estado.puedeEnviar) Brand else Line,
                    tinte = if (estado.puedeEnviar) Paper else TextDisabled,
                    borde = false,
                    habilitado = estado.puedeEnviar,
                    onClick = onEnviar,
                )
            }
        }
    }
}

@Composable
private fun BotonCircular(
    icono: ImageVector,
    descripcion: String,
    fondo: androidx.compose.ui.graphics.Color,
    tinte: androidx.compose.ui.graphics.Color,
    borde: Boolean,
    habilitado: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = habilitado,
        shape = CircleShape,
        color = fondo,
        border = if (borde) BorderStroke(1.dp, Line) else null,
        modifier = Modifier.size(48.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icono, contentDescription = descripcion, tint = tinte)
        }
    }
}

@Composable
private fun BotonPildora(icono: ImageVector, texto: String, habilitado: Boolean = true, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = habilitado,
        shape = RoundedCornerShape(50),
        color = Paper,
        border = BorderStroke(1.dp, Line),
        modifier = Modifier.height(44.dp),
    ) {
        Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (habilitado) Ink else TextDisabled)
            Spacer(Modifier.width(8.dp))
            Text(texto, style = MaterialTheme.typography.labelLarge, color = if (habilitado) Ink else TextDisabled)
        }
    }
}

@Preview(name = "Chat", widthDp = 600, heightDp = 960)
@Composable
private fun ChatPreview() {
    YachayTheme {
        ChatScreen(ChatPreviewData.conversacion, {}, {}, {}, {}, {}, {})
    }
}
