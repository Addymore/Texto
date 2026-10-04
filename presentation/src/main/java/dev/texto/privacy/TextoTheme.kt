package dev.texto.privacy

import android.content.Context
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import dev.octoshrimpy.quik.R

object TextoTheme {
    fun onColor(color: Int): Int = if (ColorUtils.calculateLuminance(color) > .179) Color.BLACK else Color.WHITE
    fun apply(context: Context, accent: Int = TextoAppearance.accent(context)) {
        val dark = context.resources.configuration.uiMode and 0x30 == 0x20
        val container = ColorUtils.blendARGB(if (dark) 0xFF23262D.toInt() else 0xFFF3F5F9.toInt(), accent, .18f)
        com.google.android.material.color.TextoColorResources.apply(context, mapOf(
            R.color.texto_control_accent to accent,
            R.color.texto_control_on_accent to onColor(accent),
            R.color.texto_control_container to container,
            R.color.texto_control_on_container to onColor(container)
        ))
        context.theme.applyStyle(R.style.TextoPalette, true)
    }
    fun appearanceKey(context: Context): String {
        val keys = setOf("card_shape", "card_finish", "density", "dynamic_colors", "list_avatars", "preview_lines", "message_counts", "unread_style", "bubbles", "card_size", "inbox_title", "inbox_motto", "background_image", "header_image")
        return TextoAppearance.accent(context).toString() + TextoAppearance.prefs(context).all.filterKeys { it in keys }.toSortedMap().toString()
    }
}
