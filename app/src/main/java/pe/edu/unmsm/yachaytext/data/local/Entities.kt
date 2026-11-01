package pe.edu.unmsm.yachaytext.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// Entidades del modelo entidad-relación (Figura 3.5 del 2.º entregable).
// Diferencia con el documento: las fechas se guardan como epoch en milisegundos
// (INTEGER) en lugar de TEXT, para ordenar y filtrar sin parsear cadenas.

@Entity(tableName = "libro")
data class LibroEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id_libro") val id: Long = 0,
    val titulo: String,
    @ColumnInfo(name = "area_curso") val areaCurso: String,
    @ColumnInfo(name = "num_paginas") val numPaginas: Int = 0,
    @ColumnInfo(name = "fecha_ingesta") val fechaIngesta: Long,
)

@Entity(
    tableName = "fragmento",
    foreignKeys = [
        ForeignKey(
            entity = LibroEntity::class,
            parentColumns = ["id_libro"],
            childColumns = ["id_libro"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("id_libro")],
)
data class FragmentoEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id_fragmento") val id: Long = 0,
    @ColumnInfo(name = "id_libro") val libroId: Long,
    @ColumnInfo(name = "nro_pagina") val nroPagina: Int,
    val orden: Int,
    val texto: String,
    /** Vector normalizado (norma 1) serializado con [EmbeddingCodec]. */
    val embedding: ByteArray,
) {
    // ByteArray no implementa equals por contenido; se compara explícitamente.
    override fun equals(other: Any?): Boolean =
        other is FragmentoEntity && id == other.id && libroId == other.libroId &&
            nroPagina == other.nroPagina && orden == other.orden && texto == other.texto &&
            embedding.contentEquals(other.embedding)

    override fun hashCode(): Int = id.hashCode() * 31 + embedding.contentHashCode()
}

@Entity(
    tableName = "conversacion",
    foreignKeys = [
        ForeignKey(
            entity = LibroEntity::class,
            parentColumns = ["id_libro"],
            childColumns = ["id_libro"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("id_libro")],
)
data class ConversacionEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id_conversacion") val id: Long = 0,
    /** Opcional: una conversación puede no estar ligada a un libro (0..1). */
    @ColumnInfo(name = "id_libro") val libroId: Long? = null,
    val titulo: String,
    @ColumnInfo(name = "fecha_inicio") val fechaInicio: Long,
)

enum class RolMensaje { ESTUDIANTE, TUTOR }

@Entity(
    tableName = "mensaje",
    foreignKeys = [
        ForeignKey(
            entity = ConversacionEntity::class,
            parentColumns = ["id_conversacion"],
            childColumns = ["id_conversacion"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("id_conversacion")],
)
data class MensajeEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id_mensaje") val id: Long = 0,
    @ColumnInfo(name = "id_conversacion") val conversacionId: Long,
    val rol: RolMensaje,
    val contenido: String,
    /** true si la consulta se dictó por voz (RF04). */
    @ColumnInfo(name = "por_voz") val porVoz: Boolean = false,
    @ColumnInfo(name = "fecha_hora") val fechaHora: Long,
)

/** Fragmentos que el RAG recuperó para generar un mensaje del tutor, con su puntaje. */
@Entity(
    tableName = "contexto_recuperado",
    primaryKeys = ["id_mensaje", "id_fragmento"],
    foreignKeys = [
        ForeignKey(
            entity = MensajeEntity::class,
            parentColumns = ["id_mensaje"],
            childColumns = ["id_mensaje"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FragmentoEntity::class,
            parentColumns = ["id_fragmento"],
            childColumns = ["id_fragmento"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("id_fragmento")],
)
data class ContextoRecuperadoEntity(
    @ColumnInfo(name = "id_mensaje") val mensajeId: Long,
    @ColumnInfo(name = "id_fragmento") val fragmentoId: Long,
    @ColumnInfo(name = "puntaje_coseno") val puntajeCoseno: Float,
)
