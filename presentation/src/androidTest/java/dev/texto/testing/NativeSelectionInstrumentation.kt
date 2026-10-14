package dev.texto.testing

import android.app.Instrumentation
import android.content.Intent
import android.os.*
import android.view.*
import android.widget.TextView

/** Safe on a connected phone: debug-only synthetic screen, no provider or preference writes. */
class NativeSelectionInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    private fun main(block: () -> Unit) {
        var error: Throwable?=null
        runOnMainSync { try { block() } catch(e: Throwable) { error=e } }
        error?.let { throw it }
    }
    override fun onStart() {
        val result=Bundle()
        var activity: android.app.Activity?=null
        var clipboard: android.content.ClipboardManager?=null
        var savedClip: android.content.ClipData?=null
        try {
            check(targetContext.packageName=="app.texto.sms.debug")
            activity=startActivitySync(Intent().setClassName(targetContext,"dev.texto.testing.SelectionProbeActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            val host=activity!!
            main { host.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            var focused=false
            for(attempt in 0..149) {
                waitForIdleSync()
                main { focused=host.window.decorView.hasWindowFocus() }
                if(focused) break
                SystemClock.sleep(100)
            }
            check(focused) { "Synthetic test screen did not receive input focus" }
            SystemClock.sleep(500)
            val list=host.findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0) as androidx.recyclerview.widget.RecyclerView
            lateinit var body: TextView
            var x=0f; var y=0f
            main {
                val holder=checkNotNull(list.findViewHolderForAdapterPosition(0))
                body=(holder.itemView as android.view.ViewGroup).getChildAt(0) as TextView
                check(body.isTextSelectable)
                val pos=IntArray(2); body.getLocationOnScreen(pos)
                x=pos[0]+body.totalPaddingLeft+body.layout.getPrimaryHorizontal(9)
                y=pos[1]+body.totalPaddingTop+body.layout.getLineBottom(0)/2f
                clipboard=host.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                savedClip=clipboard!!.primaryClip
            }
            var start=SystemClock.uptimeMillis()
            fun touch(action: Int,px: Float=x,py: Float=y) {
                val pointer=MotionEvent.PointerProperties().apply { id=0; toolType=MotionEvent.TOOL_TYPE_FINGER }
                val coords=MotionEvent.PointerCoords().apply { this.x=px; this.y=py; pressure=1f; size=1f }
                val e=MotionEvent.obtain(start,SystemClock.uptimeMillis(),action,1,arrayOf(pointer),arrayOf(coords),0,0,1f,1f,0,0,InputDevice.SOURCE_TOUCHSCREEN,0)
                check(uiAutomation.injectInputEvent(e,true)); e.recycle()
            }
            touch(MotionEvent.ACTION_DOWN)
            SystemClock.sleep(ViewConfiguration.getLongPressTimeout().toLong()+350)
            var beforeUp=""
            main { beforeUp="range=${body.selectionStart}:${body.selectionEnd}, focus=${body.hasFocus()}, window=${body.hasWindowFocus()}, longClickable=${body.isLongClickable}, movement=${body.movementMethod}, xy=$x,$y" }
            touch(MotionEvent.ACTION_UP); waitForIdleSync(); SystemClock.sleep(300)
            val service=uiAutomation.serviceInfo
            service.flags=service.flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            uiAutomation.serviceInfo=service
            var copyNode: android.view.accessibility.AccessibilityNodeInfo?=null
            for(attempt in 0..49) {
                copyNode=uiAutomation.windows.asSequence().mapNotNull { it.root }
                    .flatMap { it.findAccessibilityNodeInfosByText("Copy").asSequence() }
                    .firstOrNull { it.text?.toString()=="Copy" }
                if(copyNode!=null) break
                SystemClock.sleep(100)
            }
            uiAutomation.takeScreenshot()?.let { image ->
                java.io.File(targetContext.filesDir,"native-selection.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }; image.recycle()
            }
            var selected=""
            main {
                check(body.selectionStart>=0 && body.selectionEnd>body.selectionStart) { "Native long press did not select text. Before UP: $beforeUp; after=${body.selectionStart}:${body.selectionEnd}" }
                check(body.selectionEnd-body.selectionStart<body.text.length) { "Only whole-message selection was available" }
                selected=body.text.subSequence(body.selectionStart,body.selectionEnd).toString()
            }
            checkNotNull(copyNode) { "Native Copy toolbar did not appear" }
            val copyBounds=android.graphics.Rect()
            copyNode!!.getBoundsInScreen(copyBounds)
            start=SystemClock.uptimeMillis()
            touch(MotionEvent.ACTION_DOWN,copyBounds.exactCenterX(),copyBounds.exactCenterY())
            SystemClock.sleep(60)
            touch(MotionEvent.ACTION_UP,copyBounds.exactCenterX(),copyBounds.exactCenterY())
            waitForIdleSync()
            main {
                check(clipboard!!.primaryClip?.getItemAt(0)?.text.toString()==selected) { "Copy did not match selected word" }
                body.clearFocus()
            }
            main {
                val pos=IntArray(2); body.getLocationOnScreen(pos)
                x=pos[0]+body.totalPaddingLeft+body.layout.getPrimaryHorizontal(20)
                y=pos[1]+body.totalPaddingTop+body.layout.getLineBottom(0)/2f
            }
            start=SystemClock.uptimeMillis()
            touch(MotionEvent.ACTION_DOWN); SystemClock.sleep(60); touch(MotionEvent.ACTION_UP)
            waitForIdleSync()
            main { check(host.javaClass.getMethod("getClickedLinks").invoke(host)==1) { "Short tap did not activate the message link" } }
            start=SystemClock.uptimeMillis()
            val top=IntArray(2); main { list.getLocationOnScreen(top) }
            val sx=list.width/2f; val sy=top[1]+list.height*.75f
            touch(MotionEvent.ACTION_DOWN,sx,sy)
            for(step in 1..8) { SystemClock.sleep(16); touch(MotionEvent.ACTION_MOVE,sx,sy-step*list.height*.05f) }
            touch(MotionEvent.ACTION_UP,sx,sy-list.height*.4f); SystemClock.sleep(400); waitForIdleSync()
            main { check(list.computeVerticalScrollOffset()>0) { "Message-body touches blocked list scrolling" } }
            result.putString("result","PASS: native word selection and Copy through system touch input; short link tap; message-body swipe scrolls; synthetic debug screen only; Android "+Build.VERSION.RELEASE+" API "+Build.VERSION.SDK_INT)
        } catch(e: Throwable) { result.putString("error",e.stackTraceToString()) }
        finally { main { clipboard?.let { if(savedClip!=null) it.setPrimaryClip(savedClip!!) else if(Build.VERSION.SDK_INT>=28) it.clearPrimaryClip() }; activity?.finish() } }
        finish(if(result.containsKey("error")) 1 else 0,result)
    }
}
