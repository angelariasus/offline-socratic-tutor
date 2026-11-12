package pe.edu.unmsm.yachaytext.domain.ingesta

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Texto reconocido que pasa de la pantalla de escaneo a la de revisión. Solo vive en memoria. */
class BorradorEscaneo {
    private val _texto = MutableStateFlow<String?>(null)
    val texto: StateFlow<String?> = _texto.asStateFlow()

    /** Libro desde el que se empezó a escanear: la revisión lo propone primero. */
    var libroPreferido: Long? = null

    fun guardar(texto: String) {
        _texto.value = texto
    }

    fun limpiar() {
        _texto.value = null
    }
}

/** Áreas curriculares de la Educación Básica Regular (MINEDU) que se ofrecen al crear un libro. */
val AREAS_CURRICULARES = listOf(
    "Comunicación",
    "Matemática",
    "Ciencia y Tecnología",
    "Personal Social",
    "Ciencias Sociales",
    "Desarrollo Personal, Ciudadanía y Cívica",
    "Arte y Cultura",
    "Educación Religiosa",
    "Educación para el Trabajo",
)
