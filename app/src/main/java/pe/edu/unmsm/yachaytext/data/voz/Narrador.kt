package pe.edu.unmsm.yachaytext.data.voz

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Lee en voz alta las respuestas del tutor con el Text-to-Speech nativo (RF04). */
interface Narrador {
    /** 1 = velocidad normal; ver VelocidadVoz en Ajustes. */
    var velocidad: Float
    fun leer(id: Long, texto: String)
    fun detener()
}

/**
 * Requiere que la voz en castellano esté instalada en el dispositivo para
 * funcionar sin conexión. Los términos en quechua se leen con voz castellana.
 */
class NarradorAndroid(context: Context) : Narrador {
    private var listo = false
    override var velocidad: Float = 1f
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { estado ->
        listo = estado == TextToSpeech.SUCCESS
        if (listo) configurarIdioma()
    }

    private fun configurarIdioma() {
        val preferidos = listOf(Locale.forLanguageTag("es-PE"), Locale.forLanguageTag("es-419"), Locale.forLanguageTag("es"))
        preferidos.firstOrNull { tts.isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE }?.let { tts.language = it }
    }

    override fun leer(id: Long, texto: String) {
        if (!listo) return
        tts.setSpeechRate(velocidad)
        tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "mensaje-$id")
    }

    override fun detener() {
        tts.stop()
    }
}
