package pe.edu.unmsm.yachaytext.data.local

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/** Serializa embeddings (FloatArray) a BLOB y calcula la similitud de coseno. */
object EmbeddingCodec {

    fun encode(vector: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(vector.size * Float.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        buffer.asFloatBuffer().put(vector)
        return buffer.array()
    }

    fun decode(bytes: ByteArray): FloatArray {
        val floats = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
        return FloatArray(floats.remaining()).also { floats.get(it) }
    }

    /** Escala el vector a norma 1, para que el coseno se reduzca a un producto punto. */
    fun normalize(vector: FloatArray): FloatArray {
        var sum = 0f
        for (v in vector) sum += v * v
        val norm = sqrt(sum)
        if (norm == 0f) return vector.copyOf()
        return FloatArray(vector.size) { vector[it] / norm }
    }

    /** sim(u, v) = u · v cuando ambos vectores están normalizados. */
    fun dot(u: FloatArray, v: FloatArray): Float {
        require(u.size == v.size) { "Dimensiones distintas: ${u.size} vs ${v.size}" }
        var acc = 0f
        for (i in u.indices) acc += u[i] * v[i]
        return acc
    }
}
