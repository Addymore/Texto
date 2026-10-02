package dev.texto.privacy

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import dev.octoshrimpy.quik.feature.main.MainActivity
import dev.octoshrimpy.quik.model.Conversation
import dev.octoshrimpy.quik.model.MmsPart
import io.realm.Realm

object PrivacyGate : Application.ActivityLifecycleCallbacks {
    const val UTILITY_UNLOCK = 4812
    private fun isUtility(a: Activity) = a.javaClass.simpleName in setOf("BackupActivity", "MessageUtilsActivity", "ScheduledActivity")
    val session = VaultSession()
    val unlocked get() = session.unlocked
    private var started = 0
    fun pullDown(activity: MainActivity) {
        if (!TextoPolicy(activity).hasPin()) {
            Toast.makeText(activity, "Create your privacy PIN in Settings → Privacy & protection first.", Toast.LENGTH_LONG).show()
            return
        }
        session.request()
        activity.startActivity(Intent(activity, UnlockActivity::class.java))
    }
    fun openBin(activity: Activity) {
        if (!TextoPolicy(activity).hasPin()) {
            Toast.makeText(activity, "Create your privacy PIN first, then open Recycle bin.", Toast.LENGTH_LONG).show()
            activity.startActivity(Intent(activity, ProtectionActivity::class.java)); return
        }
        session.request()
        activity.startActivity(Intent(activity, UnlockActivity::class.java).putExtra("bin", true))
    }
    fun isLocked(activity: Activity, threadId: Long): Boolean = Realm.getDefaultInstance().use { realm ->
        realm.where(Conversation::class.java).equalTo("id", threadId).findFirst()?.recipients?.any {
            TextoPolicy(activity).decision(it.address).locked
        } == true
    }
    fun guardConversation(activity: Activity, threadId: Long): Boolean {
        if (unlocked || !isLocked(activity, threadId)) return true
        deny(activity)
        return false
    }
    fun requiresSecureWindow(a: Activity) = protected(a)
    private fun protected(a: Activity): Boolean {
        if (a is VaultActivity || a is TrashActivity) return true
        if (a is UnlockActivity || a is MainActivity) return false
        val policy = TextoPolicy(a)
        val uri = a.intent?.data
        if (uri?.scheme in listOf("sms", "smsto", "mms", "mmsto") &&
            uri?.schemeSpecificPart?.substringBefore('?')?.split(',', ';')?.any { policy.decision(it).locked } == true) return true
        if (isLocked(a, a.intent?.getLongExtra("threadId", 0) ?: 0)) return true
        val partId = a.intent?.getLongExtra("partId", 0) ?: 0
        if (partId != 0L && Realm.getDefaultInstance().use { realm ->
                realm.where(MmsPart::class.java).equalTo("id", partId).findFirst()?.messages?.any { isLocked(a, it.threadId) || it.trashedAt > 0 } == true
            }) return true
        return policy.hasLocks() && isUtility(a)
    }
    private fun deny(a: Activity) {
        a.window.decorView.visibility = View.INVISIBLE
        Toast.makeText(a, "This conversation is private.", Toast.LENGTH_LONG).show()
        a.finish()
    }
    override fun onActivityCreated(a: Activity, state: Bundle?) {
        if (protected(a)) {
            a.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            if (!unlocked) a.window.decorView.visibility = View.INVISIBLE
        }
    }
    override fun onActivityStarted(a: Activity) { started++ }
    override fun onActivityResumed(a: Activity) {
        if (a.isFinishing) return
        if (a is MainActivity) session.lock()
        if (protected(a)) {
            a.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            if (!unlocked && isUtility(a)) {
                a.window.decorView.visibility = View.INVISIBLE
                session.request()
                a.startActivityForResult(Intent(a, UnlockActivity::class.java).putExtra("utility", true), UTILITY_UNLOCK)
            } else if (!unlocked) deny(a) else a.window.decorView.visibility = View.VISIBLE
        }
    }
    override fun onActivityPaused(a: Activity) { if (protected(a)) a.window.decorView.visibility = View.INVISIBLE }
    override fun onActivityStopped(a: Activity) {
        started = (started - 1).coerceAtLeast(0)
        if (started == 0 && !a.isChangingConfigurations) session.lock()
    }
    override fun onActivitySaveInstanceState(a: Activity, state: Bundle) {}
    override fun onActivityDestroyed(a: Activity) {}
}
