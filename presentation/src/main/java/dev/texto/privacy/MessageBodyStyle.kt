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
        val foreground = surface(body, accent, emojiOnly, outgoing)
        val p = TextoAppearance.prefs(body.context)
        val scale = when (p.getString("card_size", "medium")) { "small" -> .85f; "large" -> 1.15f; else -> 1f }
        val spacing = when (p.getString("density", "comfortable")) { "compact" -> 5; "airy" -> 9; else -> 7 }
        val d = body.resources.displayMetrics.density
        body.setPadding(((spacing+4)*d*scale).toInt(), (spacing*d*scale).toInt(), ((spacing+4)*d*scale).toInt(), (spacing*d*scale).toInt())
        body.typeface=android.graphics.Typeface.create(if(SkinPresets.id(body.context)=="nothing") "monospace" else "sans-serif",android.graphics.Typeface.NORMAL)
        body.setTextColor(foreground)
        // Explicitly set links too: XML ColorStateLists do not call QkTextView's Int overload.
        body.setLinkTextColor(foreground)
        val highlight=ColorUtils.setAlphaComponent(foreground,48)
        if(body is dev.octoshrimpy.quik.common.widget.TightTextView) {
            body.selectionHighlight=highlight
            body.highlightColor=Color.TRANSPARENT
        } else body.highlightColor=highlight
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            body.textSelectHandle?.setTint(foreground)
            body.textSelectHandleLeft?.setTint(foreground)
            body.textSelectHandleRight?.setTint(foreground)
        }
    }
    /** Shared card renderer for text, file and contact attachments; return a readable foreground. */
    fun surface(view: android.view.View, accent: Int, transparent: Boolean = false, outgoing: Boolean? = null): Int {
        val attrs = view.context.obtainStyledAttributes(intArrayOf(android.R.attr.windowBackground))
        val window = attrs.getColor(0, Color.WHITE); attrs.recycle()
        val p = TextoAppearance.prefs(view.context)
        val old = view.background as? android.graphics.drawable.GradientDrawable
        val profile=SkinPresets.id(view.context)
        val effectiveAccent=if(profile.isNotEmpty()) TextoAppearance.accent(view.context) else accent
        val radius = when (p.getString("card_shape", "soft")) { "square" -> 0; "pill" -> 48; "minimal" -> 12; "round" -> 30; else -> 16 } * view.resources.displayMetrics.density
        val corners = if (p.getString("bubbles", "fluid") == "classic" && Build.VERSION.SDK_INT >= 24)
            old?.cornerRadii?.map { if (it <= 5 * view.resources.displayMetrics.density) it else radius }?.toFloatArray() else null
        val key=listOf(profile,outgoing,TextoAppearance.accent(view.context),accent,transparent,window,view.resources.configuration.uiMode,radius,
            p.getString("card_finish","tonal"),corners?.toList())
        val cached=view.getTag(R.id.texto_message_surface) as? Surface
        if(cached?.key==key && view.background === cached.background) return cached.foreground
        view.backgroundTintList = null
        (view as? androidx.appcompat.widget.AppCompatTextView)?.supportBackgroundTintList = null
        val oemBubble=SkinPresets.usesProfileSurface(view.context) && outgoing != null && !TextoAppearance.noCards(view.context)
        val dark=view.resources.configuration.uiMode and 0x30 == 0x20
        val bubbleColor=if(outgoing==true) {
            if(profile=="nothing") (if(dark) 0xFFEEEEEE.toInt() else 0xFF252525.toInt()) else TextoAppearance.accent(view.context)
        } else if(dark) 0xFF2C2D31.toInt() else 0xFFECEEF2.toInt()
        val oemCorners=if(profile in listOf("xiaomi","samsung")) floatArrayOf(radius,radius,radius,radius,
            if(outgoing==true) radius/3 else radius,if(outgoing==true) radius/3 else radius,
            if(outgoing==false) radius/3 else radius,if(outgoing==false) radius/3 else radius) else null
        view.background = if(oemBubble && !transparent) android.graphics.drawable.GradientDrawable().apply {
            setColor(bubbleColor); cornerRadius=radius
            if(oemCorners != null) cornerRadii=oemCorners
        } else if (transparent) android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
            else TextoAppearance.card(view.context, accentColor = effectiveAccent, corners = if(profile.isNotEmpty()) oemCorners ?: corners else corners)
        val colors = (if (transparent) intArrayOf(window) else if(oemBubble) intArrayOf(bubbleColor) else TextoAppearance.cardColors(view.context, effectiveAccent).map { ColorUtils.compositeColors(it, window) }.toIntArray())
        val foreground=listOf(Color.BLACK, Color.WHITE).maxByOrNull { fg -> colors.minOf { ColorUtils.calculateContrast(fg, it) } }!!
        selectionOverlay(view,foreground,if(profile.isNotEmpty()) oemCorners ?: corners else corners)
        view.setTag(R.id.texto_message_surface,Surface(key,view.background,foreground))
        return foreground
    }

    /** Also used around MMS attachments, which no longer have a full-row rectangle. */
    fun selectionOverlay(view: android.view.View, foreground: Int, corners: FloatArray? = null) {
        val radius=when(TextoAppearance.prefs(view.context).getString("card_shape","soft")) { "square" -> 0; "pill" -> 48; "minimal" -> 12; "round" -> 30; else -> 16 } * view.resources.displayMetrics.density
        val selected=android.graphics.drawable.GradientDrawable().apply {
            cornerRadius=radius
            if(corners != null) cornerRadii=corners
            setColor(ColorUtils.setAlphaComponent(foreground,32))
            setStroke((2*view.resources.displayMetrics.density).toInt().coerceAtLeast(1),foreground)
        }
        view.foreground=android.graphics.drawable.StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_activated),selected)
            addState(intArrayOf(),android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        }
    }

    fun backgroundColors(view: android.view.View, window: Int): IntArray {
        return TextoAppearance.cardColors(view.context).map { ColorUtils.compositeColors(it, window) }.toIntArray()
    }

}
