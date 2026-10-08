package dev.texto.privacy

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dagger.android.AndroidInjection
import dev.octoshrimpy.quik.R
import dev.octoshrimpy.quik.common.base.QkThemedActivity
import dev.octoshrimpy.quik.feature.conversations.ConversationsAdapter
import dev.octoshrimpy.quik.model.Conversation
import io.realm.Case
import io.realm.Realm
import io.realm.Sort
import javax.inject.Inject

class VaultActivity : QkThemedActivity() {
    @Inject lateinit var conversationsAdapter: ConversationsAdapter
    private var realm: Realm? = null
    private lateinit var list: RecyclerView
    private var query = ""
    private var dialog: androidx.appcompat.app.AlertDialog? = null
    override fun onCreate(state: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(state)
        TextoTheme.apply(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if (!PrivacyGate.unlocked) { finish(); return }
        setContentView(R.layout.texto_vault)
        findViewById<ReachableMessagesLayout>(R.id.textoReachable).apply { privatePullEnabled=false; restingSubtitle="Only visible while unlocked" }
        InboxArtwork.background(findViewById(R.id.textoReachable),"background_image")
        InboxArtwork.background(findViewById(R.id.textoHeader),"header_image")
        list = findViewById(R.id.recyclerView)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = conversationsAdapter
        conversationsAdapter.emptyView = findViewById(R.id.privateEmpty)
        conversationsAdapter.onConversationLongClick = { id ->
            dialog = dev.texto.privacy.TextoDialogs.builder(this).setTitle("Private conversation")
                .setItems(arrayOf("Open conversation", "Privacy & protection", "Move to recycle bin")) { _, which ->
                    if (PrivacyGate.unlocked) when(which) {
                        0 -> startActivity(Intent(this,dev.octoshrimpy.quik.feature.compose.ComposeActivity::class.java).putExtra("threadId",id))
                        1 -> startActivity(Intent(this,ProtectionActivity::class.java))
                        2 -> Thread {
                            val result = runCatching { Realm.getDefaultInstance().use { r ->
                                val ids = r.where(dev.octoshrimpy.quik.model.Message::class.java).equalTo("threadId",id).equalTo("trashedAt",0L).findAll().map { it.id }
                                TrashStore(this).move(ids)
                            } }
                            runOnUiThread { if(!isDestroyed && PrivacyGate.unlocked && result.isFailure) android.widget.Toast.makeText(this,"Could not move messages. Please try again.",android.widget.Toast.LENGTH_LONG).show() }
                        }.start()
                    }
                }.show()
        }
        findViewById<TextInputEditText>(R.id.privateSearch).doAfterTextChanged { query=it.toString(); load() }
        findViewById<MaterialButton>(R.id.privateThemes).setOnClickListener { startActivity(Intent(this,ThemesActivity::class.java).putExtra("from_private",true)) }
        findViewById<MaterialButton>(R.id.privateMore).setOnClickListener {
            val destinations = arrayOf(ProtectionActivity::class.java,TrashActivity::class.java,dev.octoshrimpy.quik.feature.backup.BackupActivity::class.java,dev.octoshrimpy.quik.feature.scheduled.ScheduledActivity::class.java)
            dialog = dev.texto.privacy.TextoDialogs.builder(this).setTitle("Private options").setItems(arrayOf("Privacy & protection","Recycle bin","Backup & restore","Scheduled messages")) { _, i ->
                if(PrivacyGate.unlocked) startActivity(Intent(this,destinations[i]).putExtra("protected_tools",true))
            }.show()
        }
        findViewById<MaterialButton>(R.id.privateLock).setOnClickListener { PrivacyGate.session.lock(); finish() }
    }
    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if(!PrivacyGate.unlocked) { finish(); return }
        if(!::list.isInitialized) return
        if(realm == null) realm=Realm.getDefaultInstance()
        load(); conversationsAdapter.notifyDataSetChanged()
        TextoAppearance.smooth(list); TextoAppearance.refreshRate(this)
        listOf(R.id.privateThemes,R.id.privateMore,R.id.privateLock).forEach { id -> findViewById<MaterialButton>(id).apply {
            backgroundTintList=android.content.res.ColorStateList.valueOf(TextoAppearance.accent(this@VaultActivity)); setTextColor(TextoAppearance.onAccent(this@VaultActivity))
        } }
    }
    private fun load() {
        if(!PrivacyGate.unlocked) return
        val r=realm ?: return
        r.refresh()
        val matches=r.where(Conversation::class.java).equalTo("textoLocked",true).isNotNull("lastMessage")
        if(query.isNotBlank()) matches.beginGroup().contains("recipients.address",query,Case.INSENSITIVE).or().contains("recipients.contact.name",query,Case.INSENSITIVE).or().contains("lastMessage.body",query,Case.INSENSITIVE).endGroup()
        conversationsAdapter.updateData(matches.sort(arrayOf("pinned","lastMessage.date"),arrayOf(Sort.DESCENDING,Sort.DESCENDING)).findAll())
    }
    override fun onPause() { dialog?.dismiss(); super.onPause() }
    override fun onDestroy() {
        if(::list.isInitialized) { list.adapter=null; conversationsAdapter.updateData(null) }
        realm?.close(); realm=null
        super.onDestroy()
    }
}
