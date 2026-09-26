package dev.texto.testing

import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import dev.octoshrimpy.quik.R
import dev.octoshrimpy.quik.databinding.ConversationListItemBinding
import dev.octoshrimpy.quik.feature.conversations.ConversationsAdapter
import dev.texto.privacy.PrivacyGate
import dev.texto.privacy.TextoAppearance
import dev.texto.privacy.VaultActivity

/** Separate emulator test APK: never exposes an authentication shortcut in Texto. */
class LayoutInstrumentation : Instrumentation() {
    private fun checkMain(block: () -> Unit) {
        var failure: Throwable? = null
        runOnMainSync { try { block() } catch(error: Throwable) { failure=error } }
        failure?.let { throw it }
    }
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val result=Bundle()
        try {
            check(android.os.Build.MODEL.contains("sdk",true)) { "Disposable emulator required" }
            waitForIdleSync()
            val main=startActivitySync(targetContext.packageManager.getLaunchIntentForPackage(targetContext.packageName)!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            waitForIdleSync()
            checkMain { PrivacyGate.session.request(); check(PrivacyGate.session.authenticate()) }
            val vault=startActivitySync(Intent(targetContext,VaultActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            waitForIdleSync()
            lateinit var list: RecyclerView
            lateinit var adapter: ConversationsAdapter
            var count=0
            checkMain {
                check(!vault.isFinishing)
                check(vault.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
                list=vault.findViewById(R.id.recyclerView)
                adapter=list.adapter as ConversationsAdapter
                count=adapter.itemCount; check(count>0) { "Seed a locked synthetic conversation before this check" }
                check(adapter.data!!.all { it.textoLocked })
                val public=main.findViewById<RecyclerView>(R.id.recyclerView).adapter as ConversationsAdapter
                check(public.data!!.none { it.textoLocked })
                check(list.getChildAt(0).findViewById<View>(R.id.avatars)!=null)
            }
            val p=TextoAppearance.prefs(targetContext)
            val keys=listOf("preview_lines","message_counts","list_avatars","density","unread_style")
            val before=p.all.filterKeys { it in keys }
            try {
                p.edit().putString("preview_lines","0").putBoolean("message_counts",false).putBoolean("list_avatars",false).putString("density","compact").putString("unread_style","dot").commit()
                checkMain { adapter.notifyDataSetChanged() }; waitForIdleSync()
                checkMain {
                    val row=ConversationListItemBinding.bind(list.getChildAt(0))
                    check(row.snippet.visibility==View.GONE && row.messageCount.visibility==View.GONE && row.avatars.visibility==View.GONE)
                    val sample=ConversationListItemBinding.inflate(vault.layoutInflater)
                    TextoAppearance.styleConversation(sample,24,2)
                    check(row.root.paddingTop==sample.root.paddingTop)
                    check(sample.snippet.visibility==row.snippet.visibility)
                    vault.findViewById<TextInputEditText>(R.id.privateSearch).setText("no-such-private-thread-XYZ")
                }
                waitForIdleSync()
                checkMain { check(adapter.itemCount==0); vault.findViewById<TextInputEditText>(R.id.privateSearch).setText("") }
                waitForIdleSync()
                checkMain { check(adapter.itemCount==count) }
            } finally {
                val edit=p.edit(); keys.forEach { edit.remove(it) }
                before.forEach { (key,value) -> when(value) { is String -> edit.putString(key,value); is Boolean -> edit.putBoolean(key,value) } }; edit.commit()
            }
            val monitor=addMonitor("dev.octoshrimpy.quik.feature.compose.ComposeActivity",null,false)
            checkMain { list.getChildAt(0).performClick() }
            val compose=checkNotNull(waitForMonitorWithTimeout(monitor,5000))
            waitForIdleSync()
            checkMain { check(compose.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE!=0) }
            sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_HOME)
            var locked=false
            for(attempt in 0..49) {
                checkMain { locked=!PrivacyGate.unlocked }
                if(locked) break
                android.os.SystemClock.sleep(100)
            }
            check(locked) { "Session remained unlocked after the app stopped" }
            result.putString("result","PASS: shared conversation adapter, private-only rows, public exclusion, secure thread, shared theme options and preview, private search, relock on background")
            finish(0,result)
        } catch(error: Throwable) { result.putString("error",error.stackTraceToString()); finish(1,result) }
    }
}
