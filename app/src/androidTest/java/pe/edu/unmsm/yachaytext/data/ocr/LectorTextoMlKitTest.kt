package pe.edu.unmsm.yachaytext.data.ocr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import pe.edu.unmsm.yachaytext.domain.ingesta.Fragmentador

/**
 * Corre en un dispositivo o emulador con los datos móviles y el Wi-Fi apagados:
 * verifica que el modelo de OCR incluido en el APK lee texto en castellano sin red.
 */
@RunWith(AndroidJUnit4::class)
class LectorTextoMlKitTest {

    private val lineas = listOf(
        "La fotosíntesis",
        "",
        "Las plantas fabrican su propio alimento",
        "mediante la fotosíntesis. Para ello nece-",
        "sitan luz del sol, agua y dióxido de carbono.",
    )

    private fun pagina(): Bitmap {
        val bitmap = Bitmap.createBitmap(1240, 760, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap).apply { drawColor(Color.WHITE) }
        val pincel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 46f; typeface = Typeface.SERIF }
        lineas.forEachIndexed { i, linea -> canvas.drawText(linea, 60f, 110f + i * 80f, pincel) }
        return bitmap
    }

    @Test
    fun leeTextoEnCastellanoSinConexion() = runTest {
        val texto = LectorTextoMlKit().leer(pagina())
        val limpio = Fragmentador().limpiar(texto).lowercase()

        assertTrue("OCR: $texto", "fotos" in limpio)
        assertTrue("OCR: $texto", "plantas fabrican su propio alimento" in limpio)
        // El guion de fin de línea se une: «nece-sitan» → «necesitan».
        assertTrue("OCR: $texto", "necesitan" in limpio)
    }
}
