package dev.texto.testing

import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import dev.octoshrimpy.quik.R
import dev.octoshrimpy.quik.feature.settings.about.AboutActivity
import dev.octoshrimpy.quik.feature.settings.SettingsActivity
import dev.texto.privacy.*

class ThemeConsistencyInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    private fun main(block: () -> Unit) {
        var error: Throwable?=null
        runOnMainSync { try { block() } catch(e: Throwable) { error=e } }
        error?.let { throw it }
    }
    private fun nodes(v: View): List<View> = listOf(v) + if(v is ViewGroup) (0 until v.childCount).flatMap { nodes(v.getChildAt(it)) } else emptyList()
    private fun windows() = android.view.inspector.WindowInspector.getGlobalWindowViews().flatMap { nodes(it) }
    private fun fill(v: View): Int {
        val bitmap=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888)
        v.background.setBounds(0,0,100,100); v.background.draw(Canvas(bitmap))
        return bitmap.getPixel(50,50).also { bitmap.recycle() }
    }
    override fun onStart() {
        val result=Bundle()
        val prefs=TextoAppearance.prefs(targetContext)
        val saved=prefs.all
        val defaults=android.preference.PreferenceManager.getDefaultSharedPreferences(targetContext)
        val oldAccent=defaults.getInt("theme",0); val hadAccent=defaults.contains("theme")
        var screen: android.app.Activity?=null
        try {
            check(android.os.Build.MODEL.contains("sdk",true)) { "Synthetic emulator only" }
            screen=startActivitySync(Intent(targetContext,SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            val host=screen!!
            main {
                val settings=nodes(host.window.decorView)
                for(button in settings.filterIsInstance<com.google.android.material.button.MaterialButton>()) {
                    check(button.icon!=null) { "Missing settings icon: ${button.text}" }
                }
                for(row in settings.filterIsInstance<dev.octoshrimpy.quik.common.widget.PreferenceView>()) {
                    check(row.findViewById<android.widget.ImageView>(R.id.icon).drawable!=null) { "Missing preference icon: ${row.id}" }
                }
            }
            main { host.finish() }; waitForIdleSync()
            val renderContext=android.view.ContextThemeWrapper(targetContext,R.style.AppTheme)
            var cases=0
            for(profile in SkinPresets.names.indices) {
                main {
                    SkinPresets.apply(renderContext,profile)
                    TextoTheme.apply(renderContext)
                    val profileId=SkinPresets.id(renderContext)
                    prefs.edit().remove("oem_layout").commit()
                    check(SkinPresets.id(renderContext)==profileId) { "Older saved OEM preset lost its layout" }
                    val defaultFinish=prefs.getString("card_finish",null)
                    for(finish in listOf(defaultFinish,"tonal","neutral","tinted","outlined","gradient","glass","amoled","pastel","duotone","bold","contrast","none").distinct()) {
                        prefs.edit().putString("card_finish",finish).commit()
                        for(outgoing in listOf(false,true)) {
                            val body=android.view.LayoutInflater.from(renderContext).inflate(if(outgoing) R.layout.message_list_item_out else R.layout.message_list_item_in,null).findViewById<TextView>(R.id.body)
                            MessageBodyStyle.apply(body,outgoing,android.graphics.Color.RED,false)
                            val first=fill(body)
                            MessageBodyStyle.apply(body,outgoing,android.graphics.Color.BLUE,false)
                            check(first==fill(body)) { "Legacy contact accent replaced OEM finish: $profile/$finish" }
                            val attachment=View(renderContext)
                            val foreground=MessageBodyStyle.surface(attachment,android.graphics.Color.RED,outgoing=outgoing)
                            check(fill(attachment)==first && foreground==body.currentTextColor) { "SMS/MMS surface mismatch: $profile/$finish/$outgoing" }
                            if(finish=="none") check(android.graphics.Color.alpha(first)==0)
                            cases++
                        }
                    }
                }
                waitForIdleSync()
            }
            screen=startActivitySync(Intent(targetContext,AboutActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            val about=screen!!
            repeat(2) {
                main { about.findViewById<View>(R.id.version).performClick() }
                for(attempt in 0 until 40) {
                    SystemClock.sleep(50); waitForIdleSync()
                    if(windows().filterIsInstance<TextView>().any { it.text.toString().contains("What’s new in Texto 1.7.3") }) break
                }
                main {
                    val text=windows().filterIsInstance<TextView>().map { it.text.toString() }
                    check(text.any { it.contains("What’s new in Texto 1.7.3") }) { "Version did not open current changelog" }
                    check(text.any { it.contains("Native message text selection") }) { "Current release details missing" }
                    windows().filterIsInstance<TextView>().last { it.text.toString().equals("Got it",true) }.performClick()
                }
                waitForIdleSync()
            }
            result.putString("result","PASS: $cases OEM/custom SMS and attachment surface cases, legacy-contact-color independence, no cards, settings icons and repeatable current-version About changelog")
        } catch(e: Throwable) { result.putString("error",e.stackTraceToString()) }
        finally {
            main { screen?.finish() }
            val edit=prefs.edit().clear()
            saved.forEach { (key,value) -> when(value) { is String -> edit.putString(key,value); is Boolean -> edit.putBoolean(key,value); is Int -> edit.putInt(key,value); is Long -> edit.putLong(key,value); is Float -> edit.putFloat(key,value) } }
            edit.commit()
            defaults.edit().apply { if(hadAccent) putInt("theme",oldAccent) else remove("theme") }.commit()
        }
        finish(if(result.containsKey("error")) 1 else 0,result)
    }
}
