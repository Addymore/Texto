package dev.texto.privacy

import android.graphics.*
import android.graphics.drawable.Drawable

/** Stable geometric portrait for a sender without a contact photo. */
class ModernAvatar(seed: String, color: Int) : Drawable() {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color=color }
    private val offset=((seed.hashCode() and 7)-3)*.012f
    override fun draw(canvas: Canvas) {
        val w=bounds.width().toFloat(); val h=bounds.height().toFloat()
        canvas.save(); canvas.translate(bounds.left.toFloat(),bounds.top.toFloat())
        canvas.drawCircle(w*(.5f+offset),h*.32f,w*.17f,paint)
        canvas.drawRoundRect(w*.16f,h*.55f,w*.84f,h*.98f,w*.27f,h*.27f,paint)
        canvas.restore()
    }
    override fun setAlpha(alpha: Int) { paint.alpha=alpha; invalidateSelf() }
    override fun setColorFilter(filter: ColorFilter?) { paint.colorFilter=filter; invalidateSelf() }
    @Deprecated("Drawable API") override fun getOpacity()=PixelFormat.TRANSLUCENT
}
