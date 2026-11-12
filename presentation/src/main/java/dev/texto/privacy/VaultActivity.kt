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
        findViewById<ReachableMessagesLayout>(R.id.textoReachable).apply {
            privatePullEnabled = true
            privateActionLabel = "Lock private messages"
            restingSubtitle = "Only visible while unlocked"
            onPrivatePull = { PrivacyGate.session.lock(); finish() }
        }
        InboxArtwork.background(findViewById(R.id.textoReachable),"background_image")
        InboxArtwork.background(findViewById(R.id.textoHeader),"header_image")
        list = findViewById(R.id.recyclerView)
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = conversationsAdapter
        val emptyView = findViewById<android.widget.TextView>(R.id.privateEmpty).apply {
            text = "No locked SMS conversations yet.\nTap here to choose numbers/conversations to lock or configure Fingerprint / PIN / Pattern."
            isClickable = true
            isFocusable = true
            setOnClickListener {
                PrivacyAuthDialogs.showSetupProposal(this@VaultActivity) { load() }
            }
        }
        conversationsAdapter.emptyView = emptyView
        findViewById<TextInputEditText>(R.id.privateSearch).doAfterTextChanged { query=it.toString(); load() }
        findViewById<MaterialButton>(R.id.privateThemes).setOnClickListener { startActivity(Intent(this,ThemesActivity::class.java).putExtra("from_private",true)) }
        findViewById<MaterialButton>(R.id.privateMore).setOnClickListener {
            val labels = arrayOf(
                "Choose conversations / numbers to lock",
                "Set Fingerprint / PIN / Pattern",
                "Privacy & protection",
                "Recycle bin",
                "Backup & restore",
                "Scheduled messages"
            )
            dialog = dev.texto.privacy.TextoDialogs.builder(this).setTitle("Private options").setItems(labels) { _, i ->
                if (!PrivacyGate.unlocked) return@setItems
                when (i) {
                    0 -> PrivacyAuthDialogs.showLockPicker(this) { load() }
                    1 -> PrivacyAuthDialogs.showSetupProposal(this) { load() }
                    else -> {
                        val destinations = arrayOf(
                            ProtectionActivity::class.java,
                            TrashActivity::class.java,
                            dev.octoshrimpy.quik.feature.backup.BackupActivity::class.java,
                            dev.octoshrimpy.quik.feature.scheduled.ScheduledActivity::class.java
                        )
                        startActivity(Intent(this, destinations[i - 2]).putExtra("protected_tools", true))
                    }
                }
            }.show()
        }
        findViewById<MaterialButton>(R.id.privateLock).setOnClickListener { PrivacyGate.session.lock(); finish() }
        if (state == null && intent.getBooleanExtra("propose_lock_picker", false)) {
            intent.removeExtra("propose_lock_picker")
            list.post { if (!isFinishing && PrivacyGate.unlocked) PrivacyAuthDialogs.showLockPicker(this) { load() } }
        }
    }
    override fun onResume() {
        super.onResume()
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if(!PrivacyGate.unlocked) { finish(); return }
        if(!::list.isInitialized) return
        if(realm == null) realm=Realm.getDefaultInstance()
        load(); conversationsAdapter.notifyDataSetChanged()
        TextoAppearance.smooth(list); TextoAppearance.refreshRate(this)
        val d = resources.displayMetrics.density
        listOf(R.id.privateThemes,R.id.privateMore,R.id.privateLock).forEach { id -> findViewById<MaterialButton>(id).apply {
            isAllCaps = false
            maxLines = 1
            insetTop = 0
            insetBottom = 0
            cornerRadius = (18 * d).toInt()
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
