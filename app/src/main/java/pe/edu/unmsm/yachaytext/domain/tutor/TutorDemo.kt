package pe.edu.unmsm.yachaytext.domain.tutor

import kotlinx.coroutines.delay
import pe.edu.unmsm.yachaytext.data.local.RolMensaje

/**
 * Tutor con guion fijo para demostrar la interfaz mientras se integra el
 * modelo de lenguaje (Sprint 3). Reproduce el diálogo socrático del prototipo
 * sobre la fotosíntesis y, fuera de él, responde con preguntas genéricas.
 */
class TutorDemo(private val demora: Long = 900) : Tutor {

    override fun saludo(tema: String) =
        "Hola. Leíste que las plantas fabrican su propio alimento. ¿Qué crees que necesita una planta para lograrlo?"

    override suspend fun responder(historial: List<Turno>, consulta: String): String {
        delay(demora)
        val texto = consulta.lowercase()
        val turnosEstudiante = historial.count { it.rol == RolMensaje.ESTUDIANTE }
        return when {
            "no entiendo" in texto || "no sé" in texto ->
                "Piensa en lo que hace tu papá o tu mamá después de sembrar. ¿Qué le echan a las plantas para que no se sequen?"
            "agua" in texto ->
                "¡Eso es! Las raíces absorben agua. Ya tienes dos elementos: luz del sol y agua. Falta uno que viene del aire. ¿Cuál crees que es?"
            "sol" in texto ->
                "Bien. Además de la luz, las plantas toman algo de la tierra y algo del aire. ¿Qué crees que absorben por las raíces?"
            "luz" in texto ->
                "La luz es una pieza importante. ¿Y de dónde viene esa luz en tu chacra?"
            turnosEstudiante > 4 ->
                "Vas muy bien. Explícame con tus palabras: ¿qué necesita una planta para fabricar su alimento?"
            else ->
                "Buena idea. ¿Qué parte del texto te hizo pensar eso? Cuéntamelo con tus palabras."
        }
    }
}
