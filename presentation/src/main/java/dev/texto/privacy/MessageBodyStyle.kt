package dev.texto.privacy

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import dev.octoshrimpy.quik.R

/** Keep auto-linked phone numbers, emails and URLs readable on the actual bubble. */
object MessageBodyStyle {
    fun apply(body: TextView, outgoing: Boolean, accent: Int, emojiOnly: Boolean) {
        val attributes = body.context.obtainStyledAttributes(intArrayOf(
            R.attr.bubbleColor, android.R.attr.windowBackground
        ))
        val surface = attributes.getColor(1, Color.WHITE)
        val bubble = if (outgoing) attributes.getColor(0, surface) else accent
        attributes.recycle()
        val transparent = emojiOnly || TextoAppearance.noCards(body.context)
        val background = ColorUtils.compositeColors(if (transparent) Color.TRANSPARENT else bubble, surface)
        val foreground = if (ColorUtils.calculateContrast(Color.WHITE, background) >=
            ColorUtils.calculateContrast(Color.BLACK, background)) Color.WHITE else Color.BLACK

        body.backgroundTintList = ColorStateList.valueOf(if (transparent) Color.TRANSPARENT else bubble)
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
}
