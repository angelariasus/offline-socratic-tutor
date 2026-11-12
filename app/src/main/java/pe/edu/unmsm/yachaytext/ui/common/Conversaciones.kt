package pe.edu.unmsm.yachaytext.ui.common

import pe.edu.unmsm.yachaytext.data.local.ConversacionEntity
import pe.edu.unmsm.yachaytext.data.local.LibroEntity

/** Conversación tal como se lista en Inicio y en Tutor, con el libro al que pertenece. */
data class ConversacionResumen(
    val id: Long,
    val titulo: String,
    val fechaInicio: Long,
    val libro: String? = null,
    val area: String? = null,
) {
    /** «Ciencia y Tecnología 5 · Hace 5 min», o solo la antigüedad si no hay libro. */
    fun detalle(ahora: Long): String = listOfNotNull(libro, tiempoRelativo(fechaInicio, ahora)).joinToString(" · ")
}

fun resumir(conversaciones: List<ConversacionEntity>, libros: List<LibroEntity>): List<ConversacionResumen> {
    val porId = libros.associateBy { it.id }
    return conversaciones.map { c ->
        val libro = c.libroId?.let(porId::get)
        ConversacionResumen(c.id, c.titulo, c.fechaInicio, libro?.titulo, libro?.areaCurso)
    }
}
