package pe.edu.unmsm.yachaytext.data.ocr

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/** Extrae el texto de una foto sin conexión (RF01). */
interface LectorTexto {
    suspend fun leer(imagen: ImageProxy): String
}

/** ML Kit Text Recognition v2 (alfabeto latino) con el modelo incluido en el APK. */
class LectorTextoMlKit : LectorTexto {
    private val reconocedor by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    /** Se convierte a Bitmap para no depender del formato (JPEG o YUV) que entregue cada cámara. */
    override suspend fun leer(imagen: ImageProxy): String {
        val (bitmap, rotacion) = imagen.use { it.toBitmap() to it.imageInfo.rotationDegrees }
        return leer(bitmap, rotacion)
    }

    /** Para imágenes que no vienen de la cámara (pruebas o una futura importación desde la galería). */
    suspend fun leer(imagen: Bitmap, rotacion: Int = 0): String = procesar(InputImage.fromBitmap(imagen, rotacion))

    private suspend fun procesar(entrada: InputImage): String = suspendCancellableCoroutine { cont ->
        reconocedor.process(entrada)
            .addOnSuccessListener { cont.resume(aTexto(it)) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    /** Cada bloque de ML Kit es un párrafo; sus líneas se unen con saltos simples. */
    private fun aTexto(resultado: Text): String =
        resultado.textBlocks.joinToString("\n\n") { bloque -> bloque.lines.joinToString("\n") { it.text } }
}
