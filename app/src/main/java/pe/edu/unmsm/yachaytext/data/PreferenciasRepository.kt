package pe.edu.unmsm.yachaytext.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import pe.edu.unmsm.yachaytext.domain.glosario.VarianteQuechua
import pe.edu.unmsm.yachaytext.domain.tutor.NivelAyuda

enum class TamanoTexto(val etiqueta: String, val escala: Float) {
    Normal("Normal", 1f),
    Grande("Grande", 1.15f),
    MuyGrande("Muy grande", 1.3f),
}

enum class VelocidadVoz(val etiqueta: String, val factor: Float) {
    Lenta("Lenta", 0.75f),
    Normal("Normal", 1f),
    Rapida("Rápida", 1.25f),
}

/** Perfil local y preferencias del estudiante. Se guardan solo en el dispositivo (sin cuentas ni servidor). */
data class Preferencias(
    val nombre: String? = null,
    val grado: String? = null,
    val variante: VarianteQuechua = VarianteQuechua.COLLAO,
    val tamanoTexto: TamanoTexto = TamanoTexto.Normal,
    val velocidadVoz: VelocidadVoz = VelocidadVoz.Normal,
    val nivelAyuda: NivelAyuda = NivelAyuda.Normal,
    val primerUsoCompletado: Boolean = false,
)

class PreferenciasRepository(context: Context) {
    private val prefs = context.getSharedPreferences("preferencias", Context.MODE_PRIVATE)

    fun actual(): Preferencias = Preferencias(
        nombre = prefs.getString(NOMBRE, null),
        grado = prefs.getString(GRADO, null),
        variante = enumO(VARIANTE, VarianteQuechua.COLLAO),
        tamanoTexto = enumO(TAMANO, TamanoTexto.Normal),
        velocidadVoz = enumO(VELOCIDAD, VelocidadVoz.Normal),
        nivelAyuda = enumO(AYUDA, NivelAyuda.Normal),
        primerUsoCompletado = prefs.getBoolean(PRIMER_USO, false),
    )

    fun observar(): Flow<Preferencias> = callbackFlow {
        val escucha = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(actual()) }
        trySend(actual())
        prefs.registerOnSharedPreferenceChangeListener(escucha)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(escucha) }
    }.distinctUntilChanged()

    fun actualizar(cambio: (Preferencias) -> Preferencias) {
        val p = cambio(actual())
        prefs.edit()
            .putString(NOMBRE, p.nombre?.trim()?.takeIf { it.isNotEmpty() })
            .putString(GRADO, p.grado)
            .putString(VARIANTE, p.variante.name)
            .putString(TAMANO, p.tamanoTexto.name)
            .putString(VELOCIDAD, p.velocidadVoz.name)
            .putString(AYUDA, p.nivelAyuda.name)
            .putBoolean(PRIMER_USO, p.primerUsoCompletado)
            .apply()
    }

    private inline fun <reified E : Enum<E>> enumO(clave: String, defecto: E): E =
        prefs.getString(clave, null)?.let { valor -> enumValues<E>().firstOrNull { it.name == valor } } ?: defecto

    private companion object {
        const val NOMBRE = "nombre"
        const val GRADO = "grado"
        const val VARIANTE = "variante_quechua"
        const val TAMANO = "tamano_texto"
        const val VELOCIDAD = "velocidad_voz"
        const val AYUDA = "nivel_ayuda"
        const val PRIMER_USO = "primer_uso_completado"
    }
}

/** Grados de la Educación Básica Regular. */
val GRADOS = (1..6).map { "$it.º de primaria" } + (1..5).map { "$it.º de secundaria" }
