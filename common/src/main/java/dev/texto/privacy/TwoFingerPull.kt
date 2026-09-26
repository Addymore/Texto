package dev.texto.privacy

/** Both original pointers must travel downward together; a stationary second finger cannot arm it. */
class TwoFingerPull {
    private var first = -1
    private var second = -1
    private var firstY = 0f
    private var secondY = 0f
    var armed = false
        private set
    fun reset() { first = -1; second = -1; armed = false }
    fun begin(a: Int, y: Float, b: Int, z: Float) {
        reset(); first = a; firstY = y; second = b; secondY = z
    }
    fun move(a: Int, y: Float, b: Int, z: Float, threshold: Float) {
        armed = when {
            a == first && b == second -> y - firstY >= threshold && z - secondY >= threshold
            b == first && a == second -> z - firstY >= threshold && y - secondY >= threshold
            else -> false
        }
    }
}
