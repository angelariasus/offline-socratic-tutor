package pe.edu.unmsm.yachaytext.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun rolToString(rol: RolMensaje): String = rol.name

    @TypeConverter
    fun stringToRol(value: String): RolMensaje = RolMensaje.valueOf(value)
}

@Database(
    entities = [
        LibroEntity::class,
        FragmentoEntity::class,
        ConversacionEntity::class,
        MensajeEntity::class,
        ContextoRecuperadoEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class YachayDatabase : RoomDatabase() {
    abstract fun libroDao(): LibroDao
    abstract fun fragmentoDao(): FragmentoDao
    abstract fun conversacionDao(): ConversacionDao
    abstract fun mensajeDao(): MensajeDao
    abstract fun contextoRecuperadoDao(): ContextoRecuperadoDao

    companion object {
        fun build(context: Context): YachayDatabase =
            Room.databaseBuilder(context, YachayDatabase::class.java, "yachaytext.db").build()
    }
}
