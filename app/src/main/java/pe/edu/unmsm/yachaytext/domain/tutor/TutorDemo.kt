package pe.edu.unmsm.yachaytext.domain.tutor

import java.text.Normalizer
import kotlinx.coroutines.delay
import pe.edu.unmsm.yachaytext.data.local.RolMensaje
import pe.edu.unmsm.yachaytext.domain.rag.FragmentoRecuperado

/**
 * Tutor sin modelo de lenguaje, para demostrar la interfaz y el RAG mientras se
 * integra LiteRT-LM (Sprint 3).
 *
 * - Con contexto del libro: cita la oración de la página que más se relaciona
 *   con la pregunta y pide al estudiante que la interprete, sin darle la respuesta.
 * - Sin contexto: reproduce el diálogo del prototipo sobre la fotosíntesis o
 *   responde con una pregunta genérica.
 */
class TutorDemo(private val demora: Long = 900) : Tutor {

    override fun saludo(tema: Tema) = when (tema) {
        is Tema.Libro -> "Hola. Vamos a estudiar «${tema.titulo}». ¿Qué parte de la página te gustaría entender mejor? Escribe tu pregunta."
        Tema.General -> "Hola. ¿Qué te gustaría entender hoy? Pregúntame sobre los libros que escaneaste."
        Tema.Ejemplo -> "Hola. Leíste que las plantas fabrican su propio alimento. ¿Qué crees que necesita una planta para lograrlo?"
    }

    override suspend fun responder(historial: List<Turno>, consulta: String, contexto: List<FragmentoRecuperado>, ayuda: NivelAyuda): String {
        delay(demora)
        val turnosEstudiante = historial.count { it.rol == RolMensaje.ESTUDIANTE }
        val fuente = contexto.firstOrNull()
        return if (fuente != null) conContexto(consulta, fuente, turnosEstudiante, ayuda) else sinContexto(consulta, turnosEstudiante)
    }

    private fun conContexto(consulta: String, fuente: FragmentoRecuperado, turno: Int, ayuda: NivelAyuda): String {
        val cita = oracionMasRelacionada(consulta, fuente.texto)
        if (ayuda == NivelAyuda.Menos) {
            return "Tu libro tiene la respuesta en la página ${fuente.nroPagina}. Busca la oración que habla de esto. ¿Qué dice?"
        }
        if (ayuda == NivelAyuda.Mas) {
            val clave = palabraClave(consulta, cita)
            val pista = clave?.let { " Fíjate en la palabra «$it»." }.orEmpty()
            return "En la página ${fuente.nroPagina} dice: «$cita».$pista ¿Qué te dice esa oración sobre tu pregunta?"
        }
        return when (turno % 3) {
            0 -> "Busquemos la respuesta en tu libro. En la página ${fuente.nroPagina} dice: «$cita». ¿Qué parte de esa oración responde tu pregunta? Explícalo con tus palabras."
            1 -> "Mira lo que dice la página ${fuente.nroPagina}: «$cita». Con esa pista, ¿cómo responderías tu pregunta?"
            else -> "Vuelve a leer esta parte de la página ${fuente.nroPagina}: «$cita». ¿Qué palabra de ahí te parece la más importante y por qué?"
        }
    }

    private fun sinContexto(consulta: String, turnosEstudiante: Int): String {
        val texto = consulta.lowercase()
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

    /** La oración del fragmento que comparte más palabras (de 4 letras o más) con la pregunta. */
    internal fun oracionMasRelacionada(consulta: String, fragmento: String): String {
        val claves = palabras(consulta)
        val oraciones = fragmento.split(Regex("(?<=[.!?…])\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        val mejor = oraciones.maxByOrNull { o -> palabras(o).count { p -> claves.any { c -> p.take(5) == c.take(5) } } }
            ?: fragmento
        return mejor.trimEnd('.', ' ')
    }

    /** Palabra de la cita que más se parece a la pregunta, tal como aparece en la cita. */
    private fun palabraClave(consulta: String, cita: String): String? {
        val claves = palabras(consulta)
        return cita.split(Regex("[^\\p{L}0-9]+")).filter { it.length >= 4 }
            .firstOrNull { p -> palabras(p).any { n -> claves.any { c -> n.take(5) == c.take(5) } } }
    }

    private fun palabras(texto: String): Set<String> =
        Normalizer.normalize(texto.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
            .split(Regex("[^a-z0-9]+")).filter { it.length >= 4 }.toSet()
}
