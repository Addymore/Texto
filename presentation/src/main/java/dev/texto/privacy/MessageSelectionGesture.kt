package dev.texto.privacy

import android.text.Selection
import android.text.Spannable
import android.view.*
import android.widget.TextView
import dev.octoshrimpy.quik.R

/** QUIK's native selectable TextView: Android owns long-press timing and handle gestures. */
class MessageSelectionGesture(private val body: TextView, private val row: View, private val selectionActive: () -> Boolean) : View.OnTouchListener {
    private var id=Long.MIN_VALUE
    private var actionMode: ActionMode?=null
    private var downX=0f
    private var downY=0f
    private var linkTap=false
    private val touchSlop=ViewConfiguration.get(body.context).scaledTouchSlop
    init {
        body.setOnLongClickListener(null)
        body.setTextIsSelectable(true)
        body.movementMethod=android.text.method.ArrowKeyMovementMethod.getInstance()
        body.setOnTouchListener(this)
        body.customSelectionActionModeCallback=object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode,menu: Menu): Boolean {
                actionMode=mode
                menu.add(Menu.NONE,R.id.texto_selection_gesture,Menu.NONE,"Select message")
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
                return true
            }
            override fun onPrepareActionMode(mode: ActionMode,menu: Menu)=false
            override fun onActionItemClicked(mode: ActionMode,item: MenuItem): Boolean {
                if(item.itemId!=R.id.texto_selection_gesture) return false
                mode.finish(); body.clearFocus(); row.performLongClick(); return true
            }
            override fun onDestroyActionMode(mode: ActionMode) { if(actionMode===mode) actionMode=null }
        }
        body.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {}
            override fun onViewDetachedFromWindow(v: View) { clearSelection() }
        })
    }
    private fun clearSelection() {
        actionMode?.finish(); actionMode=null
        body.clearFocus()
        (body.text as? Spannable)?.let(Selection::removeSelection)
    }
    fun bind(messageId: Long) { if(id!=messageId) { clearSelection(); id=messageId } }
    override fun onTouch(v: View,event: MotionEvent): Boolean {
        // Only route taps to message multi-select when that explicit mode is already active.
        // Otherwise do not consume DOWN, MOVE, UP, or delay RecyclerView interception.
        if(!selectionActive()) {
            // ArrowKeyMovementMethod preserves native selection. LinkMovementMethod
            // does not support arbitrary ranges and clears them on touch release.
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX=event.x; downY=event.y
                    linkTap=actionMode==null
                }
                MotionEvent.ACTION_MOVE -> if(kotlin.math.abs(event.x-downX)>touchSlop || kotlin.math.abs(event.y-downY)>touchSlop) linkTap=false
                MotionEvent.ACTION_CANCEL -> linkTap=false
                MotionEvent.ACTION_UP -> {
                    val click=linkTap && event.eventTime-event.downTime<ViewConfiguration.getLongPressTimeout()
                    linkTap=false
                    if(click && actionMode==null && body.linksClickable) {
                        val text=body.text as? android.text.Spanned
                        val layout=body.layout
                        val x=event.x-body.totalPaddingLeft+body.scrollX
                        val y=event.y-body.totalPaddingTop+body.scrollY
                        if(text!=null && layout!=null && y>=0 && y<layout.height) {
                            val line=layout.getLineForVertical(y.toInt())
                            if(x>=layout.getLineLeft(line) && x<=layout.getLineRight(line)) {
                                val offset=layout.getOffsetForHorizontal(line,x)
                                text.getSpans(offset,offset,android.text.style.ClickableSpan::class.java).firstOrNull()?.let {
                                    it.onClick(body); return true
                                }
                            }
                        }
                    }
                }
            }
            return false
        }
        if(event.actionMasked==MotionEvent.ACTION_UP) row.performClick()
        return true
    }
    companion object {
        fun attach(body: TextView,row: View,selectionActive: () -> Boolean) { body.setTag(R.id.texto_selection_gesture,MessageSelectionGesture(body,row,selectionActive)) }
        fun bind(body: TextView,id: Long) { (body.getTag(R.id.texto_selection_gesture) as? MessageSelectionGesture)?.bind(id) }
    }
}
