package dev.texto.privacy

import android.content.Context
import android.content.ContextWrapper
import android.app.Activity
import android.view.WindowManager
import android.widget.EditText
import android.widget.ScrollView

/** A stable native editor, outside recycled rows, with editing and the keyboard disabled. */
object SelectMessageText {
    fun show(context: Context, text: String, offset: Int = 0) {
        val field = EditText(context).apply {
            setText(text); keyListener = null; setTextIsSelectable(true)
            showSoftInputOnFocus = false
            val ink = TextoAppearance.readableText(context)
            setTextColor(ink)
            highlightColor = androidx.core.graphics.ColorUtils.setAlphaComponent(ink, 48)
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                textSelectHandle?.mutate()?.setTint(ink)
                textSelectHandleLeft?.mutate()?.setTint(ink)
                textSelectHandleRight?.mutate()?.setTint(ink)
            }
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            textSize = 18f
            val p = (20 * resources.displayMetrics.density).toInt()
            setPadding(p, p, p, p)
            contentDescription = "Message text. Select words or drag the selection handles to copy."
        }
        val scroll = ScrollView(context).apply { addView(field) }
        val dialog = TextoDialogs.builder(context).setTitle("Select message text")
            .setView(scroll).setNeutralButton("Copy selection", null).setPositiveButton("Done", null).create()
        var owner = context
        while (owner is ContextWrapper && owner !is Activity) owner = owner.baseContext
        if ((owner as? Activity)?.window?.attributes?.flags?.and(WindowManager.LayoutParams.FLAG_SECURE) == WindowManager.LayoutParams.FLAG_SECURE)
            dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        dialog.setOnShowListener {
            dialog.getButton(-3).setOnClickListener {
                if (field.selectionStart >= 0 && field.selectionEnd > field.selectionStart)
                    field.onTextContextMenuItem(android.R.id.copy)
            }
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
            field.requestFocus()
            val selectWord = Runnable {
                if (!dialog.isShowing || !field.hasWindowFocus() || text.isEmpty()) return@Runnable
                val at = offset.coerceIn(0, text.length)
                var start = at; var end = at
                while (start > 0 && !text[start - 1].isWhitespace()) start--
                while (end < text.length && !text[end].isWhitespace()) end++
                // Android's public selection action starts its native action mode and
                // draggable handles. Wait for window focus so the dialog transition
                // cannot immediately dismiss them, and reveal words in long messages.
                field.setSelection(start)
                field.bringPointIntoView(start)
                field.performAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_SELECTION,
                    android.os.Bundle().apply {
                        putInt(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, start)
                        putInt(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, end)
                    })
            }
            if (field.hasWindowFocus()) field.post(selectWord)
            else field.viewTreeObserver.addOnWindowFocusChangeListener(object : android.view.ViewTreeObserver.OnWindowFocusChangeListener {
                override fun onWindowFocusChanged(hasFocus: Boolean) {
                    if (hasFocus) {
                        field.viewTreeObserver.removeOnWindowFocusChangeListener(this)
                        field.post(selectWord)
                    }
                }
            })
        }
        dialog.show()
    }
}
