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
    fun prefs(context: Context) = context.applicationContext.getSharedPreferences("texto_privacy", Context.MODE_PRIVATE)
    fun accent(context: Context): Int {
        if (android.os.Build.VERSION.SDK_INT >= 31 && prefs(context).getBoolean("dynamic_colors",false))
            return context.getColor(android.R.color.system_accent1_600)
        return android.preference.PreferenceManager.getDefaultSharedPreferences(context).getInt("theme", 0xFF375BCD.toInt())
    }
    fun card(context: Context, selected: Boolean = false): android.graphics.drawable.Drawable {
        val p = prefs(context); val dark = context.resources.configuration.uiMode and 0x30 == 0x20
        val radius = when(p.getString("card_shape", "soft")) { "minimal" -> 12; "round" -> 30; else -> 16 }
        val base = if(dark) 0xFF23262D.toInt() else 0xFFF3F5F9.toInt()
        val finish = p.getString("card_finish","tonal")
        val fill = if (selected) androidx.core.graphics.ColorUtils.blendARGB(base,accent(context),.24f) else if (finish == "tinted" || finish == "tonal") androidx.core.graphics.ColorUtils.blendARGB(base,accent(context),if(dark) .16f else .08f) else base
        val shape = GradientDrawable().apply { cornerRadius = radius * context.resources.displayMetrics.density; setColor(ColorStateList(arrayOf(intArrayOf(android.R.attr.state_activated), intArrayOf()), intArrayOf(androidx.core.graphics.ColorUtils.blendARGB(base,accent(context),.24f), fill))); if (finish == "outlined" || finish == "tonal") setStroke((context.resources.displayMetrics.density).toInt().coerceAtLeast(1), androidx.core.graphics.ColorUtils.blendARGB(base,accent(context),if (finish == "tonal") .18f else .45f)) }
        return RippleDrawable(ColorStateList.valueOf(accent(context) and 0x00FFFFFF or 0x22000000),shape,null)
    }
    fun onAccent(context: Context): Int = if (androidx.core.graphics.ColorUtils.calculateLuminance(accent(context)) > .179) Color.BLACK else Color.WHITE
    fun styleConversation(binding: dev.octoshrimpy.quik.databinding.ConversationListItemBinding, count: Long, unread: Long, selected: Boolean = false) {
        val context = binding.root.context; val p = prefs(context); val d = context.resources.displayMetrics.density
        val spacing = when(p.getString("density","comfortable")) { "compact" -> 12; "airy" -> 20; else -> 16 }
        val padding = (spacing*d).toInt()
        binding.root.setPadding(padding,padding,padding,padding)
        binding.root.minimumHeight = ((spacing*4+30)*d).toInt()
        binding.root.background = card(context,selected)
        binding.avatars.visibility = if(p.getBoolean("list_avatars",true)) View.VISIBLE else View.GONE
        val lines = p.getString("preview_lines","2")?.toIntOrNull()?.coerceIn(0,3) ?: 2
        binding.snippet.visibility = if(lines == 0) View.GONE else View.VISIBLE
        binding.snippet.maxLines = lines.coerceAtLeast(1)
        binding.messageCount.visibility = if(p.getBoolean("message_counts",true)) View.VISIBLE else View.GONE
        binding.messageCount.text = "$count ${if(count == 1L) "message" else "messages"}"
        val dot = p.getString("unread_style","count") == "dot"
        binding.unread.visibility = if(unread > 0) View.VISIBLE else View.GONE
        binding.unread.text = if(dot) "" else if(unread > 99) "99+ new" else "$unread new"
        binding.unread.setTextColor(onAccent(context))
        binding.unread.minWidth = ((if(dot) 10 else 24)*d).toInt()
        binding.unread.layoutParams = binding.unread.layoutParams.apply { width = if(dot) (10*d).toInt() else -2; height = ((if(dot) 10 else 24)*d).toInt() }
        binding.unread.setPadding(if(dot) 0 else (8*d).toInt(),0,if(dot) 0 else (8*d).toInt(),0)
        binding.unread.background = GradientDrawable().apply { cornerRadius=12*d; setColor(accent(context)) }
    }
    fun styleSettingsCard(view: View) {
        val d = view.resources.displayMetrics.density
        view.backgroundTintList = null
        view.background = card(view.context)
        // MaterialButton transfers its stored support tint when a custom drawable is assigned.
        if (view is com.google.android.material.button.MaterialButton) view.supportBackgroundTintList = null
        view.backgroundTintList = null
        view.minimumHeight = (64*d).toInt()
        (view.layoutParams as? android.view.ViewGroup.MarginLayoutParams)?.let {
            it.setMargins((16*d).toInt(),(4*d).toInt(),(16*d).toInt(),(4*d).toInt())
            view.layoutParams = it
        }
        if (view is android.widget.TextView) {
            view.setTextColor(if (view.resources.configuration.uiMode and 0x30 == 0x20) Color.WHITE else 0xFF20232B.toInt())
            view.gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
            view.setPadding((20*d).toInt(),(12*d).toInt(),(20*d).toInt(),(12*d).toInt())
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
        if (!prefs(activity).getBoolean("high_refresh", true)) { activity.window.attributes = activity.window.attributes.apply { preferredDisplayModeId = 0 }; return }
        @Suppress("DEPRECATION") val display = activity.windowManager.defaultDisplay
        val current = display.mode
        val best = display.supportedModes.filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }.maxByOrNull { it.refreshRate } ?: return
        activity.window.attributes = activity.window.attributes.apply { preferredDisplayModeId = best.modeId }
    }
}
