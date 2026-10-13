package dev.texto.privacy

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import dev.octoshrimpy.quik.R

object SettingsSections {
    fun organize(parent: LinearLayout) {
        val rows=(0 until parent.childCount).map { parent.getChildAt(it) }.filter { it.id != View.NO_ID }
        val groups=listOf(
            R.string.settings_category_general to listOf(R.id.notifications,R.id.sync,R.id.syncingProgress,R.id.about),
            R.string.texto_settings_inbox_contacts to listOf(R.id.textoContactsSettings,R.id.swipeActions,R.id.unreadAtTop,R.id.mobileOnly,R.id.textoDialingSettings),
            R.string.texto_settings_messaging to listOf(R.id.delayed,R.id.delivery,R.id.signature,R.id.unicode,R.id.longAsMms,R.id.mmsSize,R.id.showStt,R.id.autoEmoji),
            R.string.settings_category_appearance to listOf(R.id.theme,R.id.night,R.id.black,R.id.nightStart,R.id.nightEnd,R.id.textSize,R.id.autoColor,R.id.systemFont),
            R.string.texto_settings_privacy to listOf(R.id.textoPrivacySettings,R.id.textoBinSettings,R.id.disableScreenshots,R.id.messsageLinkHandling))
        val assigned=groups.flatMap { it.second }.toSet()
        val extra=rows.filter { it.id !in assigned }
        parent.removeAllViews()
        val d=parent.resources.displayMetrics.density
        groups.forEachIndexed { index,(title,ids) ->
            val items=ids.mapNotNull { id -> rows.find { it.id == id } } + if(index==0) extra else emptyList()
            if(items.isEmpty()) return@forEachIndexed
            parent.addView(TextView(parent.context).apply {
                setText(title);isAllCaps=true;setTextColor(TextoAppearance.readableText(parent.context)); textSize=13f; setTypeface(null,android.graphics.Typeface.BOLD)
                setPadding((20*d).toInt(),(28*d).toInt(),(16*d).toInt(),(8*d).toInt())
                isClickable=false; isFocusable=false
                if(android.os.Build.VERSION.SDK_INT>=28) isAccessibilityHeading=true
            })
            // Keep preference rows as direct children so existing click subscriptions stay intact.
            items.forEach { row ->
                parent.addView(row)
                if(row.id != R.id.syncingProgress) { row.tag="texto_submenu"; TextoAppearance.styleSettingsCard(row) }
            }
        }
    }
}
