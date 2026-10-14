package dev.texto.privacy

import android.content.Context
import org.json.JSONObject

/** Explicitly applied visual profiles. Texto remains unchanged until Apply is chosen. */
object SkinPresets {
    val names = arrayOf("Samsung One UI inspired", "ColorOS / OxygenOS inspired", "Pixel inspired", "Xiaomi HyperOS inspired", "Nothing inspired", "Motorola inspired", "Honor inspired", "vivo inspired")
    val ids = arrayOf("samsung", "oppo", "pixel", "xiaomi", "nothing", "motorola", "honor", "vivo")
    val descriptions = arrayOf(
        "Large centered, reachable heading; clean conversation list; blue sent bubbles; rounded white settings and a flat navigation bar.",
        "Left-aligned bold heading; soft floating surfaces; green sent bubbles; compact previews and translucent-looking navigation.",
        "Compact heading; clean avatar-led list; pill bubbles; blue tonal surfaces and Material-style navigation.",
        "Large left heading; compact list; blue sent bubbles with asymmetric corners; rounded grouped settings.",
        "Monospaced headings; monochrome surfaces; red controls; outlined settings and squared navigation.",
        "Compact heading; teal sent bubbles; clean conversation list and rounded Material surfaces.",
        "Centered heading; soft blue bubbles; generous rounded surfaces and floating navigation.",
        "Bold left heading; purple sent bubbles; soft rounded surfaces and compact navigation.")
    private val finishes = arrayOf("neutral", "glass", "tonal", "pastel", "outlined", "tinted", "duotone", "gradient")
    fun usesProfileSurface(context: Context): Boolean {
        val index=ids.indexOf(id(context))
        return index>=0 && TextoAppearance.prefs(context).getString("card_finish","tonal")==finishes[index]
    }
    private val keys = listOf("card_shape", "card_finish", "card_size", "density", "bubbles", "oem_layout", "preview_lines", "unread_style")
    fun id(context: Context): String {
        val p=TextoAppearance.prefs(context)
        return p.getString("oem_layout",null)?.takeIf { it in ids }
            ?: names.indexOf(p.getString("skin_preset",null)).takeIf { it>=0 }?.let { ids[it] }.orEmpty()
    }
    fun apply(context: Context, index: Int) {
        require(index in names.indices)
        val prefs = TextoAppearance.prefs(context)
        val defaults=android.preference.PreferenceManager.getDefaultSharedPreferences(context)
        val edit = prefs.edit()
        val original=JSONObject(prefs.getString("before_oem_preset",null) ?: "{}")
        // Extend a 1.7.0 snapshot without losing its original five saved values.
        keys.forEach { if(!original.has(it)) original.put(it,prefs.getString(it,null) ?: JSONObject.NULL) }
        if(!original.has("theme")) original.put("theme",if(defaults.contains("theme")) defaults.getInt("theme",0) else JSONObject.NULL)
        if(!original.has("dynamic_colors")) original.put("dynamic_colors",if(prefs.contains("dynamic_colors")) prefs.getBoolean("dynamic_colors",false) else JSONObject.NULL)
        edit.putString("before_oem_preset",original.toString())
        val shape = arrayOf("round", "soft", "pill", "round", "minimal", "round", "round", "soft")
        val accents=intArrayOf(0xFF1473E6.toInt(),0xFF087F60.toInt(),0xFF285EA8.toInt(),0xFF176FE8.toInt(),0xFFBD2636.toInt(),0xFF007E89.toInt(),0xFF366AE2.toInt(),0xFF7054C9.toInt())
        edit.putString("skin_preset", names[index]).putString("oem_layout",ids[index])
            .putString("card_shape", shape[index]).putString("card_finish", finishes[index])
            .putString("card_size", "small").putString("density", "compact")
            .putString("preview_lines",if(index==1 || index==3) "1" else "2")
            .putString("unread_style",if(index==0 || index==4) "dot" else "count")
            .putBoolean("dynamic_colors",false).putString("bubbles", "fluid").apply()
        defaults.edit().putInt("theme",accents[index]).apply()
    }
    fun restore(context: Context) {
        val prefs = TextoAppearance.prefs(context)
        val saved = prefs.getString("before_oem_preset", null) ?: return
        val original = JSONObject(saved)
        val edit = prefs.edit()
        keys.forEach { if(original.isNull(it)) edit.remove(it) else edit.putString(it, original.getString(it)) }
        if(original.has("dynamic_colors")) {
            if(original.isNull("dynamic_colors")) edit.remove("dynamic_colors") else edit.putBoolean("dynamic_colors",original.getBoolean("dynamic_colors"))
        }
        if(original.has("theme")) android.preference.PreferenceManager.getDefaultSharedPreferences(context).edit().apply {
            if(original.isNull("theme")) remove("theme") else putInt("theme",original.getInt("theme"))
        }.apply()
        edit.remove("before_oem_preset").remove("skin_preset").apply()
    }
}
