package pe.edu.unmsm.yachaytext.ui.escaneo

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FlashlightOff
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.NoPhotography
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.edu.unmsm.yachaytext.YachayApplication
import pe.edu.unmsm.yachaytext.data.ocr.LectorTexto
import pe.edu.unmsm.yachaytext.domain.ingesta.BorradorEscaneo
import pe.edu.unmsm.yachaytext.ui.common.EstadoVacio
import pe.edu.unmsm.yachaytext.ui.theme.Paper
import pe.edu.unmsm.yachaytext.ui.theme.Linen

enum class EtapaEscaneo(val mensaje: String) {
    Tomando("Tomando la foto…"),
    Leyendo("Leyendo el texto…"),
}

data class EscaneoUiState(
    val etapa: EtapaEscaneo? = null,
    val linterna: Boolean = false,
    val aviso: String? = null,
) {
    val ocupado: Boolean get() = etapa != null
}

class EscaneoViewModel(private val lector: LectorTexto, private val borrador: BorradorEscaneo) : ViewModel() {
    private val _estado = MutableStateFlow(EscaneoUiState())
    val estado: StateFlow<EscaneoUiState> = _estado.asStateFlow()

    fun alternarLinterna() = _estado.update { it.copy(linterna = !it.linterna) }

    private var espera: Job? = null

    /** Respuesta inmediata al tocar el disparador; si la cámara no entrega la foto a tiempo, se avisa. */
    fun alDisparar() {
        _estado.update { it.copy(etapa = EtapaEscaneo.Tomando, aviso = null) }
        espera?.cancel()
        espera = viewModelScope.launch {
            delay(ESPERA_MAXIMA_MS)
            if (_estado.value.etapa == EtapaEscaneo.Tomando) alFallarCaptura()
        }
    }

    fun alCapturar(imagen: ImageProxy, alLeer: () -> Unit) {
        espera?.cancel()
        _estado.update { it.copy(etapa = EtapaEscaneo.Leyendo, aviso = null) }
        viewModelScope.launch {
            val texto = runCatching { lector.leer(imagen) }.getOrNull()
            when {
                texto == null -> _estado.update { it.copy(etapa = null, aviso = "No pudimos leer la foto. Inténtalo otra vez.") }
                texto.isBlank() -> _estado.update { it.copy(etapa = null, aviso = "No encontramos texto. Acerca la página y busca más luz.") }
                else -> {
                    borrador.guardar(texto)
                    _estado.update { it.copy(etapa = null) }
                    alLeer()
                }
            }
        }
    }

    fun alFallarCaptura() {
        espera?.cancel()
        _estado.update { it.copy(etapa = null, aviso = "No se pudo tomar la foto. Inténtalo otra vez.") }
    }

    companion object {
        const val ESPERA_MAXIMA_MS = 12_000L

        val Factory = viewModelFactory {
            initializer {
                with((this[APPLICATION_KEY] as YachayApplication).container) { EscaneoViewModel(lectorTexto, borradorEscaneo) }
            }
        }
    }
}

@Composable
fun EscaneoRoute(onVolver: () -> Unit, onTextoLeido: () -> Unit, viewModel: EscaneoViewModel = viewModel(factory = EscaneoViewModel.Factory)) {
    val contexto = LocalContext.current
    val ciclo = LocalLifecycleOwner.current
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    var permiso by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(contexto, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permiso = it }

    val camara = remember {
        LifecycleCameraController(contexto).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            imageCaptureMode = ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
        }
    }
    DisposableEffect(permiso) {
        if (permiso) camara.bindToLifecycle(ciclo)
        onDispose { camara.unbind() }
    }
    camara.enableTorch(estado.linterna)

    EscaneoScreen(
        estado = estado,
        permisoConcedido = permiso,
        onPedirPermiso = { pedirPermiso.launch(Manifest.permission.CAMERA) },
        onVolver = onVolver,
        onLinterna = viewModel::alternarLinterna,
        onCapturar = {
            viewModel.alDisparar()
            camara.takePicture(
                ContextCompat.getMainExecutor(contexto),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) = viewModel.alCapturar(image, onTextoLeido)
                    override fun onError(exception: ImageCaptureException) = viewModel.alFallarCaptura()
                },
            )
        },
        vistaPrevia = {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        // TextureView: se compone dentro de Compose. Con SurfaceView (por defecto) el fondo de la pantalla la tapa.
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        controller = camara
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        },
    )
}

