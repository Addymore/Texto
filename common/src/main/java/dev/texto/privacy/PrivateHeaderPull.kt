package dev.texto.privacy

/** A deliberate vertical pull; extra pointers or sideways motion cancel it. */
class PrivateHeaderPull {
    var active = false
        private set
    private var x = 0f
    private var y = 0f
    var armed = false
        private set
    fun begin(startX: Float, startY: Float) { reset(); active = true; x = startX; y = startY }
    fun move(currentX: Float, currentY: Float, threshold: Float) {
        if (!active) return
        val dy = currentY - y
        if (kotlin.math.abs(currentX - x) > threshold / 2 || dy < -threshold / 2) { reset(); return }
        armed = dy >= threshold
    }
    fun reset() { active = false; armed = false }
}
