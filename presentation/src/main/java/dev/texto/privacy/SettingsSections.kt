package dev.texto.privacy

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import dev.octoshrimpy.quik.R

object SettingsSections {
    fun organize(parent: LinearLayout) {
        val rows=(0 until parent.childCount).map { parent.getChildAt(it) }.filter { it.id != View.NO_ID }
        val appearance=setOf(R.id.theme,R.id.night,R.id.black,R.id.nightStart,R.id.nightEnd,R.id.textSize,R.id.autoColor,R.id.systemFont,R.id.showStt)
        val privacy=setOf(R.id.textoPrivacySettings,R.id.textoBinSettings,R.id.disableScreenshots,R.id.messsageLinkHandling)
        val loose=(0 until parent.childCount).map { parent.getChildAt(it) }.filter { it is com.google.android.material.button.MaterialButton && it.id == View.NO_ID }
        parent.removeAllViews()
        val d=parent.resources.displayMetrics.density
        listOf("GENERAL" to (rows.filter { it.id !in appearance && it.id !in privacy }+loose),"APPEARANCE" to rows.filter { it.id in appearance },"PRIVACY" to rows.filter { it.id in privacy }).forEach { (title,items) ->
            parent.addView(TextView(parent.context).apply {
                text=title;setTextColor(TextoAppearance.readableText(parent.context)); textSize=13f; setTypeface(null,android.graphics.Typeface.BOLD)
                setPadding((20*d).toInt(),(28*d).toInt(),(16*d).toInt(),(8*d).toInt())
                isClickable=false; isFocusable=false
                if(android.os.Build.VERSION.SDK_INT>=28) isAccessibilityHeading=true
            })
            items.forEach { row -> row.tag="texto_submenu"; parent.addView(row); TextoAppearance.styleSettingsCard(row) }
        }
    }
}
