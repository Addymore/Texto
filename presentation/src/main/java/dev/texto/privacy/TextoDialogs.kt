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
    fun builder(context: Context?): MaterialAlertDialogBuilder {
        val host = requireNotNull(context)
        return object : MaterialAlertDialogBuilder(host, dev.octoshrimpy.quik.R.style.TextoAlertDialog) {
            override fun create(): AlertDialog = super.create().also { dialog ->
                var owner = host
                while (owner is android.content.ContextWrapper && owner !is android.app.Activity) owner = owner.baseContext
                val secure = android.view.WindowManager.LayoutParams.FLAG_SECURE
                if ((owner as? android.app.Activity)?.window?.attributes?.flags?.and(secure) == secure) {
                    dialog.window?.addFlags(secure)
                    // A dialog is a separate window: hiding its protected Activity
                    // does not hide this window when another screen takes focus.
                    (owner as? androidx.lifecycle.LifecycleOwner)?.lifecycle?.let { lifecycle ->
                        val observer = object : androidx.lifecycle.DefaultLifecycleObserver {
                            override fun onPause(owner: androidx.lifecycle.LifecycleOwner) {
                                lifecycle.removeObserver(this)
                                dialog.dismiss()
                            }
                            override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) {
                                lifecycle.removeObserver(this)
                                dialog.dismiss()
                            }
                        }
                        lifecycle.addObserver(observer)
                        dialog.setOnDismissListener { lifecycle.removeObserver(observer) }
                    }
                }
            }
        }.setBackground(background(host))
    }

    /** Legacy custom dialogs still use AlertDialog directly. */
    fun style(dialog: AlertDialog) {
        val context = dialog.context
        val d = context.resources.displayMetrics.density
        dialog.window?.setBackgroundDrawable(InsetDrawable(background(context), (20*d).toInt()))
        listOf(-1,-2,-3).forEach { dialog.getButton(it)?.setTextColor(TextoAppearance.accent(context)) }
    }
}
