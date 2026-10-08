package dev.texto.privacy

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import com.google.android.material.button.MaterialButton
import dev.octoshrimpy.quik.R

/** Floating iOS-inspired pill navigation, using native Material buttons. */
class TextoTabBar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    var onTabSelected: ((Int) -> Unit)? = null
    private val tabs = mutableListOf<MaterialButton>()
    private val dark = resources.configuration.uiMode and 0x30 == 0x20
    init {
        orientation = HORIZONTAL; gravity = Gravity.CENTER; setPadding(dp(6), dp(5), dp(6), dp(5))
        elevation = dp(8).toFloat()
        background = TextoAppearance.card(context)
        if (TextoAppearance.noCards(context)) { background = null; elevation = 0f }
        val labels = listOf("Messages", "Themes")
        val icons = listOf(R.drawable.ic_message_black_24dp, R.drawable.ic_invert_colors_black_24dp)
        labels.forEachIndexed { i, label ->
            val button = MaterialButton(context, null, com.google.android.material.R.attr.materialButtonStyle).apply {
                text = label; textSize = 11f; isAllCaps = false; minWidth = 0; minimumWidth = 0
                insetTop = 0; insetBottom = 0; cornerRadius = dp(28); strokeWidth = 0
                setPadding(dp(4), dp(4), dp(4), dp(4)); iconSize = dp(21); iconPadding = dp(2)
                setIconResource(icons[i]); iconGravity = MaterialButton.ICON_GRAVITY_TEXT_TOP
                setOnClickListener { onTabSelected?.invoke(i) }
            }
            tabs += button; addView(button, LayoutParams(0, dp(56), 1f))
        }
        select(0)
    }
    fun select(index: Int) {
        tabs.forEachIndexed { i, button ->
            val selected = i == index
            button.isSelected = selected
            button.contentDescription = "${button.text}${if (selected) ", selected" else ""}"
            val accent = TextoAppearance.accent(context)
            val foreground = if (selected && !TextoAppearance.noCards(context)) (if (dark) androidx.core.graphics.ColorUtils.blendARGB(accent, Color.WHITE, 0.45f) else if(androidx.core.graphics.ColorUtils.calculateContrast(accent,Color.WHITE) < 4.5) androidx.core.graphics.ColorUtils.blendARGB(accent,Color.BLACK,.6f) else accent) else (if (dark) 0xFFC8CBD4.toInt() else 0xFF686E7A.toInt())
            button.setTextColor(foreground); button.iconTint = ColorStateList.valueOf(foreground)
            button.backgroundTintList = ColorStateList.valueOf(if (selected && !TextoAppearance.noCards(context)) (if (dark) androidx.core.graphics.ColorUtils.blendARGB(0xFF23262D.toInt(), accent, .2f) else Color.WHITE) else Color.TRANSPARENT)
            button.elevation = if (selected && !TextoAppearance.noCards(context)) dp(1).toFloat() else 0f
        }
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
