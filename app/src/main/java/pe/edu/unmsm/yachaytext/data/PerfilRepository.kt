package pe.edu.unmsm.yachaytext.data

import android.content.Context
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Perfil local del estudiante (sin cuentas ni servidor). Se completa en la pantalla de primer uso. */
data class Perfil(val nombre: String? = null, val grado: String? = null)

class PerfilRepository(context: Context) {
    private val prefs = context.getSharedPreferences("perfil", Context.MODE_PRIVATE)

    private fun leer() = Perfil(prefs.getString(NOMBRE, null), prefs.getString(GRADO, null))

    fun observar(): Flow<Perfil> = callbackFlow {
        val escucha = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(leer()) }
        trySend(leer())
        prefs.registerOnSharedPreferenceChangeListener(escucha)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(escucha) }
    }

    fun guardar(perfil: Perfil) {
        prefs.edit().putString(NOMBRE, perfil.nombre).putString(GRADO, perfil.grado).apply()
    }

    private companion object {
        const val NOMBRE = "nombre"
        const val GRADO = "grado"
    }
}
