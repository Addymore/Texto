package dev.texto.privacy

import android.content.Context
import org.json.JSONObject

/** Optional visual presets. Opening the section never changes the user's theme. */
object SkinPresets {
    val names = arrayOf("Samsung One UI inspired", "ColorOS / OxygenOS inspired", "Pixel inspired", "Xiaomi HyperOS inspired", "Nothing inspired", "Motorola inspired", "Honor inspired", "vivo inspired")
    private val keys = listOf("card_shape", "card_finish", "card_size", "density", "bubbles")
    fun apply(context: Context, index: Int) {
        require(index in names.indices)
        val prefs = TextoAppearance.prefs(context)
        val edit = prefs.edit()
        if (!prefs.contains("before_oem_preset")) {
            val original = JSONObject()
            keys.forEach { original.put(it, prefs.getString(it, null) ?: JSONObject.NULL) }
            edit.putString("before_oem_preset", original.toString())
        }
        val shape = arrayOf("round", "soft", "pill", "round", "minimal", "round", "round", "soft")
        val finish = arrayOf("neutral", "glass", "tonal", "pastel", "outlined", "tinted", "duotone", "gradient")
        edit.putString("skin_preset", names[index])
            .putString("card_shape", shape[index]).putString("card_finish", finish[index])
            .putString("card_size", "small").putString("density", "compact")
            .putString("bubbles", "fluid").apply()
    }
    fun restore(context: Context) {
        val prefs = TextoAppearance.prefs(context)
        val saved = prefs.getString("before_oem_preset", null) ?: return
        val original = JSONObject(saved)
        val edit = prefs.edit()
        keys.forEach { if(original.isNull(it)) edit.remove(it) else edit.putString(it, original.getString(it)) }
        edit.remove("before_oem_preset").remove("skin_preset").apply()
    }
}
