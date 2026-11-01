package pe.edu.unmsm.yachaytext

import android.app.Application
import android.content.Context
import pe.edu.unmsm.yachaytext.data.ChatRepository
import pe.edu.unmsm.yachaytext.data.LibroRepository
import pe.edu.unmsm.yachaytext.data.PerfilRepository
import pe.edu.unmsm.yachaytext.data.local.YachayDatabase
import pe.edu.unmsm.yachaytext.data.voz.Narrador
import pe.edu.unmsm.yachaytext.data.voz.NarradorAndroid
import pe.edu.unmsm.yachaytext.domain.glosario.GlosarioQuechua
import pe.edu.unmsm.yachaytext.domain.tutor.IniciarConsulta
import pe.edu.unmsm.yachaytext.domain.tutor.Tutor
import pe.edu.unmsm.yachaytext.domain.tutor.TutorDemo

/**
 * Contenedor de dependencias manual. Se evita Hilt para no sumar memoria ni
 * tiempo de arranque en tabletas de 2 GB de RAM (RNF02).
 */
class AppContainer(context: Context) {
    val database: YachayDatabase by lazy { YachayDatabase.build(context) }
    val chatRepository: ChatRepository by lazy { ChatRepository(database) }
    val libroRepository: LibroRepository by lazy { LibroRepository(database) }
    val perfilRepository: PerfilRepository by lazy { PerfilRepository(context) }
    val glosario: GlosarioQuechua by lazy { GlosarioQuechua() }
    val narrador: Narrador by lazy { NarradorAndroid(context) }

    // Temporal: se reemplaza por el tutor RAG + LiteRT-LM en el Sprint 3.
    val tutor: Tutor by lazy { TutorDemo() }
    val iniciarConsulta: IniciarConsulta by lazy { IniciarConsulta(chatRepository, tutor) }
}

class YachayApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
