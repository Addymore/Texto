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
    private data class ClassicMask(val key: List<Any?>, val corners: FloatArray?)
    fun apply(body: TextView, outgoing: Boolean, accent: Int, emojiOnly: Boolean, classicBubble: Int? = null) {
        val foreground = surface(body, accent, emojiOnly, outgoing, classicBubble)
        val p = TextoAppearance.prefs(body.context)
        val scale = when (p.getString("card_size", "medium")) { "small" -> .85f; "large" -> 1.15f; else -> 1f }
        val spacing = when (p.getString("density", "comfortable")) { "compact" -> 5; "airy" -> 9; else -> 7 }
        val d = body.resources.displayMetrics.density
        val horizontal=((spacing+4)*d*scale).toInt(); val vertical=(spacing*d*scale).toInt()
        if(body.paddingLeft!=horizontal || body.paddingTop!=vertical || body.paddingRight!=horizontal || body.paddingBottom!=vertical)
            body.setPadding(horizontal,vertical,horizontal,vertical)
        val face=when(p.getString("message_font","theme")) { "serif" -> android.graphics.Typeface.SERIF; "mono" -> android.graphics.Typeface.MONOSPACE; "sans" -> android.graphics.Typeface.SANS_SERIF; else -> if(SkinPresets.id(body.context)=="nothing") android.graphics.Typeface.MONOSPACE else android.graphics.Typeface.DEFAULT }
        if(body.typeface!=face) body.typeface=face
        val lineHeight=when(p.getString("message_line_height","normal")) { "relaxed" -> 1.15f; "spacious" -> 1.3f; else -> 1f }
        if(body.lineSpacingMultiplier!=lineHeight) body.setLineSpacing(0f,lineHeight)
        if(body.currentTextColor!=foreground) body.setTextColor(foreground)
        // Explicitly set links too: XML ColorStateLists do not call QkTextView's Int overload.
        if(body.linkTextColors.defaultColor!=foreground) body.setLinkTextColor(foreground)
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
    fun surface(view: android.view.View, accent: Int, transparent: Boolean = false, outgoing: Boolean? = null, classicBubble: Int? = null): Int {
        val attrs = view.context.obtainStyledAttributes(intArrayOf(android.R.attr.windowBackground))
        val window = attrs.getColor(0, Color.WHITE); attrs.recycle()
        val p = TextoAppearance.prefs(view.context)
        val maskKey=listOf(classicBubble,view.resources.displayMetrics.density,view.resources.configuration.uiMode)
        var mask=view.getTag(R.id.texto_classic_mask) as? ClassicMask
        if(mask?.key!=maskKey) {
            val drawable=classicBubble?.let { androidx.appcompat.content.res.AppCompatResources.getDrawable(view.context,it) } as? android.graphics.drawable.GradientDrawable
            mask=ClassicMask(maskKey,if(Build.VERSION.SDK_INT>=24) drawable?.cornerRadii else null)
            view.setTag(R.id.texto_classic_mask,mask)
        }
        val oldCorners=mask?.corners ?: if(Build.VERSION.SDK_INT>=24) (view.background as? android.graphics.drawable.GradientDrawable)?.cornerRadii else null
        val profile=SkinPresets.id(view.context)
        val effectiveAccent=if(profile.isNotEmpty()) TextoAppearance.accent(view.context) else accent
        val radius = when (p.getString("card_shape", "soft")) { "square" -> 0; "pill" -> 48; "minimal" -> 12; "round" -> 30; else -> 16 } * view.resources.displayMetrics.density
        val corners = if (p.getString("bubbles", "fluid") == "classic" && Build.VERSION.SDK_INT >= 24)
            oldCorners?.map { if (it <= 5 * view.resources.displayMetrics.density) it else radius }?.toFloatArray() else null
        val key=listOf(profile,outgoing,TextoAppearance.accent(view.context),accent,transparent,window,view.resources.configuration.uiMode,radius,
            p.getString("card_finish","tonal"),p.getString("gradient_direction","diagonal"),p.getString("border_width","auto"),if(p.contains("sent_color")) p.getInt("sent_color",0) else null,if(p.contains("received_color")) p.getInt("received_color",0) else null,corners?.toList())
        val cached=view.getTag(R.id.texto_message_surface) as? Surface
        if(cached?.key==key && view.background === cached.background) return cached.foreground
        view.backgroundTintList = null
        (view as? androidx.appcompat.widget.AppCompatTextView)?.supportBackgroundTintList = null
        val colorKey=if(outgoing==true) "sent_color" else "received_color"
        val customColor=if(outgoing!=null && p.contains(colorKey) && !TextoAppearance.noCards(view.context)) p.getInt(colorKey,accent) or 0xFF000000.toInt() else null
        // One surface renderer for inbox cards, SMS and MMS. OEM presets supply preferences,
        // never a separate hard-coded grey bubble that discards the selected finish.
        view.background = if (transparent) android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
            else TextoAppearance.card(view.context, accentColor = effectiveAccent, corners = corners, fillColor = customColor)
        val colors = if (transparent) intArrayOf(window) else TextoAppearance.cardColors(view.context, effectiveAccent, customColor).map { ColorUtils.compositeColors(it, window) }.toIntArray()
        val foreground=listOf(Color.BLACK, Color.WHITE).maxByOrNull { fg -> colors.minOf { ColorUtils.calculateContrast(fg, it) } }!!
        selectionOverlay(view,foreground,corners)
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
