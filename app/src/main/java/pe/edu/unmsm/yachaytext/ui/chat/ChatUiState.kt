package pe.edu.unmsm.yachaytext.ui.chat

import pe.edu.unmsm.yachaytext.data.Pagina
import pe.edu.unmsm.yachaytext.data.local.RolMensaje
import pe.edu.unmsm.yachaytext.domain.glosario.Coincidencia
import pe.edu.unmsm.yachaytext.domain.glosario.TerminoQuechua
import pe.edu.unmsm.yachaytext.domain.glosario.VarianteQuechua

data class MensajeUi(
    val id: Long,
    val rol: RolMensaje,
    val texto: String,
    val porVoz: Boolean = false,
    /** Términos del glosario presentes en el texto, para resaltarlos (RF05). */
    val coincidencias: List<Coincidencia> = emptyList(),
    /** Libro y página usados como contexto por el RAG, si los hubo. */
    val fuente: String? = null,
    val fuenteLibroId: Long? = null,
    val fuentePaginas: List<Int> = emptyList(),
) {
    val terminos: List<TerminoQuechua> get() = coincidencias.map { it.termino }.distinct()
}

/** Páginas citadas que el estudiante abrió para leerlas. */
data class FuenteAbierta(val titulo: String, val paginas: List<Pagina>)

/** Atajos del chat (Tabla 3.9 del documento de avance). */
enum class Atajo(val etiqueta: String, val consulta: String) {
    Explicame("Explícame con preguntas", "Explícame este tema con preguntas"),
    Quiz("Ponme a prueba", "Hazme preguntas para ponerme a prueba sobre este tema"),
    Palabras("Palabras difíciles", "¿Qué palabras difíciles hay en este texto?"),
}

data class ChatUiState(
    val titulo: String = "",
    val subtitulo: String? = null,
    val mensajes: List<MensajeUi> = emptyList(),
    val entrada: String = "",
    val pensando: Boolean = false,
    val grabando: Boolean = false,
    val leyendoId: Long? = null,
    val variante: VarianteQuechua = VarianteQuechua.COLLAO,
    val fuenteAbierta: FuenteAbierta? = null,
) {
    /** Conversación nueva: el teclado se abre solo para escribir la primera pregunta. */
    val sinPreguntas: Boolean get() = mensajes.none { it.rol == RolMensaje.ESTUDIANTE }

    val puedeEnviar: Boolean get() = entrada.isNotBlank() && !pensando
}
