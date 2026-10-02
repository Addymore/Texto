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
            val home = startActivitySync(targetContext.packageManager.getLaunchIntentForPackage(targetContext.packageName)!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            for (name in listOf("backup.BackupActivity", "messageutils.MessageUtilsActivity", "scheduled.ScheduledActivity")) {
                val intent = Intent().setClassName(targetContext, "dev.octoshrimpy.quik.feature.$name").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                main { PrivacyGate.session.lock() }
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
            val canceled = startActivitySync(Intent().setClassName(targetContext, "dev.octoshrimpy.quik.feature.backup.BackupActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            val unlock = checkNotNull(waitForMonitorWithTimeout(monitor, 8000))
            main { unlock.setResult(Activity.RESULT_CANCELED); unlock.finish() }
            waitForIdleSync()
            main { check(canceled.isFinishing); check(!PrivacyGate.unlocked); home.finish() }
            removeMonitor(monitor)
            result.putString("result", "PASS: Backup, Message management and Scheduled request PIN, stay hidden until verified, resume successfully; cancellation closes utility without exposing messages")
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
