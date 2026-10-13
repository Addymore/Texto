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
    private val slop=ViewConfiguration.get(body.context).scaledTouchSlop * 2
    private val selectText=Runnable { if(held && !canceled && body.isShown && body.hasWindowFocus() && !selectionActive()) beginTextSelection() }
    init {
        body.setTextIsSelectable(false)
        body.setOnTouchListener(this)
        body.setOnLongClickListener { row.performLongClick() }
        body.addOnAttachStateChangeListener(object: View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {}
            override fun onViewDetachedFromWindow(v: View) { reset() }
        })

    }
    fun bind(messageId: Long) { if(id != messageId) { reset(); id=messageId } }
    private fun reset() {
        held=false; canceled=true; body.removeCallbacks(selectText)
        body.parent?.requestDisallowInterceptTouchEvent(false)
    }
    private fun beginTextSelection() {
        val text = body.text.toString()
        val offset = body.getOffsetForPosition(x, y).coerceIn(0, text.length)
        reset()
        body.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        SelectMessageText.show(body.context, text, offset)
    }
    override fun onTouch(v: View, e: MotionEvent): Boolean {
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
