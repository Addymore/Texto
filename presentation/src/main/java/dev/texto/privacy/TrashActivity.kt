package dev.texto.privacy

import android.os.Bundle
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.octoshrimpy.quik.R
import dev.octoshrimpy.quik.model.Message
import io.realm.Realm
import io.realm.Sort
import java.text.DateFormat
import java.util.Date

class TrashActivity : AppCompatActivity() {
    private var dialog: androidx.appcompat.app.AlertDialog? = null
    override fun onCreate(state: Bundle?) { setTheme(R.style.TextoTheme); TextoTheme.apply(this); super.onCreate(state); window.addFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    override fun onResume() { super.onResume(); if (!PrivacyGate.unlocked) { finish(); return }; render() }
    override fun onPause() { dialog?.dismiss(); super.onPause() }
    private fun render() {
        if (!PrivacyGate.unlocked) { finish(); return }
        val prefs = TextoPolicy(this).preferences
        val days = TrashRetention.days(prefs.getInt("bin_days", 30))
        val d = resources.displayMetrics.density
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding((24*d).toInt(), (36*d).toInt(), (24*d).toInt(), (16*d).toInt()) }
        root.addView(TextView(this).apply { text = "Recycle bin"; textSize = 32f })
        root.addView(TextView(this).apply { text = "Protected by your privacy PIN. Tap a message to restore it. SMS and MMS attachments are retained until expiry."; textSize = 15f; setPadding(0,16,0,16) })
        root.addView(MaterialButton(this).apply { text = "Auto-delete after $days days"; setOnClickListener {
            dialog = dev.texto.privacy.TextoDialogs.builder(this@TrashActivity).setTitle("Keep deleted messages")
                .setSingleChoiceItems(arrayOf("30 days", "60 days", "90 days"), TrashRetention.choices.indexOf(days)) { choice, index ->
                    val selected = TrashRetention.choices[index]
                    choice.dismiss()
                    dialog = dev.texto.privacy.TextoDialogs.builder(this@TrashActivity).setTitle("Keep for $selected days?")
                        .setMessage("Age is measured from when each message was deleted. Items older than $selected days will be permanently removed at the next cleanup.")
                        .setNegativeButton("Cancel", null).setPositiveButton("Apply") { _, _ ->
                            if (PrivacyGate.unlocked) { prefs.edit().putInt("bin_days", selected).apply(); TrashStore.schedule(this@TrashActivity); render() }
                        }.show()
                }.setNegativeButton("Cancel", null).show()
        } })
        val entries = Realm.getDefaultInstance().use { realm -> realm.refresh(); realm.copyFromRealm(realm.where(Message::class.java).greaterThan("trashedAt", 0L).sort("trashedAt", Sort.DESCENDING).findAll()) }
        if (entries.isEmpty()) root.addView(TextView(this).apply { text = "Your recycle bin is empty"; textSize = 18f })
        val list = ListView(this).apply { divider = null }
        val date = DateFormat.getDateInstance(DateFormat.MEDIUM)
        list.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, entries.map {
            "${it.address}\n${it.getSummary().take(140)}\nExpires ${date.format(Date(TrashRetention.expiresAt(it.trashedAt, days)))}"
        })
        list.setOnItemClickListener { _, _, position, _ ->
            val message = entries[position]
            dialog = dev.texto.privacy.TextoDialogs.builder(this).setTitle("Restore message?").setMessage(message.getSummary())
                .setNegativeButton("Cancel", null).setPositiveButton("Restore") { _, _ ->
                    if (!PrivacyGate.unlocked) return@setPositiveButton
                    Thread {
                        val result = runCatching { TrashStore(this).restore(listOf(message.id)) }
                        runOnUiThread { if (!isDestroyed && PrivacyGate.unlocked) {
                            result.onSuccess { render() }.onFailure { Toast.makeText(this, "Could not restore. Please try again.", Toast.LENGTH_LONG).show() }
                        } }
                    }.start()
                }.show()
        }
        root.addView(list, LinearLayout.LayoutParams(-1,0,1f))
        root.addView(TextView(this).apply { text = "Cleanup runs daily and when Texto opens. Android may defer it while the phone is idle. Restored private numbers remain locked."; textSize = 12f })
        root.addView(MaterialButton(this).apply { text = "Lock and close"; setOnClickListener { PrivacyGate.session.lock(); finish() } })
        setContentView(root)
    }
}
