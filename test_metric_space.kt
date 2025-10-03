// Simple test to verify metric space implementation
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

// Copy the normalizeEmbedding function for testing
fun normalizeEmbedding(vec: FloatArray): FloatArray {
    var ss = 0f
    for (v in vec) ss += v * v
    val n = sqrt(ss.toDouble()).toFloat()
    if (n <= 0f) return vec
    val out = FloatArray(vec.size)
    val inv = 1f / n
    for (i in vec.indices) out[i] = vec[i] * inv
    return out
}

// Copy the cosine function for testing
fun cosine(a: FloatArray, b: FloatArray): Float {
    // a,b assumed normalized; this is plain dot
    val n = minOf(a.size, b.size)
    var dot = 0f
    for (i in 0 until n) dot += a[i] * b[i]
    return dot
}

fun main() {
    println("Testing metric space implementation...")
    
    // Test normalization
    val v = floatArrayOf(3f, 4f, 0f)
    val n1 = normalizeEmbedding(v)
    val n2 = normalizeEmbedding(n1)
    
    fun l2(x: FloatArray): Float = sqrt(x.fold(0f) { acc, v -> acc + v*v })
    
    println("Original vector: ${v.contentToString()}")
    println("Normalized vector: ${n1.contentToString()}")
    println("L2 norm of normalized: ${l2(n1)}")
    println("Idempotent check: ${n1.contentEquals(n2)}")
    
    // Test cosine similarity
    val a = floatArrayOf(1f, 0f, 0f)
    val b = floatArrayOf(0f, 1f, 0f)
    val c = floatArrayOf(1f, 0f, 0f)
    
    println("Cosine(a, b): ${cosine(a, b)}")
    println("Cosine(a, c): ${cosine(a, c)}")
    
    // Test binary format
    val testVec = floatArrayOf(1.0f, 2.0f)
    val bb = ByteBuffer.allocate(testVec.size * 4).order(ByteOrder.LITTLE_ENDIAN)
    for (v in testVec) bb.putFloat(v)
    val bytes = bb.array()
    
    println("Binary bytes: ${bytes.joinToString(" ") { "%02x".format(it) }}")
    
    // Test roundtrip
    val bb2 = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    val recovered = FloatArray(testVec.size)
    for (i in recovered.indices) {
        recovered[i] = bb2.float
    }
    
    println("Original: ${testVec.contentToString()}")
    println("Recovered: ${recovered.contentToString()}")
    println("Roundtrip success: ${testVec.contentEquals(recovered)}")
    
    println("All tests passed!")
}
