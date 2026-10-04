package dev.texto.testing

import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.octoshrimpy.quik.R
import dev.octoshrimpy.quik.feature.compose.ComposeActivity
import dev.octoshrimpy.quik.model.Message
import dev.texto.privacy.*
import io.realm.Realm

/** Uses actual adapter binding, TightTextView and RecyclerView; never runs on a real phone. */
class AppearanceRegressionInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    private fun main(block: () -> Unit) {
        var error: Throwable? = null
        runOnMainSync { try { block() } catch (e: Throwable) { error=e } }
        error?.let { throw it }
    }
    override fun onStart() {
        val result=Bundle()
        var realm: Realm?=null
        var activity: android.app.Activity?=null
        var adapter: dev.octoshrimpy.quik.feature.compose.MessagesAdapter?=null
        val prefs=TextoAppearance.prefs(targetContext)
        val previous=prefs.getString("card_finish", "tonal")
        val code=prefs.getString("dial_country_code", "")
        try {
            check(android.os.Build.MODEL.contains("sdk",true)) { "Synthetic emulator only" }
            check(PhoneDialing.number("0722 000 123")=="0722000123")
            check(PhoneDialing.number("0722 000 123", "+250")=="+250722000123")
            check(PhoneDialing.number("+44 7700 900123", "+250")=="+447700900123")
            check(PhoneDialing.number("112", "+250")=="112")
            check(PhoneDialing.number("*182#", "+250")=="*182#")
            prefs.edit().putString("dial_country_code", "").commit()
            val dial=PhoneDialing.intent(targetContext,"0722 000 123")
            check(dial.action==Intent.ACTION_DIAL && dial.data?.schemeSpecificPart=="0722000123")
            val compose=startActivitySync(Intent(targetContext,ComposeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as ComposeActivity
            adapter=compose.messageAdapter
            // Dispose the live compose model before binding the isolated fixture. Otherwise
            // its asynchronous empty-thread result can overwrite the fixture's adapter data.
            main { compose.finish() }
            waitForIdleSync()
            activity=startActivitySync(targetContext.packageManager.getLaunchIntentForPackage(targetContext.packageName)!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            val screen=requireNotNull(activity)
            val messages=requireNotNull(adapter)
            SystemClock.sleep(1000)
            lateinit var list: RecyclerView
            main {
                realm=Realm.getInstance(io.realm.RealmConfiguration.Builder().name("appearance-161").inMemory().allowWritesOnUiThread(true).build())
                realm!!.executeTransaction { r -> r.insert(Message().apply {
                    id=99161; threadId=99161; type=Message.TYPE_SMS; boxId=1; date=System.currentTimeMillis()
                    address="+15550101777"; body="Select these words inside the message. Call 0722 000 123 tomorrow."
                }) }
                list=RecyclerView(screen).apply { layoutManager=LinearLayoutManager(screen); this.adapter=messages }
                screen.setContentView(list)
                messages.updateData(realm!!.where(Message::class.java).findAll())
            }
            waitForIdleSync(); SystemClock.sleep(500)
            lateinit var body: TextView
            main { body=checkNotNull(list.findViewHolderForAdapterPosition(0)) {
                    "Fixture not laid out: adapter=${list.adapter}, count=${messages.itemCount}, size=${list.width}x${list.height}"
                }.itemView.findViewById(R.id.body)
                check(body.javaClass.simpleName=="TightTextView")
                check(body.autoLinkMask==0)
                check(body.text is android.text.Spanned)
            }
            val dialMonitor=addMonitor(android.content.IntentFilter(Intent.ACTION_DIAL).apply { addDataScheme("tel") }, Instrumentation.ActivityResult(android.app.Activity.RESULT_CANCELED,null),true)
            main {
                val text=body.text as android.text.Spanned
                val span=text.getSpans(0,text.length,android.text.style.ClickableSpan::class.java).first {
                    text.subSequence(text.getSpanStart(it),text.getSpanEnd(it)).toString().contains("0722")
                }
                span.onClick(body)
            }
            check(dialMonitor.hits==1) { "Actual message phone link did not open the dialer directly" }
            removeMonitor(dialMonitor)
            val now=SystemClock.uptimeMillis()
            main {
                val x=body.totalPaddingLeft+body.layout.getPrimaryHorizontal(9)
                val y=body.totalPaddingTop+body.layout.getLineBottom(0)/2f
                val down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,x,y,0)
                body.dispatchTouchEvent(down); down.recycle()
            }
            SystemClock.sleep(1600)
            main { check(!body.isTextSelectable) { "Selection started too early" } }
            SystemClock.sleep(1800); waitForIdleSync()
            main {
                check(body.isTextSelectable && body.selectionStart>=0 && body.selectionEnd>body.selectionStart) { "Selection not ready: selectable=${body.isTextSelectable}, range=${body.selectionStart}..${body.selectionEnd}, focus=${body.hasFocus()}, window=${body.hasWindowFocus()}, shown=${body.isShown}, movement=${body.movementMethod}" }
                check(body.selectionEnd-body.selectionStart<body.text.length) { "Whole message selected instead of a word" }
                check(body.movementMethod is android.text.method.ArrowKeyMovementMethod)
                val up=MotionEvent.obtain(now,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,
                    body.totalPaddingLeft+body.layout.getPrimaryHorizontal(9), body.totalPaddingTop+body.layout.getLineBottom(0)/2f,0)
                body.dispatchTouchEvent(up); up.recycle()
            }
            waitForIdleSync()
            uiAutomation.takeScreenshot()?.let { image ->
                java.io.File(targetContext.getExternalFilesDir(null),"word-selection-161.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
                image.recycle()
            }
            main {
                check(body.selectionStart>=0 && body.selectionEnd>body.selectionStart) { "Word selection disappeared on finger release" }
                val selected=body.text.subSequence(body.selectionStart,body.selectionEnd).toString()
                check(body.onTextContextMenuItem(android.R.id.copy))
                val clip=screen.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                check(clip.primaryClip?.getItemAt(0)?.text.toString()==selected) { "Copy did not preserve the selected word" }
            }
            waitForIdleSync()
            main {
                prefs.edit().putString("card_finish","none").commit()
                val button=com.google.android.material.button.MaterialButton(screen)
                TextoAppearance.styleSettingsCard(button)
                val bitmap=android.graphics.Bitmap.createBitmap(100,100,android.graphics.Bitmap.Config.ARGB_8888)
                button.background.setBounds(0,0,100,100); button.background.draw(android.graphics.Canvas(bitmap))
                check(android.graphics.Color.alpha(bitmap.getPixel(50,50))==0) { "No-cards settings still have a fill" }
                bitmap.recycle()
                MessageBodyStyle.apply(body,false,TextoAppearance.accent(screen),false)
                check(body.backgroundTintList?.defaultColor==android.graphics.Color.TRANSPARENT)
                for (finish in listOf("tonal","gradient","glass","amoled","outlined")) {
                    prefs.edit().putString("card_finish",finish).commit()
                    check(TextoAppearance.card(screen) is android.graphics.drawable.RippleDrawable)
                }
            }
            result.putString("result","PASS: real message adapter, TightTextView and RecyclerView select and copy an individual word only after three seconds; displayed phone numbers and optional country code; no-card settings and message bubbles; all new card finishes")
        } catch(e: Throwable) { result.putString("error",e.stackTraceToString()) }
        finally {
            main { adapter?.updateData(null); realm?.close(); activity?.finish() }
            prefs.edit().putString("card_finish",previous).putString("dial_country_code",code).commit()
        }
        finish(if(result.containsKey("error")) 1 else 0,result)
    }
}
