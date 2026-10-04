package dev.texto.privacy

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object TextoDialogs {
    fun builder(context: Context?) = MaterialAlertDialogBuilder(requireNotNull(context), dev.octoshrimpy.quik.R.style.TextoAlertDialog)

    /** Legacy custom dialogs still use AlertDialog directly. */
    fun style(dialog: AlertDialog) {
        val context = dialog.context
        val d = context.resources.displayMetrics.density
        val color = com.google.android.material.color.MaterialColors.getColor(context,
            com.google.android.material.R.attr.colorSurface, android.graphics.Color.DKGRAY)
        dialog.window?.setBackgroundDrawable(InsetDrawable(GradientDrawable().apply {
            setColor(color); cornerRadius = 28*d
        }, (20*d).toInt()))
        listOf(-1,-2,-3).forEach { dialog.getButton(it)?.setTextColor(TextoAppearance.accent(context)) }
    }
}
