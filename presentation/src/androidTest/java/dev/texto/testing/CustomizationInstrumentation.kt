package dev.texto.testing

import android.app.Instrumentation
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import dev.octoshrimpy.quik.R
import dev.texto.privacy.*

class CustomizationInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    private fun main(block: () -> Unit) { var error: Throwable?=null; runOnMainSync { try { block() } catch(e: Throwable) { error=e } }; error?.let { throw it } }
    private fun nodes(v: View): List<View> = listOf(v)+if(v is ViewGroup) (0 until v.childCount).flatMap { nodes(v.getChildAt(it)) } else emptyList()
    override fun onStart() {
        val result=Bundle(); val p=TextoAppearance.prefs(targetContext); val saved=p.all
        val pm=targetContext.packageManager
        val oldIcons=LauncherIcons.ids.associateWith { pm.getComponentEnabledSetting(LauncherIcons.component(targetContext,it)) }
        var screen: android.app.Activity?=null
        try {
            check(android.os.Build.MODEL.contains("sdk",true)) { "Emulator only" }
            for(id in LauncherIcons.ids) {
                LauncherIcons.apply(targetContext,id)
                check(LauncherIcons.selected(targetContext)==id)
                val launch=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(targetContext.packageName)
                val entries=pm.queryIntentActivities(launch,0)
                check(entries.size==1 && entries.single().activityInfo.name=="dev.texto.launcher.$id") { "Duplicate or missing launcher: $id" }
                check(pm.getActivityInfo(android.content.ComponentName(targetContext,"dev.octoshrimpy.quik.feature.main.MainActivity"),0).enabled)
                checkNotNull(pm.getLaunchIntentForPackage(targetContext.packageName))
                checkNotNull(entries.single().loadIcon(pm))
            }
            screen=startActivitySync(Intent(targetContext,dev.octoshrimpy.quik.feature.settings.SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            val host=screen!!
            for(direction in listOf(View.LAYOUT_DIRECTION_LTR,View.LAYOUT_DIRECTION_RTL)) {
                main { host.window.decorView.layoutDirection=direction; host.window.decorView.requestLayout() }; waitForIdleSync()
                main {
                    val all=nodes(host.window.decorView)
                    for(row in all.filterIsInstance<dev.octoshrimpy.quik.common.widget.PreferenceView>().filter { it.visibility==View.VISIBLE }) {
                        val position=IntArray(2); val title=IntArray(2); row.getLocationOnScreen(position); row.titleView.getLocationOnScreen(title)
                        val offset=if(direction==View.LAYOUT_DIRECTION_LTR) title[0]-position[0] else position[0]+row.width-title[0]-row.titleView.width
                        check(kotlin.math.abs(offset-60*row.resources.displayMetrics.density)<2) { "Preference alignment $offset for ${row.title}" }
                    }
                    for(row in all.filterIsInstance<com.google.android.material.button.MaterialButton>().filter { it.visibility==View.VISIBLE }) {
                        val padding=if(direction==View.LAYOUT_DIRECTION_LTR) row.compoundPaddingLeft else row.compoundPaddingRight
                        check(kotlin.math.abs(padding-60*row.resources.displayMetrics.density)<2) { "Button alignment $padding for ${row.text}" }
                    }
                }
            }
            main {
                val body=TextView(host)
                p.edit().putString("card_finish","tonal").putInt("sent_color",Color.BLACK).putInt("received_color",Color.WHITE).putString("message_font","serif").putString("message_line_height","spacious").commit()
                MessageBodyStyle.apply(body,true,Color.BLUE,false)
                check(body.currentTextColor==Color.WHITE && body.linkTextColors.defaultColor==Color.WHITE)
                check(body.typeface==android.graphics.Typeface.SERIF && body.lineSpacingMultiplier==1.3f)
                MessageBodyStyle.apply(body,false,Color.BLUE,false); check(body.currentTextColor==Color.BLACK)
                SkinPresets.apply(host,0); check(!p.contains("sent_color")); SkinPresets.restore(host); check(p.getInt("sent_color",0)==Color.BLACK)
                p.edit().putString("card_finish","none").commit(); MessageBodyStyle.apply(body,true,Color.BLUE,false)
                check(body.background is android.graphics.drawable.RippleDrawable) { "Custom colors overrode no cards" }
                for(direction in listOf("diagonal","horizontal","vertical")) for(border in listOf("auto","0","1","2","3")) {
                    p.edit().putString("card_finish","gradient").putString("gradient_direction",direction).putString("border_width",border).commit()
                    val background=TextoAppearance.card(host) as android.graphics.drawable.RippleDrawable
                    val shape=background.getDrawable(0) as android.graphics.drawable.GradientDrawable
                    check(shape.orientation==when(direction) { "vertical" -> android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM; "horizontal" -> android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT; else -> android.graphics.drawable.GradientDrawable.Orientation.TL_BR })
                }
            }
            result.putString("result","PASS: all 14 launcher choices resolve exactly one entry and keep MainActivity enabled; Settings icon/text columns align in LTR/RTL; sent/received contrast, typography, preset restore, no-cards override and gradient/border combinations")
        } catch(e: Throwable) { result.putString("error",e.stackTraceToString()) }
        finally {
            oldIcons.entries.sortedBy { it.value==android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED }.forEach { (id,state) -> pm.setComponentEnabledSetting(LauncherIcons.component(targetContext,id),state,android.content.pm.PackageManager.DONT_KILL_APP) }
            main { p.edit().clear().apply { saved.forEach { (k,v) -> when(v) { is String -> putString(k,v); is Int -> putInt(k,v); is Long -> putLong(k,v); is Boolean -> putBoolean(k,v); is Float -> putFloat(k,v); is Set<*> -> putStringSet(k,v.filterIsInstance<String>().toSet()) } } }.commit(); screen?.finish() }
        }
        finish(if(result.containsKey("error")) 1 else 0,result)
    }
}
