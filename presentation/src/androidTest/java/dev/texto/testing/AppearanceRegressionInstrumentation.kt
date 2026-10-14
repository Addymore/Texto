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
    private fun windows(): List<android.view.View> {
        fun flatten(view: android.view.View): List<android.view.View> = listOf(view) +
            if (view is android.view.ViewGroup) (0 until view.childCount).flatMap { flatten(view.getChildAt(it)) } else emptyList()
        return android.view.inspector.WindowInspector.getGlobalWindowViews().flatMap { flatten(it) }
    }
    private fun stage(name: String) { sendStatus(1, Bundle().apply { putString("stage", name) }) }
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
            stage("Open compose")
            val compose=startActivitySync(Intent(targetContext,ComposeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as ComposeActivity
            stage("Public SMS/MMS search")
            // Public search returns each matching SMS/MMS, never locked, blocked or trashed content.
            val searchIds = longArrayOf(991701,991702,991703,991704)
            try {
                Realm.getDefaultInstance().use { r -> r.executeTransaction { db ->
                    searchIds.forEachIndexed { index, id ->
                        val message = Message().apply { this.id=id; threadId=if(index==3) 991700L else 991700L+index; type=Message.TYPE_SMS; body="TextoSearch170 match $index"; date=id; if(index==3) trashedAt=1 }
                        db.insertOrUpdate(message)
                        if(index<3) db.insertOrUpdate(dev.octoshrimpy.quik.model.Conversation().apply {
                            this.id=message.threadId; lastMessage=message; textoLocked=index==1; blocked=index==2
                            recipients.add(dev.octoshrimpy.quik.model.Recipient().apply { this.id=id; address="+1555010$id" })
                        })
                    }
                    db.insertOrUpdate(Message().apply { id=991705; threadId=991700; type=Message.TYPE_MMS; date=991705
                        parts.add(dev.octoshrimpy.quik.model.MmsPart().apply { id=991705; text="TextoSearch170 MMS paragraph" }) })
                } }
                val matches=compose.conversationRepo.searchConversations("TextoSearch170").filter { it.messages>0 }
                check(matches.size==2 && matches.map { it.messageId }.toSet()==setOf(991701L,991705L)) { "Wrong public search matches: $matches" }
                check(matches.first().body.contains("MMS paragraph"))
                check(matches.all { it.date>0 && it.body.contains("TextoSearch170") })
            } finally {
                Realm.getDefaultInstance().use { r -> r.executeTransaction { db ->
                    db.where(Message::class.java).between("id",991701L,991705L).findAll().deleteAllFromRealm()
                    db.where(dev.octoshrimpy.quik.model.Conversation::class.java).between("id",991700L,991702L).findAll().deleteAllFromRealm()
                    db.where(dev.octoshrimpy.quik.model.Recipient::class.java).between("id",991701L,991705L).findAll().deleteAllFromRealm()
                    db.where(dev.octoshrimpy.quik.model.MmsPart::class.java).equalTo("id",991705L).findAll().deleteAllFromRealm()
                } }
            }
            stage("Archive rules")
            val policy=TextoPolicy(targetContext)
            val archivedBefore=policy.entries("archived");val lockedBefore=policy.entries("locked")
            val testNumber="+15550101633"
            try {
                Realm.getDefaultInstance().use { r -> r.executeTransaction { realm ->
                    realm.insertOrUpdate(dev.octoshrimpy.quik.model.Conversation().apply { id=99163;recipients.add(dev.octoshrimpy.quik.model.Recipient().apply { id=99163;address=testNumber }) })
                } }
                compose.conversationRepo.markArchived(99163)
                check(policy.decision(testNumber).archived)
                compose.conversationRepo.markUnarchived(listOf(99163))
                Realm.getDefaultInstance().use { r -> check(r.where(dev.octoshrimpy.quik.model.Conversation::class.java).equalTo("id",99163L).findFirst()!!.archived) }
                compose.conversationRepo.markUnarchived(listOf(99163),userInitiated=true)
                check(!policy.decision(testNumber).archived)
                policy.save("locked",lockedBefore+testNumber)
                compose.conversationRepo.markArchived(99163)
                compose.conversationRepo.markUnarchived(listOf(99163),userInitiated=true)
                check(policy.decision(testNumber).locked && policy.decision(testNumber).archived)
            } finally {
                policy.save("archived",archivedBefore);policy.save("locked",lockedBefore)
                Realm.getDefaultInstance().use { r -> r.executeTransaction { it.where(dev.octoshrimpy.quik.model.Conversation::class.java).equalTo("id",99163L).findAll().deleteAllFromRealm();it.where(dev.octoshrimpy.quik.model.Recipient::class.java).equalTo("id",99163L).findAll().deleteAllFromRealm() } }
            }
            stage("Bind actual message row")
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
            stage("Number action dialog")
            val dialMonitor=addMonitor(android.content.IntentFilter(Intent.ACTION_DIAL).apply { addDataScheme("tel") }, Instrumentation.ActivityResult(android.app.Activity.RESULT_CANCELED,null),true)
            main {
                val text=body.text as android.text.Spanned
                val span=text.getSpans(0,text.length,android.text.style.ClickableSpan::class.java).first {
                    text.subSequence(text.getSpanStart(it),text.getSpanEnd(it)).toString().contains("0722")
                }
                screen.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                span.onClick(body)
            }
            waitForIdleSync()
            main { windows().filterIsInstance<android.widget.ListView>().last().let {
                check((it.rootView.layoutParams as android.view.WindowManager.LayoutParams).flags and android.view.WindowManager.LayoutParams.FLAG_SECURE != 0)
                it.performItemClick(it.getChildAt(0), 0, it.adapter.getItemId(0))
                screen.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
            } }
            check(dialMonitor.hits==1) { "Confirmed message phone link did not open the dialer" }
            removeMonitor(dialMonitor)
            stage("Three-second selection")
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
            lateinit var selectedField: TextView
            main {
                selectedField = body
                check(body.isTextSelectable) { "Message body did not become selectable inline" }
                check(selectedField.selectionStart >= 0 && selectedField.selectionEnd > selectedField.selectionStart)
                check(selectedField.selectionEnd - selectedField.selectionStart < selectedField.text.length)
                check(selectedField !is android.widget.EditText)
                val up=MotionEvent.obtain(now,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,0f,0f,0)
                body.dispatchTouchEvent(up); up.recycle()
            }
            waitForIdleSync()
            SystemClock.sleep(500)
            main { check(windows().filterIsInstance<TextView>().any { it.isShown && it.text.toString() == "Copy" }) { "Native text selection toolbar did not open" } }
            uiAutomation.takeScreenshot()?.let { image ->
                java.io.File(targetContext.getExternalFilesDir(null),"word-selection-161.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
                image.recycle()
            }
            main {
                check(selectedField.selectionEnd > selectedField.selectionStart) { "Selection disappeared on finger release" }
                val selected=selectedField.text.subSequence(selectedField.selectionStart,selectedField.selectionEnd).toString()
                check(selectedField.onTextContextMenuItem(android.R.id.copy))
                val clip=screen.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                check(clip.primaryClip?.getItemAt(0)?.text.toString()==selected)
                android.text.Selection.setSelection(selectedField.text as android.text.Spannable,0,36)
                check(selectedField.onTextContextMenuItem(android.R.id.copy))
                check(clip.primaryClip?.getItemAt(0)?.text.toString() == selectedField.text.substring(0,36))
                selectedField.clearFocus()
            }
            waitForIdleSync()
            stage("Recycled links and surfaces")
            main {
                val holder=checkNotNull(list.findViewHolderForAdapterPosition(0)) as dev.octoshrimpy.quik.common.base.QkViewHolder
                val cached=body.getTag(R.id.texto_message_links)
                messages.onBindViewHolder(holder,0)
                check(body.getTag(R.id.texto_message_links) === cached) { "Unchanged text reran link detection" }
                realm!!.executeTransaction { r -> r.where(Message::class.java).findFirst()!!.body="Call 0722 000 999 instead." }
                messages.onBindViewHolder(holder,0)
                val updated=body.text as android.text.Spanned
                val links=updated.getSpans(0,updated.length,android.text.style.ClickableSpan::class.java)
                check(links.any { updated.subSequence(updated.getSpanStart(it),updated.getSpanEnd(it)).toString().contains("999") }) { "Edited message retained stale phone link" }
                realm!!.executeTransaction { r -> r.where(Message::class.java).findFirst()!!.body="No phone or web links here." }
                messages.onBindViewHolder(holder,0)
                val plain=body.text as android.text.Spanned
                check(plain.getSpans(0,plain.length,android.text.style.ClickableSpan::class.java).isEmpty()) { "Recycled plain text retained a link" }
                prefs.edit().putString("card_finish","none").commit()
                val button=com.google.android.material.button.MaterialButton(screen)
                TextoAppearance.styleSettingsCard(button)
                val bitmap=android.graphics.Bitmap.createBitmap(100,100,android.graphics.Bitmap.Config.ARGB_8888)
                button.background.setBounds(0,0,100,100); button.background.draw(android.graphics.Canvas(bitmap))
                check(android.graphics.Color.alpha(bitmap.getPixel(50,50))==0) { "No-cards settings still have a fill" }
                bitmap.recycle()
                MessageBodyStyle.apply(body,false,TextoAppearance.accent(screen),false)
                check(body.backgroundTintList == null && TextoAppearance.noCards(screen))
                for (finish in listOf("tonal","gradient","glass","amoled","outlined")) {
                    prefs.edit().putString("card_finish",finish).commit()
                    check(TextoAppearance.card(screen) is android.graphics.drawable.RippleDrawable)
                }
            }
            stage("Protected dialog lifecycle")
            main {
                screen.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
                dev.texto.privacy.SelectMessageText.show(screen, "Protected selection fixture")
            }
            waitForIdleSync()
            main {
                val field=windows().filterIsInstance<android.widget.EditText>().single { it.text.toString() == "Protected selection fixture" }
                check((field.rootView.layoutParams as android.view.WindowManager.LayoutParams).flags and android.view.WindowManager.LayoutParams.FLAG_SECURE != 0)
            }
            val next = startActivitySync(Intent(targetContext, dev.texto.privacy.ThemesActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            waitForIdleSync()
            main {
                check(windows().filterIsInstance<android.widget.EditText>().none { it.isShown && it.text.toString() == "Protected selection fixture" }) { "Protected selection dialog remained visible after its screen paused" }
                screen.finish(); next.finish()
            }
            result.putString("result","PASS: individual SMS/MMS search results exclude locked, blocked and trashed messages; persistent archive survives automatic receive/send unarchive, explicit unarchive works and locked rules remain protected; real message adapter, TightTextView and RecyclerView select and copy an individual word only after three seconds; displayed phone numbers and optional country code; no-card settings and message bubbles; all new card finishes; secure selection dialog closes when its Activity pauses")
        } catch(e: Throwable) { result.putString("error",e.stackTraceToString()) }
        finally {
            main { adapter?.updateData(null); realm?.close(); activity?.finish() }
            prefs.edit().putString("card_finish",previous).putString("dial_country_code",code).commit()
        }
        finish(if(result.containsKey("error")) 1 else 0,result)
    }
}
