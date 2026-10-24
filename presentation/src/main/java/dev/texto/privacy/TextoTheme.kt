package dev.texto.privacy

import android.content.Context
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import dev.octoshrimpy.quik.R

object TextoTheme {
    fun onColor(color: Int): Int = if (ColorUtils.calculateLuminance(color) > .179) Color.BLACK else Color.WHITE
    fun apply(context: Context, accent: Int = TextoAppearance.accent(context)) {
        if (context is android.app.Activity && TextoAppearance.prefs(context).getBoolean("dynamic_colors", false)) {
            com.google.android.material.color.DynamicColors.applyToActivityIfAvailable(context)
        }
        val dark = context.resources.configuration.uiMode and 0x30 == 0x20
        val black = dark && android.preference.PreferenceManager.getDefaultSharedPreferences(context).getBoolean("black", false)
        if (black) {
            context.theme.applyStyle(R.style.AppTheme_Black, true)
        }
        val container = ColorUtils.blendARGB(if (black) Color.BLACK else if (dark) 0xFF23262D.toInt() else 0xFFF3F5F9.toInt(), accent, .18f)
        com.google.android.material.color.TextoColorResources.apply(context, mapOf(
            R.color.texto_control_accent to accent,
            R.color.texto_control_on_accent to onColor(accent),
            R.color.texto_control_container to container,
            R.color.texto_control_on_container to onColor(container)
        ))
        context.theme.applyStyle(R.style.TextoPalette, true)
        if (context is android.app.Activity) {
            val a = context.obtainStyledAttributes(intArrayOf(android.R.attr.windowBackground))
            val windowColor = if (black) Color.BLACK else a.getColor(0, if (dark) 0xFF192025.toInt() else Color.WHITE)
            a.recycle()
            context.window.statusBarColor = windowColor
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.window.navigationBarColor = windowColor
            }
            androidx.core.view.WindowCompat.getInsetsController(context.window, context.window.decorView).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
            TextoAppearance.refreshRate(context)
        }
    }
    fun appearanceKey(context: Context): String {
        val keys = setOf("skin_preset", "oem_layout", "card_shape", "card_finish", "density", "dynamic_colors", "list_avatars", "preview_lines", "message_counts", "unread_style", "bubbles", "card_size", "inbox_title", "inbox_motto", "background_image", "header_image", "bottom_bar_visible", "bottom_bar_items", "high_refresh", "reduce_motion", "dial_country_code")
        val black = android.preference.PreferenceManager.getDefaultSharedPreferences(context).getBoolean("black", false)
        return "${TextoAppearance.accent(context)}:$black:" + TextoAppearance.prefs(context).all.filterKeys { it in keys || it in setOf("sent_color","received_color","message_font","message_line_height","border_width","gradient_direction") }.toSortedMap().toString()
    }
}
