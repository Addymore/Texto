package dev.texto.privacy

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutManager
import android.os.Build
import android.text.InputType
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import dev.octoshrimpy.quik.model.Conversation
import io.realm.Realm

object PrivacyAuthDialogs {
    fun hasLockedSms(context: Context): Boolean {
        val policy = TextoPolicy(context)
        if (policy.hasLocks()) return true
        return runCatching {
            Realm.getDefaultInstance().use { realm ->
                realm.where(Conversation::class.java).equalTo("textoLocked", true).count() > 0
            }
        }.getOrDefault(false)
    }

    /**
     * Xiaomi Messages-style onboarding proposal when pulling down with one finger
     * and either no credentials (PIN/Pattern) or no locked SMS are configured yet.
     */
    fun showSetupProposal(activity: Activity, onComplete: (() -> Unit)? = null) {
        val policy = TextoPolicy(activity)
        val hasCreds = policy.hasPin()
        val hasLocked = hasLockedSms(activity)
        val title = if (!hasCreds) "Set up Private Messages" else "No locked SMS yet"
        val message = if (!hasCreds) {
            "No locked messages or security method set yet. Choose Fingerprint + PIN or Fingerprint + Pattern to protect your private SMS conversations."
        } else {
            "You have not locked any SMS conversations yet. You can select contacts/numbers to lock now, or update your Fingerprint / PIN / Pattern security."
        }

        val options = if (!hasCreds) {
            arrayOf(
                "Set Fingerprint + PIN (6–12 digits)",
                "Set Fingerprint + Pattern (3×3 grid)",
                "Open Privacy & Protection settings"
            )
        } else {
            arrayOf(
                "Choose conversations / numbers to lock",
                "Open Private Messages space",
                "Set or change Fingerprint + PIN",
                "Set or change Fingerprint + Pattern"
            )
        }

        TextoDialogs.builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setItems(options) { _, which ->
                if (!hasCreds) {
                    when (which) {
                        0 -> showPinSetupDialog(activity) {
                            policy.setBiometricEnabled(true)
                            PrivacyGate.session.request()
                            PrivacyGate.session.authenticate()
                            promptLockContactsAfterSetup(activity, onComplete)
                        }
                        1 -> showPatternSetupDialog(activity) {
                            policy.setBiometricEnabled(true)
                            PrivacyGate.session.request()
                            PrivacyGate.session.authenticate()
                            promptLockContactsAfterSetup(activity, onComplete)
                        }
                        2 -> activity.startActivity(Intent(activity, ProtectionActivity::class.java))
                    }
                } else {
                    when (which) {
                        0 -> {
                            if (PrivacyGate.unlocked) {
                                showLockPicker(activity, onComplete)
                            } else {
                                PrivacyGate.openPrivateSpace(activity, proposeLockPicker = true)
                            }
                        }
                        1 -> PrivacyGate.openPrivateSpace(activity, proposeLockPicker = false)
                        2 -> showPinSetupDialog(activity) { onComplete?.invoke() }
                        3 -> showPatternSetupDialog(activity) { onComplete?.invoke() }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun promptLockContactsAfterSetup(activity: Activity, onComplete: (() -> Unit)?) {
        Toast.makeText(activity, "Security configured! Choose numbers to lock or open Private Messages.", Toast.LENGTH_SHORT).show()
        showLockPicker(activity, onComplete)
    }

    fun showLockPicker(activity: Activity, onComplete: (() -> Unit)? = null) {
        RuleContactCards.show(activity, "locked", "Locked & private numbers") {
            applyRulesToRealm(activity)
            onComplete?.invoke()
            if (!VaultActivity::class.java.isInstance(activity)) {
                PrivacyGate.openPrivateSpace(activity, proposeLockPicker = false)
            }
        }
    }

    fun applyRulesToRealm(context: Context) {
        val policy = TextoPolicy(context)
        Realm.getDefaultInstance().use { realm ->
            realm.executeTransaction {
                realm.where(Conversation::class.java).findAll().forEach { conversation ->
                    val rules = conversation.recipients.map {
                        policy.decision(it.address, conversation.lastMessage?.getText().orEmpty(), isKnownContact = it.contact != null)
                    }
                    conversation.textoLocked = rules.any { it.locked }
                    if (rules.any { it.archived || it.spam }) conversation.archived = true
                    if (rules.any { it.locked } && Build.VERSION.SDK_INT >= 26) {
                        val manager = context.getSystemService(NotificationManager::class.java)
                        manager.getNotificationChannel("notifications_${conversation.id}")?.let { channel ->
                            channel.name = "Protected conversation"
                            channel.description = "Content hidden by Texto"
                            manager.createNotificationChannel(channel)
                        }
                    }
                    if (rules.any { it.blocked }) {
                        conversation.blocked = true
                        conversation.blockReason = "Texto spam rule"
                    }
                }
            }
        }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll()
        if (Build.VERSION.SDK_INT >= 25) {
            val shortcuts = context.getSystemService(ShortcutManager::class.java)
            shortcuts.removeAllDynamicShortcuts()
            shortcuts.disableShortcuts(shortcuts.pinnedShortcuts.map { it.id }, "Open Texto to view protected conversations")
        }
        context.sendBroadcast(
            Intent(context, dev.octoshrimpy.quik.feature.widget.WidgetProvider::class.java)
                .setAction("${context.packageName}.action.ACTION_NOTIFY_DATASET_CHANGED")
        )
    }

    fun showPinSetupDialog(activity: Activity, onSaved: () -> Unit) {
        val policy = TextoPolicy(activity)
        val fields = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 16, 40, 0)
        }
        fun pinField(hintText: String) = TextInputEditText(activity).apply {
            hint = hintText
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            fields.addView(this)
        }
        val old = if (policy.hasOnlyPin()) pinField("Current PIN") else null
        val first = pinField("New PIN (6–12 digits)")
        val second = pinField("Confirm new PIN")
        val dialog = TextoDialogs.builder(activity)
            .setTitle("Privacy PIN & Fingerprint")
            .setView(fields)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(-1).setOnClickListener {
                val value = first.text.toString()
                if (!value.matches(Regex("[0-9]{6,12}")) || value != second.text.toString()) {
                    first.error = "Use 6–12 matching digits"
                    return@setOnClickListener
                }
                dialog.getButton(-1).isEnabled = false
                val previous = old?.text?.toString()
                Thread {
                    val valid = previous == null || policy.verifyPin(previous)
                    if (valid) policy.setPin(value)
                    activity.runOnUiThread {
                        dialog.getButton(-1).isEnabled = true
                        if (valid) {
                            dialog.dismiss()
                            onSaved()
                        } else {
                            old?.error = "Incorrect PIN or retry delay active (${policy.retrySeconds()}s)"
                        }
                    }
                }.start()
            }
        }
        dialog.show()
    }

    fun showPatternSetupDialog(activity: Activity, onSaved: () -> Unit) {
        val policy = TextoPolicy(activity)
        val d = activity.resources.displayMetrics.density
        val ink = TextoAppearance.readableText(activity)
        val container = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * d).toInt(), (12 * d).toInt(), (24 * d).toInt(), (8 * d).toInt())
        }
        val status = TextView(activity).apply {
            text = "Connect at least 4 dots to draw your new privacy pattern."
            textSize = 14f
            setTextColor(ink)
            setPadding(0, 0, 0, (12 * d).toInt())
        }
        val patternView = PatternLockView(activity)
        container.addView(status)
        container.addView(patternView, LinearLayout.LayoutParams(-1, (240 * d).toInt()))

        var firstPattern: String? = null
        val dialog = TextoDialogs.builder(activity)
            .setTitle("Privacy Pattern & Fingerprint")
            .setView(container)
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Reset", null)
            .create()

        patternView.onPatternDetected = { drawn ->
            val count = drawn.split("-").filter { it.isNotBlank() }.size
            if (count < 4) {
                status.text = "Connect at least 4 dots. Try again."
                patternView.clearPattern()
            } else if (firstPattern == null) {
                firstPattern = drawn
                status.text = "Draw the same pattern again to confirm."
                patternView.clearPattern()
            } else if (firstPattern == drawn) {
                policy.setPattern(drawn)
                dialog.dismiss()
                Toast.makeText(activity, "Privacy pattern saved.", Toast.LENGTH_SHORT).show()
                onSaved()
            } else {
                firstPattern = null
                status.text = "Patterns did not match. Draw your new pattern again."
                patternView.clearPattern()
            }
        }
        dialog.setOnShowListener {
            dialog.getButton(android.content.DialogInterface.BUTTON_NEUTRAL)?.setOnClickListener {
                firstPattern = null
                status.text = "Connect at least 4 dots to draw your new privacy pattern."
                patternView.clearPattern()
            }
        }
        dialog.show()
    }
}
