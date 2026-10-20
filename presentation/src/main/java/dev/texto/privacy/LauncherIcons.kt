package dev.texto.privacy

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dev.octoshrimpy.quik.R

/** Launcher aliases leave the real SMS activity enabled and retain Texto's app identity. */
object LauncherIcons {
    val ids = arrayOf("default", "samsung", "pixel", "oppo", "oneplus", "realme", "xiaomi", "huawei", "honor", "vivo", "motorola", "nothing", "sony", "asus")
    val names = arrayOf("Texto original", "Samsung inspired", "Pixel / Google inspired", "OPPO inspired", "OnePlus inspired", "realme inspired", "Xiaomi inspired", "Huawei inspired", "HONOR inspired", "vivo inspired", "Motorola inspired", "Nothing inspired", "Sony inspired", "ASUS inspired")
    val previews = intArrayOf(R.drawable.texto_icon, R.mipmap.icon_samsung, R.mipmap.icon_pixel, R.mipmap.icon_oppo, R.mipmap.icon_oneplus, R.mipmap.icon_realme, R.mipmap.icon_xiaomi, R.mipmap.icon_huawei, R.mipmap.icon_honor, R.mipmap.icon_vivo, R.mipmap.icon_motorola, R.mipmap.icon_nothing, R.mipmap.icon_sony, R.mipmap.icon_asus)
    fun component(context: Context, id: String) = ComponentName(context.packageName,"dev.texto.launcher.$id")
    fun selected(context: Context): String = ids.firstOrNull { id ->
        when(context.packageManager.getComponentEnabledSetting(component(context,id))) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> id=="default"
            else -> false
        }
    } ?: "default"
    fun apply(context: Context, id: String) {
        require(id in ids)
        val pm=context.packageManager
        fun state(key: String)=if(key==id) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        if(Build.VERSION.SDK_INT>=33) {
            pm.setComponentEnabledSettings(ids.map { PackageManager.ComponentEnabledSetting(component(context,it),state(it),PackageManager.DONT_KILL_APP) })
        } else {
            // Enable first: older Android must never be left without a launcher entry.
            pm.setComponentEnabledSetting(component(context,id),state(id),PackageManager.DONT_KILL_APP)
            ids.filter { it!=id }.forEach { pm.setComponentEnabledSetting(component(context,it),state(it),PackageManager.DONT_KILL_APP) }
        }
    }
}
