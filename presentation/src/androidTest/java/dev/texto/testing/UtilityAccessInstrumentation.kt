package dev.texto.testing

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import dev.texto.privacy.PrivacyGate
import dev.texto.privacy.TextoPolicy
import dev.texto.privacy.UnlockActivity

/** Exercises the real utility/PIN activity round trip on synthetic emulator data only. */
class UtilityAccessInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    private fun main(block: () -> Unit) {
        var error: Throwable? = null
        runOnMainSync { try { block() } catch (e: Throwable) { error = e } }
        error?.let { throw it }
    }
    private fun views(view: View): List<View> = listOf(view) +
        if (view is ViewGroup) (0 until view.childCount).flatMap { views(view.getChildAt(it)) } else emptyList()
    override fun onStart() {
        val result = Bundle()
        val policy = TextoPolicy(targetContext)
        val before = policy.preferences.all
        try {
            check(android.os.Build.MODEL.contains("sdk", true)) { "Synthetic emulator only" }
            policy.setPin("123456")
            policy.save("locked", listOf("+15550101999"))
            policy.save("protected_tools",listOf("+15550101888"))
            io.realm.Realm.getInstance(io.realm.RealmConfiguration.Builder().name("tool-scope-test").inMemory().build()).use { realm ->
                realm.executeTransaction {
                    it.insert(dev.octoshrimpy.quik.model.Conversation().apply { id=99001; archived=true })
                    it.insert(dev.octoshrimpy.quik.model.Conversation().apply { id=99002; textoLocked=true })
                }
                val ordinary=dev.texto.privacy.ToolScope(targetContext,realm,false)
                val protected=dev.texto.privacy.ToolScope(targetContext,realm,true)
                for((thread,address,expected) in listOf(Triple(1L,"+15550101777",true),Triple(1L,"+15550101999",false),Triple(1L,"+15550101888",false),Triple(99001L,"+15550101777",false),Triple(99002L,"+15550101777",false))) {
                    check(ordinary.accepts(thread,listOf(address))==expected)
                    check(protected.accepts(thread,listOf(address))!=expected)
                }
                check(!ordinary.accepts(1L,listOf("+15550101777","+15550101999")))
            }
            val art=java.io.File(targetContext.filesDir,"test-art-160.png")
            val bitmap=android.graphics.Bitmap.createBitmap(360,640,android.graphics.Bitmap.Config.ARGB_8888)
            val canvas=android.graphics.Canvas(bitmap)
            canvas.drawColor(0xFF007E70.toInt())
            canvas.drawCircle(260f,130f,180f,android.graphics.Paint().apply { color=0xFF90D5C6.toInt() })
            art.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }; bitmap.recycle()
            policy.preferences.edit().putString("background_image",android.net.Uri.fromFile(art).toString()).putString("header_image",android.net.Uri.fromFile(art).toString()).commit()
            policy.preferences.edit().putString("inbox_title","My inbox").putString("inbox_motto","Small moments matter").commit()

            val home = startActivitySync(targetContext.packageManager.getLaunchIntentForPackage(targetContext.packageName)!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            android.os.SystemClock.sleep(2000)
            main {
                check(home.findViewById<View>(dev.octoshrimpy.quik.R.id.textoHeader).background is android.graphics.drawable.LayerDrawable) { "Header art failed to load" }
                check(home.findViewById<View>(dev.octoshrimpy.quik.R.id.textoReachable).background is android.graphics.drawable.LayerDrawable) { "Background failed to load" }
                val texts=views(home.window.decorView).filterIsInstance<TextView>().map { it.text.toString() }
                check("My inbox" in texts && "Small moments matter" in texts)
                val sample=dev.octoshrimpy.quik.databinding.ConversationListItemBinding.inflate(home.layoutInflater)
                policy.preferences.edit().putString("card_size","small").commit()
                dev.texto.privacy.TextoAppearance.styleConversation(sample,5,2)
                val small=sample.root.minimumHeight
                policy.preferences.edit().putString("card_size","large").commit()
                dev.texto.privacy.TextoAppearance.styleConversation(sample,5,2)
                check(sample.root.minimumHeight>small)
                policy.preferences.edit().putString("card_finish","none").commit()
                dev.texto.privacy.TextoAppearance.styleConversation(sample,5,2)
                check(((sample.root.background as android.graphics.drawable.RippleDrawable).getDrawable(0) as android.graphics.drawable.ColorDrawable).color==android.graphics.Color.TRANSPARENT)
                policy.preferences.edit().remove("card_size").remove("card_finish").commit()
            }
            uiAutomation.takeScreenshot()?.let { image -> java.io.File(targetContext.getExternalFilesDir(null),"inbox-160.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }; image.recycle() }
            main { home.findViewById<androidx.drawerlayout.widget.DrawerLayout>(dev.octoshrimpy.quik.R.id.drawerLayout).openDrawer(android.view.Gravity.START,false) }
            waitForIdleSync()
            uiAutomation.takeScreenshot()?.let { image -> java.io.File(targetContext.getExternalFilesDir(null),"drawer-160.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }; image.recycle() }
            main { home.findViewById<androidx.drawerlayout.widget.DrawerLayout>(dev.octoshrimpy.quik.R.id.drawerLayout).closeDrawers() }
            lateinit var gestureBody: TextView
            lateinit var gestureRow: android.widget.FrameLayout
            var selectedMessages=0
            main {
                gestureRow=android.widget.FrameLayout(home)
                gestureBody=TextView(home).apply { text="Select these words inside the message"; textSize=20f; setPadding(24,24,24,24) }
                gestureRow.addView(gestureBody,android.widget.FrameLayout.LayoutParams(-1,180))
                gestureRow.setOnLongClickListener { selectedMessages++; true }
                home.addContentView(gestureRow,android.view.ViewGroup.LayoutParams(-1,180))
                dev.texto.privacy.MessageSelectionGesture.attach(gestureBody,gestureRow) { false }
                dev.texto.privacy.MessageSelectionGesture.bind(gestureBody,1L)
            }
            waitForIdleSync()
            fun touch(kind: Int,whenMs: Long,x: Float=40f) { main {
                val event=android.view.MotionEvent.obtain(whenMs,whenMs,kind,x,45f,0)
                gestureBody.dispatchTouchEvent(event); event.recycle()
            } }
            var now=android.os.SystemClock.uptimeMillis()
            touch(android.view.MotionEvent.ACTION_DOWN,now)
            touch(android.view.MotionEvent.ACTION_UP,now+700)
            main { check(selectedMessages==1 && !gestureBody.isTextSelectable) { "Normal hold must select message only" } }
            now=android.os.SystemClock.uptimeMillis()
            touch(android.view.MotionEvent.ACTION_DOWN,now)
            android.os.SystemClock.sleep(1500)
            main { check(!gestureBody.isTextSelectable) { "Text selection opened before three seconds" } }
            android.os.SystemClock.sleep(1800)
            waitForIdleSync()
            main { check(gestureBody.isTextSelectable && gestureBody.selectionStart>=0 && gestureBody.selectionEnd>gestureBody.selectionStart) { "Three-second hold did not start native text selection: selectable=${gestureBody.isTextSelectable}, selection=${gestureBody.selectionStart}..${gestureBody.selectionEnd}, shown=${gestureBody.isShown}, windowFocus=${gestureBody.hasWindowFocus()}, focus=${gestureBody.hasFocus()}, size=${gestureBody.width}x${gestureBody.height}" } }
            touch(android.view.MotionEvent.ACTION_UP,android.os.SystemClock.uptimeMillis())
            waitForIdleSync()
            uiAutomation.takeScreenshot()?.let { image -> java.io.File(targetContext.getExternalFilesDir(null),"text-selection-160.png").outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }; image.recycle() }
            main { dev.texto.privacy.MessageSelectionGesture.bind(gestureBody,2L); check(!gestureBody.isTextSelectable) }
            now=android.os.SystemClock.uptimeMillis()
            touch(android.view.MotionEvent.ACTION_DOWN,now)
            touch(android.view.MotionEvent.ACTION_MOVE,now+100,200f)
            android.os.SystemClock.sleep(3200)
            main { check(!gestureBody.isTextSelectable) { "Scrolling should cancel text selection" }; (gestureRow.parent as ViewGroup).removeView(gestureRow) }
            for (name in listOf("backup.BackupActivity", "messageutils.MessageUtilsActivity", "scheduled.ScheduledActivity")) {
                val intent = Intent().setClassName(targetContext, "dev.octoshrimpy.quik.feature.$name").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                main { PrivacyGate.session.lock() }
                val ordinary = startActivitySync(intent)
                waitForIdleSync()
                main { check(!ordinary.isFinishing && ordinary.window.decorView.visibility == View.VISIBLE); check(!PrivacyGate.session.pending); ordinary.finish() }
                waitForIdleSync()
                intent.putExtra("protected_tools",true)
                val monitor = addMonitor(UnlockActivity::class.java.name, null, false)
                val utility = startActivitySync(intent)
                val unlock = checkNotNull(waitForMonitorWithTimeout(monitor, 8000)) { "$name did not request authentication" }
                waitForIdleSync()
                android.os.SystemClock.sleep(700)
                uiAutomation.rootInActiveWindow?.findAccessibilityNodeInfosByText("Use PIN")?.firstOrNull()
                    ?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
                waitForIdleSync()
                main {
                    check(!utility.isFinishing)
                    check(utility.window.decorView.visibility == View.INVISIBLE)
                    check(utility.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
                    val children = views(unlock.window.decorView)
                    children.filterIsInstance<TextInputEditText>().single().setText("123456")
                    children.filterIsInstance<TextView>().single { it.text.toString() == "Unlock" }.performClick()
                }
                for (i in 0..99) {
                    waitForIdleSync()
                    var ready = false
                    main { ready = PrivacyGate.unlocked && utility.window.decorView.visibility == View.VISIBLE }
                    if (ready) break
                    android.os.SystemClock.sleep(100)
                }
                main { check(PrivacyGate.unlocked && !utility.isFinishing && utility.window.decorView.visibility == View.VISIBLE) { "$name did not resume after PIN: unlocked=${PrivacyGate.unlocked}, pending=${PrivacyGate.session.pending}, finishing=${utility.isFinishing}, visibility=${utility.window.decorView.visibility}, unlockFinishing=${unlock.isFinishing}, text=${views(unlock.window.decorView).filterIsInstance<TextView>().map { it.text.toString() }}" } }
                removeMonitor(monitor)
                main { utility.finish() }
                waitForIdleSync()
            }
            main { PrivacyGate.session.lock() }
            val monitor = addMonitor(UnlockActivity::class.java.name, null, false)
            val canceled = startActivitySync(Intent().setClassName(targetContext, "dev.octoshrimpy.quik.feature.backup.BackupActivity").putExtra("protected_tools",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            val unlock = checkNotNull(waitForMonitorWithTimeout(monitor, 8000))
            main { unlock.setResult(Activity.RESULT_CANCELED); unlock.finish() }
            waitForIdleSync()
            main { check(canceled.isFinishing); check(!PrivacyGate.unlocked); home.finish() }
            removeMonitor(monitor)
            result.putString("result", "PASS: three-second native text selection, shorter message hold and scroll cancellation; protected-number scope, mixed groups, archived/locked threads, custom title/motto, loaded background/header artwork, card size and no cards; ordinary Backup, Message management and Scheduled open without PIN; protected tools remain hidden until PIN verification; cancellation closes protected utility")
            finish(0, result)
        } catch (error: Throwable) {
            result.putString("error", error.stackTraceToString()); finish(1, result)
        } finally {
            val editor = policy.preferences.edit().clear()
            before.forEach { (key, value) -> when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            } }
            editor.commit()
        }
    }
}
