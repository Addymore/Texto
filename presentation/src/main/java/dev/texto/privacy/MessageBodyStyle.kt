package dev.texto.privacy

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import dev.octoshrimpy.quik.R

/** Keep auto-linked phone numbers, emails and URLs readable on the actual bubble. */
object MessageBodyStyle {
    private data class Surface(val key: List<Any?>, val background: android.graphics.drawable.Drawable, val foreground: Int)
    fun apply(body: TextView, outgoing: Boolean, accent: Int, emojiOnly: Boolean) {
        val foreground = surface(body, accent, emojiOnly)
        val p = TextoAppearance.prefs(body.context)
        val scale = when (p.getString("card_size", "medium")) { "small" -> .85f; "large" -> 1.15f; else -> 1f }
        val spacing = when (p.getString("density", "comfortable")) { "compact" -> 5; "airy" -> 9; else -> 7 }
        val d = body.resources.displayMetrics.density
        body.setPadding(((spacing+4)*d*scale).toInt(), (spacing*d*scale).toInt(), ((spacing+4)*d*scale).toInt(), (spacing*d*scale).toInt())
        body.setTextColor(foreground)
        // Explicitly set links too: XML ColorStateLists do not call QkTextView's Int overload.
        body.setLinkTextColor(foreground)
        body.highlightColor = ColorUtils.setAlphaComponent(foreground, 48)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            body.textSelectHandle?.setTint(foreground)
            body.textSelectHandleLeft?.setTint(foreground)
            body.textSelectHandleRight?.setTint(foreground)
        }
    }
    /** Shared card renderer for text, file and contact attachments; return a readable foreground. */
    fun surface(view: android.view.View, accent: Int, transparent: Boolean = false): Int {
        val attrs = view.context.obtainStyledAttributes(intArrayOf(android.R.attr.windowBackground))
        val window = attrs.getColor(0, Color.WHITE); attrs.recycle()
        val p = TextoAppearance.prefs(view.context)
        val old = view.background as? android.graphics.drawable.GradientDrawable
        val radius = when (p.getString("card_shape", "soft")) { "square" -> 0; "pill" -> 48; "minimal" -> 12; "round" -> 30; else -> 16 } * view.resources.displayMetrics.density
        val corners = if (p.getString("bubbles", "fluid") == "classic" && Build.VERSION.SDK_INT >= 24)
            old?.cornerRadii?.map { if (it <= 5 * view.resources.displayMetrics.density) it else radius }?.toFloatArray() else null
        val key=listOf(accent,transparent,window,view.resources.configuration.uiMode,radius,
            p.getString("card_finish","tonal"),corners?.toList())
        val cached=view.getTag(R.id.texto_message_surface) as? Surface
        if(cached?.key==key && view.background === cached.background) return cached.foreground
        view.backgroundTintList = null
        view.background = if (transparent) android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
            else TextoAppearance.card(view.context, accentColor = accent, corners = corners)
        val colors = (if (transparent) intArrayOf(window) else TextoAppearance.cardColors(view.context, accent).map { ColorUtils.compositeColors(it, window) }.toIntArray())
        val foreground=listOf(Color.BLACK, Color.WHITE).maxByOrNull { fg -> colors.minOf { ColorUtils.calculateContrast(fg, it) } }!!
        view.setTag(R.id.texto_message_surface,Surface(key,view.background,foreground))
        return foreground
    }

    fun backgroundColors(view: android.view.View, window: Int): IntArray {
        return TextoAppearance.cardColors(view.context).map { ColorUtils.compositeColors(it, window) }.toIntArray()
    }

}
