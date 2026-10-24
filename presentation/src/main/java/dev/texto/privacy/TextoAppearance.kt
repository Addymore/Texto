package dev.texto.privacy

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator

object TextoAppearance {
    private val mediumTypeface by lazy { android.graphics.Typeface.create("sans-serif-medium", 0) }
    private val monoTypeface by lazy { android.graphics.Typeface.create("monospace", 0) }
    fun prefs(context: Context) = context.applicationContext.getSharedPreferences("texto_privacy", Context.MODE_PRIVATE)
    fun accent(context: Context): Int {
        if (android.os.Build.VERSION.SDK_INT >= 31 && prefs(context).getBoolean("dynamic_colors",false))
            return context.getColor(android.R.color.system_accent1_600)
        return android.preference.PreferenceManager.getDefaultSharedPreferences(context).getInt("theme", 0xFF375BCD.toInt())
    }
    fun noCards(context: Context) = prefs(context).getString("card_finish", "tonal") == "none"
    fun card(context: Context, selected: Boolean = false, accentColor: Int = accent(context), corners: FloatArray? = null, fillColor: Int? = null): android.graphics.drawable.Drawable {
        val p = prefs(context); val dark = context.resources.configuration.uiMode and 0x30 == 0x20
        val radius = when(p.getString("card_shape", "soft")) { "square" -> 0; "pill" -> 48; "minimal" -> 12; "round" -> 30; else -> 16 }
        val base = if(dark) 0xFF23262D.toInt() else if(SkinPresets.id(context) in listOf("samsung","oppo","xiaomi","vivo")) Color.WHITE else 0xFFF3F5F9.toInt()
        val finish = p.getString("card_finish","tonal")
        if (finish == "none") {
            val content = android.graphics.drawable.StateListDrawable().apply {
                addState(intArrayOf(android.R.attr.state_activated), android.graphics.drawable.ColorDrawable(accentColor and 0x00FFFFFF or 0x33000000))
                addState(intArrayOf(), android.graphics.drawable.ColorDrawable(if (selected) accentColor and 0x00FFFFFF or 0x33000000 else Color.TRANSPARENT))
            }
            return RippleDrawable(ColorStateList.valueOf(accentColor and 0x00FFFFFF or 0x22000000), content, null)
        }
        val fills = cardColors(context, accentColor, fillColor)
        val shape = GradientDrawable().apply {
            cornerRadius = radius * context.resources.displayMetrics.density
            if (fills.size > 1) { orientation = when(p.getString("gradient_direction","diagonal")) { "vertical" -> GradientDrawable.Orientation.TOP_BOTTOM; "horizontal" -> GradientDrawable.Orientation.LEFT_RIGHT; else -> GradientDrawable.Orientation.TL_BR }; colors = fills }
            else setColor(ColorStateList(arrayOf(intArrayOf(android.R.attr.state_activated), intArrayOf()), intArrayOf(androidx.core.graphics.ColorUtils.blendARGB(base,accentColor,.24f), if(selected) androidx.core.graphics.ColorUtils.blendARGB(base,accentColor,.24f) else fills[0])))
            if (finish in listOf("outlined", "tonal", "glass", "amoled", "bold")) setStroke((context.resources.displayMetrics.density * if(finish == "bold") 2 else 1).toInt().coerceAtLeast(1), androidx.core.graphics.ColorUtils.blendARGB(base, accentColor, if(finish == "tonal") .18f else .4f))
            p.getString("border_width","auto")?.toFloatOrNull()?.let { width -> setStroke((width.coerceIn(0f,3f)*context.resources.displayMetrics.density).toInt(),controlColor(context)) }
        }
        if (corners != null) shape.cornerRadii = corners
        if (selected && finish in listOf("gradient", "glass", "amoled")) shape.setStroke((2*context.resources.displayMetrics.density).toInt(), accentColor)
        return RippleDrawable(ColorStateList.valueOf(accentColor and 0x00FFFFFF or 0x22000000),shape,null)
    }
    fun cardColors(context: Context, accentColor: Int = accent(context), fillColor: Int? = null): IntArray {
        val dark = context.resources.configuration.uiMode and 0x30 == 0x20
        val base = if(dark) 0xFF23262D.toInt() else if(SkinPresets.id(context) in listOf("samsung","oppo","xiaomi","vivo")) Color.WHITE else 0xFFF3F5F9.toInt()
        val surfaceBase = fillColor ?: base
        fun blend(color: Int, amount: Float) = androidx.core.graphics.ColorUtils.blendARGB(surfaceBase, color, amount)
        if (fillColor != null && !noCards(context)) return when(prefs(context).getString("card_finish","tonal")) {
            "gradient", "duotone" -> intArrayOf(fillColor, androidx.core.graphics.ColorUtils.blendARGB(fillColor,base,.25f))
            else -> intArrayOf(fillColor)
        }
        return when(prefs(context).getString("card_finish", "tonal")) {
            "none" -> intArrayOf(Color.TRANSPARENT)
            "gradient" -> intArrayOf(blend(accentColor, .28f), base)
            "pastel" -> intArrayOf(blend(accentColor, if(dark) .24f else .16f))
            "duotone" -> intArrayOf(blend(accentColor, .22f), blend(androidx.core.graphics.ColorUtils.blendARGB(accentColor, if(dark) Color.WHITE else Color.BLACK,.25f), .12f))
            "contrast" -> intArrayOf(if(dark) 0xFF111111.toInt() else Color.WHITE)
            "glass" -> intArrayOf(blend(Color.WHITE, if(dark) .06f else .55f))
            "amoled" -> intArrayOf(if(dark) Color.BLACK else Color.WHITE)
            "tinted", "tonal" -> intArrayOf(blend(accentColor, if(dark) .16f else .08f))
            else -> intArrayOf(base)
        }
    }
    fun readableText(context: Context): Int {
        val a=context.obtainStyledAttributes(intArrayOf(android.R.attr.windowBackground))
        val window=a.getColor(0,Color.WHITE);a.recycle()
        val backgrounds=cardColors(context).map { androidx.core.graphics.ColorUtils.compositeColors(it,window) }
        return listOf(Color.BLACK,Color.WHITE).maxByOrNull { fg -> backgrounds.minOf { androidx.core.graphics.ColorUtils.calculateContrast(fg,it) } }!!
    }
    /** Keep icons visible even with a very pale/dark user-picked accent. */
    fun controlColor(context: Context): Int {
        val a=context.obtainStyledAttributes(intArrayOf(android.R.attr.windowBackground))
        val window=a.getColor(0,Color.WHITE); a.recycle()
        val backgrounds=cardColors(context).map { androidx.core.graphics.ColorUtils.compositeColors(it,window) }
        val original=accent(context)
        fun contrast(color: Int)=backgrounds.minOf { androidx.core.graphics.ColorUtils.calculateContrast(color,it) }
        if(contrast(original)>=3.0) return original
        val target=listOf(Color.BLACK,Color.WHITE).maxByOrNull { contrast(it) }!!
        for(step in 1..10) {
            val candidate=androidx.core.graphics.ColorUtils.blendARGB(original,target,step/10f)
            if(contrast(candidate)>=3.0) return candidate
        }
        return target
    }
    fun onAccent(context: Context): Int = if (androidx.core.graphics.ColorUtils.calculateLuminance(accent(context)) > .179) Color.BLACK else Color.WHITE
    fun styleConversation(binding: dev.octoshrimpy.quik.databinding.ConversationListItemBinding, count: Long, unread: Long, selected: Boolean = false) {
        val context = binding.root.context; val p = prefs(context); val d = context.resources.displayMetrics.density
        // Keep a style snapshot on the holder, not a global cache of views or message data.
        // Counts/selection still update on every bind; unchanged styles do not request layout.
        val style = listOf(SkinPresets.id(context),p.getString("density","comfortable"), p.getString("card_size","medium"),
            p.getString("card_shape","soft"), p.getString("card_finish","tonal"), p.getString("border_width","auto"), p.getString("gradient_direction","diagonal"), accent(context),
            context.resources.configuration.uiMode, d, binding.unread.textSize, selected, p.getBoolean("list_avatars",true),
            p.getString("preview_lines","2"), p.getBoolean("message_counts",true), p.getString("unread_style","count"))
        val dot = p.getString("unread_style","count") == "dot"
        if (binding.root.getTag(dev.octoshrimpy.quik.R.id.texto_conversation_style) != style) {
            binding.root.setTag(dev.octoshrimpy.quik.R.id.texto_conversation_style, style)
            val spacing = when(p.getString("density","comfortable")) { "compact" -> 4; "airy" -> 8; else -> 6 }
            val scale = when(p.getString("card_size","medium")) { "small" -> .85f; "large" -> 1.15f; else -> 1f }
            binding.root.setPadding((12*d*scale).toInt(),(spacing*d*scale).toInt(),(12*d*scale).toInt(),(spacing*d*scale).toInt())
            binding.root.minimumHeight = (60*d*scale).toInt().coerceAtLeast((48*d).toInt())
            val profile=SkinPresets.id(context)
            binding.root.background = if(SkinPresets.usesProfileSurface(context) && profile in listOf("samsung","pixel","xiaomi","motorola"))
                android.graphics.drawable.GradientDrawable().apply { cornerRadius=16*d; setColor(if(selected) androidx.core.graphics.ColorUtils.setAlphaComponent(accent(context),40) else Color.TRANSPARENT) }
                else card(context,selected)
            if(profile.isNotEmpty()) {
                binding.title.typeface=if(profile=="nothing") monoTypeface else mediumTypeface
            }
            binding.avatars.visibility = if(p.getBoolean("list_avatars",true)) View.VISIBLE else View.GONE
            val avatarSize = (40*d*scale).toInt()
            if (binding.avatars.layoutParams.width != avatarSize) binding.avatars.layoutParams = binding.avatars.layoutParams.apply { width=avatarSize; height=avatarSize }
            val lines = p.getString("preview_lines","2")?.toIntOrNull()?.coerceIn(0,3) ?: 2
            binding.snippet.visibility = if(lines == 0) View.GONE else View.VISIBLE
            binding.snippet.maxLines = lines.coerceAtLeast(1)
            binding.messageCount.visibility = if(p.getBoolean("message_counts",true)) View.VISIBLE else View.GONE
            binding.unread.setTextColor(onAccent(context))
            binding.unread.minWidth = ((if(dot) 10 else 20)*d).toInt()
            binding.unread.layoutParams = binding.unread.layoutParams.apply {
                width = if(dot) (10*d).toInt() else -2
                height = if(dot) (10*d).toInt() else maxOf((20*d).toInt(), kotlin.math.ceil(binding.unread.paint.fontSpacing+4*d).toInt())
            }
            binding.unread.setPadding(if(dot) 0 else (6*d).toInt(),0,if(dot) 0 else (6*d).toInt(),0)
            binding.unread.background = GradientDrawable().apply { cornerRadius=10*d; setColor(accent(context)) }
        }
        val countText = "$count ${if(count == 1L) "message" else "messages"}"
        if (binding.messageCount.text.toString() != countText) binding.messageCount.text = countText
        binding.unread.visibility = if(unread > 0) View.VISIBLE else View.GONE
        val unreadText = if(dot) "" else if(unread > 99) "99+ new" else "$unread new"
        if (binding.unread.text.toString() != unreadText) binding.unread.text = unreadText
    }
    fun styleSettingsCard(view: View) {
        val d = view.resources.displayMetrics.density
        view.backgroundTintList = null
        view.background = card(view.context)
        // MaterialButton transfers its stored support tint when a custom drawable is assigned.
        if (view is com.google.android.material.button.MaterialButton) {
            view.supportBackgroundTintList = null
            if(view.icon == null) view.setIconResource(dev.octoshrimpy.quik.R.drawable.ic_settings_black_24dp)
            view.iconSize=(24*d).toInt()
            view.iconPadding=(16*d).toInt()
            view.iconTint = ColorStateList.valueOf(controlColor(view.context))
            view.iconGravity = com.google.android.material.button.MaterialButton.ICON_GRAVITY_START
            view.insetTop=0; view.insetBottom=0
            view.isAllCaps=false
        }
        view.backgroundTintList = null
        val p = prefs(view.context)
        val scale = when(p.getString("card_size", "medium")) { "small" -> .85f; "large" -> 1.15f; else -> 1f }
        val spacing = when(p.getString("density", "comfortable")) { "compact" -> 6; "airy" -> 10; else -> 8 }
        view.minimumHeight = (56*d*scale).toInt().coerceAtLeast((48*d).toInt())
        (view.layoutParams as? android.view.ViewGroup.MarginLayoutParams)?.let {
            it.setMargins(((if(view.tag == "texto_submenu") 36 else 16)*d).toInt(),(4*d).toInt(),(16*d).toInt(),(4*d).toInt())
            it.marginStart=((if(view.tag == "texto_submenu") 36 else 16)*d).toInt(); it.marginEnd=(16*d).toInt()
            view.layoutParams = it
        }
        if (view is android.widget.TextView) {
            view.setTextColor(readableText(view.context))
            view.gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
            view.setPaddingRelative((20*d).toInt(),(spacing*d*scale).toInt(),(20*d).toInt(),(spacing*d*scale).toInt())
        }
    }
    fun smooth(list: RecyclerView) {
        list.setHasFixedSize(true); list.setItemViewCacheSize(20)
        (list.layoutManager as? LinearLayoutManager)?.initialPrefetchItemCount = 4
        (list.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        if (prefs(list.context).getBoolean("reduce_motion",false)) list.itemAnimator = null
        list.overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
    }
    fun refreshRate(activity: Activity) {
        if (!prefs(activity).getBoolean("high_refresh", false)) { if (activity.window.attributes.preferredDisplayModeId != 0) activity.window.attributes = activity.window.attributes.apply { preferredDisplayModeId = 0 }; return }
        @Suppress("DEPRECATION") val display = activity.windowManager.defaultDisplay
        val current = display.mode
        val best = display.supportedModes.filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }.maxByOrNull { it.refreshRate } ?: return
        if (activity.window.attributes.preferredDisplayModeId != best.modeId) activity.window.attributes = activity.window.attributes.apply { preferredDisplayModeId = best.modeId }
    }
}
