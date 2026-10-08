package dev.texto.privacy

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object TextoDialogs {
    private fun background(context: Context): android.graphics.drawable.Drawable {
        // A modal still needs an opaque surface when decorative cards are disabled.
        return if (TextoAppearance.noCards(context)) android.graphics.drawable.ColorDrawable(
            com.google.android.material.color.MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurface, android.graphics.Color.DKGRAY))
        else TextoAppearance.card(context)
    }
    fun builder(context: Context?) = MaterialAlertDialogBuilder(requireNotNull(context), dev.octoshrimpy.quik.R.style.TextoAlertDialog)
        .setBackground(background(context))

    /** Legacy custom dialogs still use AlertDialog directly. */
    fun style(dialog: AlertDialog) {
        val context = dialog.context
        val d = context.resources.displayMetrics.density
        dialog.window?.setBackgroundDrawable(InsetDrawable(background(context), (20*d).toInt()))
        listOf(-1,-2,-3).forEach { dialog.getButton(it)?.setTextColor(TextoAppearance.accent(context)) }
    }
}
