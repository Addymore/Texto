package dev.texto.testing

import android.app.Instrumentation
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import dev.octoshrimpy.quik.R
import dev.texto.privacy.MessageBodyStyle

/** Exercises the real incoming/outgoing message views, including Android auto-link spans. */
class MessageContrastInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val result = Bundle()
        try {
            waitForIdleSync()
            var failure: Throwable? = null
            var cases = 0
            val appearance = dev.texto.privacy.TextoAppearance.prefs(targetContext)
            val previousFinish = appearance.getString("card_finish", null)
            runOnMainSync {
                try {
                    for (finish in listOf("tonal","neutral","tinted","outlined","gradient","glass","amoled","none")) {
                    appearance.edit().putString("card_finish",finish).commit()
                    for (night in listOf(false, true)) for (black in listOf(false, true)) {
                        val config = Configuration(targetContext.resources.configuration).apply {
                            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                                if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
                        }
                        val context = ContextThemeWrapper(targetContext.createConfigurationContext(config),
                            if (black) R.style.AppTheme_Black else R.style.AppTheme)
                        for (accent in listOf(0xFF007E70.toInt(), 0xFFB73873.toInt())) {
                            dev.texto.privacy.TextoTheme.apply(context, accent)
                            fun resolved(attr: Int): Int {
                                val a = context.obtainStyledAttributes(intArrayOf(attr))
                                return a.getColor(0, 0).also { a.recycle() }
                            }
                            check(resolved(R.attr.colorPrimary) == accent) { "Primary did not follow custom accent" }
                            check(resolved(android.R.attr.colorAccent) == accent) { "Platform picker accent mismatch" }
                            val picker = android.app.DatePickerDialog(context)
                            val a = picker.context.obtainStyledAttributes(intArrayOf(android.R.attr.colorAccent))
                            check(a.getColor(0, 0) == accent) { "Scheduled date picker accent mismatch" }; a.recycle()
                            val settings = LayoutInflater.from(context).inflate(R.layout.settings_controller, null)
                            val button = settings.findViewById<TextView>(R.id.textoPrivacySettings)
                            dev.texto.privacy.TextoAppearance.styleSettingsCard(button)
                            check(button.background is android.graphics.drawable.RippleDrawable)
                            check((button as com.google.android.material.button.MaterialButton).supportBackgroundTintList == null) { "Settings support tint overrides card surface" }
                            check(button.backgroundTintList == null) { "Settings retains a fixed blue tint" }
                        }
                        for (outgoing in listOf(false, true)) {
                            val layout = if (outgoing) R.layout.message_list_item_out else R.layout.message_list_item_in
                            val body = LayoutInflater.from(context).inflate(layout, null).findViewById<TextView>(R.id.body)
                            for (accent in listOf(Color.BLACK, Color.WHITE, 0xFF375BCD.toInt(), 0xFF007E70.toInt(), 0xFF808080.toInt())) {
                                // Reuse this view across accents, as RecyclerView does.
                                body.text = "Call +1-202-555-0123 or visit https://example.com"
                                MessageBodyStyle.apply(body, outgoing, accent, false)
                                val attrs = context.obtainStyledAttributes(intArrayOf(android.R.attr.windowBackground))
                                val window = attrs.getColor(0, Color.WHITE); attrs.recycle()
                                val backgrounds = dev.texto.privacy.TextoAppearance.cardColors(context, accent).map { ColorUtils.compositeColors(it, window) }
                                val bg = backgrounds.minByOrNull { ColorUtils.calculateContrast(body.currentTextColor,it) }!!
                                check(body.backgroundTintList == null) { "Tint flattened the selected card finish" }
                                check(ColorUtils.calculateContrast(body.currentTextColor, bg) >= 4.5)
                                val spans = (body.text as Spannable).getSpans(0, body.text.length, URLSpan::class.java)
                                check(spans.any { it.url.startsWith("tel:") }) { "Phone number was not linked" }
                                for (span in spans) {
                                    val paint = TextPaint(body.paint).apply { linkColor = body.linkTextColors.defaultColor }
                                    span.updateDrawState(paint)
                                    check(paint.isUnderlineText)
                                    check(ColorUtils.calculateContrast(paint.color, bg) >= 4.5)
                                    val askedLink = object : ClickableSpan() { override fun onClick(widget: View) {} }
                                    askedLink.updateDrawState(paint)
                                    check(ColorUtils.calculateContrast(paint.color, bg) >= 4.5)
                                }
                                cases++
                            }
                        }
                    }
                    }
                } catch (error: Throwable) { failure = error } finally {
                    if(previousFinish == null) appearance.edit().remove("card_finish").commit() else appearance.edit().putString("card_finish",previousFinish).commit()
                }
            }
            failure?.let { throw it }
            result.putString("result", "PASS: $cases message contrast cases across all eight finishes; phone/web and confirmation links, light/dark/AMOLED, incoming/outgoing, custom accents and recycled views; shared palette, Settings cards and scheduled date picker")
            finish(0, result)
        } catch (error: Throwable) { result.putString("error", error.stackTraceToString()); finish(1, result) }
    }
}
