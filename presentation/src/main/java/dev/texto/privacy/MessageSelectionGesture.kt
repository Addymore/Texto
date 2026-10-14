package dev.texto.privacy

import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.*
import android.widget.TextView
import dev.octoshrimpy.quik.R
import kotlin.math.abs

/** Keep normal message actions separate from deliberate in-bubble text selection. */
class MessageSelectionGesture(private val body: TextView, private val row: View, private val selectionActive: () -> Boolean) : View.OnTouchListener {
    private var id=Long.MIN_VALUE
    private var x=0f; private var y=0f; private var down=0L
    private var held=false; private var canceled=false
    private var selecting=false
    private var consumeRelease=false
    private var actionMode: ActionMode? = null
    private val slop=ViewConfiguration.get(body.context).scaledTouchSlop * 2
    private val selectText=Runnable { if(held && !canceled && body.isShown && body.hasWindowFocus() && !selectionActive()) beginTextSelection() }
    init {
        body.setTextIsSelectable(false)
        body.setOnTouchListener(this)
        body.setOnLongClickListener { if(!selecting) { beginTextSelection(); true } else false }
        body.customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean { actionMode=mode; return true }
            override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false
            override fun onActionItemClicked(mode: ActionMode, item: MenuItem) = false
            override fun onDestroyActionMode(mode: ActionMode) {
                actionMode=null
                body.post { if (actionMode == null) endSelection() }
            }
        }
        body.addOnAttachStateChangeListener(object: View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {}
            override fun onViewDetachedFromWindow(v: View) { reset(); endSelection() }
        })

    }
    fun bind(messageId: Long) { if(id != messageId) { reset(); endSelection(); id=messageId } }
    private fun reset() {
        held=false; canceled=true; body.removeCallbacks(selectText)
        body.parent?.requestDisallowInterceptTouchEvent(false)
    }
    private fun beginTextSelection() {
        val text = body.text.toString()
        val offset = body.getOffsetForPosition(x, y).coerceIn(0, text.length)
        reset()
        body.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        if (text.isEmpty()) return
        selecting=true; consumeRelease=true
        body.setTextIsSelectable(true)
        body.requestFocus()
        var start=offset.coerceAtMost(text.lastIndex); var end=start
        while(start>0 && !text[start-1].isWhitespace()) start--
        while(end<text.length && !text[end].isWhitespace()) end++
        val selection=android.os.Bundle().apply {
            putInt(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT,start)
            putInt(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT,end)
        }
        // Let TextView create its editor/layout before asking Android for native handles.
        body.post {
            if(selecting && body.isAttachedToWindow && body.hasWindowFocus()) {
                body.performAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_SELECTION,selection)
            }
        }
    }
    private fun endSelection() {
        if (!selecting) return
        selecting=false; consumeRelease=false
        actionMode?.finish(); actionMode=null
        body.setTextIsSelectable(false)
        body.clearFocus()
        body.movementMethod=android.text.method.LinkMovementMethod.getInstance()
    }
    override fun onTouch(v: View, e: MotionEvent): Boolean {
        if (selecting) {
            // The original finger-up must not collapse the newly selected word.
            if (consumeRelease) {
                if(e.actionMasked == MotionEvent.ACTION_UP || e.actionMasked == MotionEvent.ACTION_CANCEL) consumeRelease=false
                return true
            }
            return false // Android owns subsequent handle drags and the Copy action.
        }
        when(e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                x=e.x; y=e.y; down=e.eventTime; held=true; canceled=false
                body.parent?.requestDisallowInterceptTouchEvent(true)
                body.postDelayed(selectText,3000L)
                return true
            }
            MotionEvent.ACTION_MOVE -> if(abs(e.x-x)>slop || abs(e.y-y)>slop) { canceled=true; held=false; body.removeCallbacks(selectText); body.parent?.requestDisallowInterceptTouchEvent(false) }
            MotionEvent.ACTION_POINTER_DOWN,MotionEvent.ACTION_CANCEL -> { canceled=true; held=false; body.removeCallbacks(selectText); body.parent?.requestDisallowInterceptTouchEvent(false) }
            MotionEvent.ACTION_UP -> {
                held=false; body.removeCallbacks(selectText); body.parent?.requestDisallowInterceptTouchEvent(false)
                if(!canceled) {
                    if(e.eventTime-down >= ViewConfiguration.getLongPressTimeout()) {
                        row.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); row.performLongClick()
                    } else if(selectionActive() || !clickLink(e.x,e.y)) row.performClick()
                }
            }
        }
        return true
    }
    private fun clickLink(x: Float,y: Float): Boolean {
        val text=body.text as? Spanned ?: return false
        val layout=body.layout ?: return false
        val px=x-body.totalPaddingLeft+body.scrollX; val py=y-body.totalPaddingTop+body.scrollY
        if(py<0 || py>layout.height) return false
        val line=layout.getLineForVertical(py.toInt())
        if(px<layout.getLineLeft(line) || px>layout.getLineRight(line)) return false
        val offset=layout.getOffsetForHorizontal(line,px)
        return text.getSpans(offset,offset,ClickableSpan::class.java).firstOrNull()?.let { it.onClick(body); true } ?: false
    }
    companion object {
        fun attach(body: TextView,row: View,selectionActive: () -> Boolean) { body.setTag(R.id.texto_selection_gesture,MessageSelectionGesture(body,row,selectionActive)) }
        fun bind(body: TextView,id: Long) { (body.getTag(R.id.texto_selection_gesture) as? MessageSelectionGesture)?.bind(id) }
    }
}