@Composable
fun EscaneoScreen(
    estado: EscaneoUiState,
    permisoConcedido: Boolean,
    onPedirPermiso: () -> Unit,
    onVolver: () -> Unit,
    onLinterna: () -> Unit,
    onCapturar: () -> Unit,
    vistaPrevia: @Composable () -> Unit,
) {
    if (!permisoConcedido) {
        Column(Modifier.fillMaxSize().background(Linen).safeDrawingPadding()) {
            IconButton(onClick = onVolver, modifier = Modifier.padding(4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EstadoVacio(
                    icono = Icons.Outlined.NoPhotography,
                    titulo = "Yachay necesita la cámara",
                    texto = "La usamos solo para leer las páginas de tu libro. Las fotos no salen de la tableta.",
                    accion = "Permitir cámara",
                    onAccion = onPedirPermiso,
                )
            }
        }
        return
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        vistaPrevia()
        MarcoGuia()

        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onVolver) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                }
                Text("Escanear página", style = MaterialTheme.typography.titleLarge, color = Color.White)
            }
            Spacer(Modifier.weight(1f))
            Text(
                estado.aviso ?: "Coloca la página dentro del marco, con buena luz",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 24.dp)
                    .background(Color.Black.copy(alpha = if (estado.aviso != null) 0.75f else 0.4f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
            Row(
                Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(onClick = onLinterna, shape = CircleShape, color = Color.Black.copy(alpha = 0.45f), modifier = Modifier.size(56.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (estado.linterna) Icons.Outlined.FlashlightOn else Icons.Outlined.FlashlightOff,
                            contentDescription = if (estado.linterna) "Apagar linterna" else "Encender linterna",
                            tint = Color.White,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Surface(
                    onClick = onCapturar,
                    enabled = !estado.ocupado,
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(4.dp, Color.White.copy(alpha = 0.5f)),
                    modifier = Modifier.size(84.dp).semantics { contentDescription = "Tomar foto" },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(Modifier.size(64.dp).background(Color.White, CircleShape))
                    }
                }
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(56.dp))
            }
        }

        if (estado.etapa != null) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(Modifier.height(16.dp))
                    Text(estado.etapa.mensaje, style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
            }
        }
    }
}

/** Oscurece el exterior y dibuja el marco donde debe quedar la página (proporción 3:4). */
@Composable
private fun MarcoGuia() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
        ) {
            val ancho = minOf(size.width * 0.84f, size.height * 0.62f * 3f / 4f)
            val alto = ancho * 4f / 3f
            val origen = Offset((size.width - ancho) / 2f, (size.height - alto) / 2f - size.height * 0.03f)
            val radio = CornerRadius(24.dp.toPx())
            drawRect(Color.Black.copy(alpha = 0.45f))
            drawRoundRect(Color.Transparent, origen, Size(ancho, alto), radio, blendMode = BlendMode.Clear)
            // Esquinas de escáner en lugar de un marco cerrado: indican «encuadra la página aquí».
            val trazo = 5.dp.toPx()
            val brazo = ancho * 0.16f
            val r = radio.x
            // Cada esquina: (punto de la esquina, dirección horizontal, dirección vertical hacia adentro).
            listOf(
                Triple(origen, 1f, 1f),
                Triple(Offset(origen.x + ancho, origen.y), -1f, 1f),
                Triple(Offset(origen.x + ancho, origen.y + alto), -1f, -1f),
                Triple(Offset(origen.x, origen.y + alto), 1f, -1f),
            ).forEach { (c, sx, sy) ->
                val esquina = Path().apply {
                    moveTo(c.x, c.y + sy * brazo)
                    lineTo(c.x, c.y + sy * r)
                    quadraticTo(c.x, c.y, c.x + sx * r, c.y)
                    lineTo(c.x + sx * brazo, c.y)
                }
                drawPath(esquina, Color.White, style = Stroke(trazo, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

