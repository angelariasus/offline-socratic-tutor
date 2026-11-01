package pe.edu.unmsm.yachaytext.ui.chat

import pe.edu.unmsm.yachaytext.data.local.RolMensaje
import pe.edu.unmsm.yachaytext.domain.glosario.GlosarioQuechua

/** Estados de ejemplo para @Preview y capturas de pantalla. Diálogo tomado del prototipo. */
object ChatPreviewData {
    private val glosario = GlosarioQuechua()
    private const val FUENTE = "Ciencia y Tecnología 5 · pág. 42"

    private fun tutor(id: Long, texto: String, fuente: String? = null) =
        MensajeUi(id, RolMensaje.TUTOR, texto, coincidencias = glosario.buscar(texto), fuente = fuente)

    private fun estudiante(id: Long, texto: String, porVoz: Boolean = false) =
        MensajeUi(id, RolMensaje.ESTUDIANTE, texto, porVoz = porVoz)

    private val mensajes = listOf(
        tutor(1, "Hola, Angel. Leíste que las plantas fabrican su propio alimento. ¿Qué crees que necesita una planta para lograrlo?", FUENTE),
        estudiante(2, "¿luz?"),
        tutor(3, "La luz es una pieza importante. ¿Y de dónde viene esa luz en tu chacra?"),
        estudiante(4, "del sol", porVoz = true),
        tutor(5, "Bien. Además de la luz, las plantas toman algo de la tierra y algo del aire. ¿Qué crees que absorben por las raíces?"),
        estudiante(6, "no entiendo"),
        tutor(7, "Piensa en lo que hace tu papá o tu mamá después de sembrar. ¿Qué le echan a las plantas para que no se sequen?"),
        estudiante(8, "agua"),
        tutor(9, "¡Eso es! Las raíces absorben agua. Ya tienes dos elementos: luz del sol y agua. Falta uno que viene del aire. ¿Cuál crees que es?", FUENTE),
    )

    val conversacion = ChatUiState(
        titulo = "La fotosíntesis",
        subtitulo = "Ciencia y Tecnología 5",
        mensajes = mensajes,
        leyendoId = 9,
    )

    val pensandoYEscribiendo = conversacion.copy(
        mensajes = mensajes.take(4),
        pensando = true,
        leyendoId = null,
    )

    val grabando = conversacion.copy(mensajes = mensajes.take(3), grabando = true, leyendoId = null)

    val conTexto = conversacion.copy(mensajes = mensajes.take(1), entrada = "¿La planta come tierra?", leyendoId = null)
}
