package dev.texto.privacy

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dev.octoshrimpy.quik.R
import dev.octoshrimpy.quik.model.Conversation
import dev.octoshrimpy.quik.feature.settings.SettingsActivity
import dev.octoshrimpy.quik.feature.blocking.BlockingActivity
import io.realm.Realm

class ProtectionActivity : AppCompatActivity() {
    private val policy by lazy { TextoPolicy(this) }
    private lateinit var content: LinearLayout
    private var lockedRuleDialog: androidx.appcompat.app.AlertDialog? = null
    override fun onPause() { lockedRuleDialog?.dismiss(); lockedRuleDialog = null; super.onPause() }
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.TextoTheme)
        if (policy.preferences.getBoolean("dynamic_colors", false)) com.google.android.material.color.DynamicColors.applyToActivityIfAvailable(this)
        TextoTheme.apply(this)
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        render()
    }
    private fun render() {
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 40, 48, 56) }
        setContentView(ScrollView(this).apply { addView(content) })
        label("Privacy & protection", 30f)
        label("Rules follow a number, including future SMS and MMS. Use international numbers, for example +250788123456.", 16f)
        button(if (policy.hasPin()) "Change PIN" else "Create a privacy PIN") { changePin() }
        label("Locked numbers stay hidden and silent. Normal messages open freely. Android’s shared SMS storage is not encrypted by Texto.", 14f)
        listOf("locked" to "Locked & archived", "archived" to "Always archived", "blocked" to "Blocked numbers",
            "allowed" to "Trusted numbers", "prefixes" to "Blocked number prefixes", "words" to "Spam phrases").forEach { (key, title) ->
            button("$title · ${policy.entries(key).size}") {
                if (key == "locked" && !policy.hasPin()) changePin() else edit(key, title)
            }
        }
        label("Spam rules quarantine messages in Blocked. Trusted numbers bypass Texto spam rules, but never privacy rules. Prefix rules use normalized international prefixes; phrases are case-insensitive. Existing QUIK blocking settings also apply.", 14f)
        button("Review blocked messages & advanced filters") { startActivity(Intent(this, BlockingActivity::class.java)) }
        button("Appearance, colors & conversation themes") { startActivity(Intent(this, SettingsActivity::class.java)) }
        button(if (policy.preferences.getBoolean("dynamic_colors", false)) "Wallpaper colors: on" else "Wallpaper colors: off") {
            policy.preferences.edit().putBoolean("dynamic_colors", !policy.preferences.getBoolean("dynamic_colors", false)).apply()
            recreate()
        }
        label("Wallpaper colors use Material You on supported Android 12+ devices. Your existing light, dark, AMOLED and per-contact color settings remain available in Appearance.", 14f)
        label("Message receipts", 22f)
        label("✓ Sent   ✓✓ Delivered   ↓ Received\nDelivery depends on carrier reports and your delivery-report setting. SMS does not provide read receipts. RCS requires carrier/OEM privileged integration and is unavailable in this build.", 14f)
        button("Back to messages") { finish() }
    }
    private fun label(value: String, size: Float) { content.addView(TextView(this).apply { text = value; textSize = size; setPadding(0, 16, 0, 16) }) }
    private fun button(value: String, action: () -> Unit) { content.addView(MaterialButton(this).apply { text = value; isAllCaps = false; setOnClickListener { action() } }) }
    private fun edit(key: String, title: String) {
        if (key == "locked" && policy.hasLocks() && !PrivacyGate.unlocked) {
            Toast.makeText(this, "Private settings are locked.", Toast.LENGTH_LONG).show()
            return
        }
        val field = TextInputEditText(this).apply { minLines = 4; maxLines = 10; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE; setText((policy.entries(key) + listOfNotNull(intent.getStringExtra("address").takeIf { key != "words" && key != "prefixes" })).sorted().joinToString("\n")) }
        val dialog = MaterialAlertDialogBuilder(this).setTitle(title).setMessage("One entry per line. Remove an entry to stop applying that rule.")
            .setView(field).setNegativeButton("Cancel", null).setPositiveButton("Save") { _, _ ->
                if (key == "locked" && policy.hasLocks() && !PrivacyGate.unlocked) return@setPositiveButton
                policy.save(key, field.text.toString().lines())
                applyRules()
                render()
            }.show()
        if (key == "locked") lockedRuleDialog = dialog
    }
    private fun applyRules() {
        Realm.getDefaultInstance().use { realm ->
            realm.executeTransaction {
                realm.where(Conversation::class.java).findAll().forEach { conversation ->
                    val rules = conversation.recipients.map { policy.decision(it.address, conversation.lastMessage?.getText().orEmpty()) }
                    conversation.textoLocked = rules.any { it.locked }
                            if (rules.any { it.archived }) conversation.archived = true
                    if (rules.any { it.locked } && Build.VERSION.SDK_INT >= 26) {
                        val manager = getSystemService(NotificationManager::class.java)
                        manager.getNotificationChannel("notifications_${conversation.id}")?.let { channel ->
                            channel.name = "Protected conversation"
                            channel.description = "Content hidden by Texto"
                            manager.createNotificationChannel(channel)
                        }
                    }
                    if (rules.any { it.blocked }) { conversation.blocked = true; conversation.blockReason = "Texto spam rule" }
                }
            }
        }
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
        if (Build.VERSION.SDK_INT >= 25) {
            val shortcuts = getSystemService(ShortcutManager::class.java)
            shortcuts.removeAllDynamicShortcuts()
            shortcuts.disableShortcuts(shortcuts.pinnedShortcuts.map { it.id }, "Open Texto to view protected conversations")
        }
        // Refresh existing home-screen widgets after rules change.
        sendBroadcast(Intent(this, dev.octoshrimpy.quik.feature.widget.WidgetProvider::class.java)
            .setAction("$packageName.action.ACTION_NOTIFY_DATASET_CHANGED"))
    }
    private fun changePin() {
        val fields = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 0, 40, 0) }
        fun pin(hintText: String) = TextInputEditText(this).apply { hint = hintText; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD; fields.addView(this) }
        val old = if (policy.hasPin()) pin("Current PIN") else null
        val first = pin("New PIN (6–12 digits)"); val second = pin("Confirm new PIN")
        val dialog = MaterialAlertDialogBuilder(this).setTitle("Privacy PIN").setView(fields)
            .setNegativeButton("Cancel", null).setPositiveButton("Save", null).create()
        dialog.setOnShowListener { dialog.getButton(-1).setOnClickListener {
            val value = first.text.toString()
            if (!value.matches(Regex("[0-9]{6,12}")) || value != second.text.toString()) {
                first.error = "Use 6–12 matching digits"; return@setOnClickListener
            }
            dialog.getButton(-1).isEnabled = false
            val previous = old?.text?.toString()
            Thread {
                val valid = previous == null || policy.verifyPin(previous)
                if (valid) policy.setPin(value)
                runOnUiThread {
                    dialog.getButton(-1).isEnabled = true
                    if (valid) { dialog.dismiss(); render() }
                    else old?.error = "Incorrect PIN or retry delay active (${policy.retrySeconds()}s)"
                }
            }.start()
        } }
        dialog.show()
    }
}
