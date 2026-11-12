package dev.texto.privacy

import android.os.Bundle
import android.text.InputType
import android.view.WindowManager
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import dev.octoshrimpy.quik.R

class UnlockActivity : AppCompatActivity() {
    private var autoPrompt: (() -> Unit)? = null
    private var pendingComplete = false
    private var pinMode = false
    override fun onPostResume() {
        super.onPostResume()
        if (pendingComplete) {
            pendingComplete = false
            complete()
            return
        }
        autoPrompt?.also { autoPrompt = null; it() }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.TextoTheme)
        TextoTheme.apply(this)
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if (!PrivacyGate.session.pending) { finish(); return }
        val policy = TextoPolicy(this)
        val ink = TextoAppearance.readableText(this)
        val d = resources.displayMetrics.density
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 80, 48, 48) }
        val title = TextView(this).apply {
            text = if (intent.getBooleanExtra("utility", false)) "Confirm access"
            else if (intent.getBooleanExtra("bin", false)) "Recycle bin"
            else "Private messages"
            textSize = 32f
            setTextColor(ink)
        }
        val methodDesc = when {
            policy.hasOnlyPin() && policy.hasPattern() -> "fingerprint, pattern, or PIN"
            policy.hasPattern() -> "fingerprint or pattern"
            else -> "fingerprint or your PIN"
        }
        val note = TextView(this).apply {
            text = "Use $methodDesc to continue."
            textSize = 16f
            setTextColor(ink)
        }
        val pin = TextInputEditText(this).apply {
            hint = "6–12 digit PIN"
            setTextColor(ink)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        val error = TextView(this).apply { setTextColor(ink) }
        val pinSection = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(title)
        box.addView(note)
        box.addView(error)
        box.addView(pinSection)

        if (policy.hasPattern()) {
            val patternHint = TextView(this).apply {
                text = "Draw your unlock pattern:"
                textSize = 14f
                setTextColor(ink)
                setPadding(0, (8 * d).toInt(), 0, (4 * d).toInt())
            }
            val patternView = PatternLockView(this).apply {
                onPatternDetected = { drawn ->
                    isEnabled = false
                    Thread {
                        val valid = policy.verifyPattern(drawn)
                        runOnUiThread {
                            isEnabled = true
                            if (valid) complete() else {
                                clearPattern()
                                error.text = if (policy.retrySeconds() > 0) "Try again in ${policy.retrySeconds()} seconds." else "Incorrect pattern."
                            }
                        }
                    }.start()
                }
            }
            pinSection.addView(patternHint)
            pinSection.addView(patternView, LinearLayout.LayoutParams(-1, (220 * d).toInt()))
        }

        pinSection.addView(pin)
        pinSection.addView(MaterialButton(this).apply {
            text = "Unlock"
            setIconResource(R.drawable.ic_texto_privacy)
            TextoAppearance.styleSettingsCard(this)
            setOnClickListener {
                isEnabled = false
                val value = pin.text.toString()
                Thread {
                    val valid = policy.verifyPin(value)
                    runOnUiThread {
                        isEnabled = true
                        if (valid) complete() else {
                            pin.text?.clear()
                            error.text = if (policy.retrySeconds() > 0) "Try again in ${policy.retrySeconds()} seconds." else "Incorrect PIN."
                        }
                    }
                }.start()
            }
        })

        fun showPin() {
            pinMode = true
            pinSection.visibility = View.VISIBLE
            note.text = if (policy.hasPattern()) "Draw your pattern or enter your PIN to continue." else "Enter your PIN to continue."
            if (policy.hasOnlyPin()) pin.requestFocus()
        }

        pinMode = savedInstanceState?.getBoolean("pin_mode") ?: false
        val canBio = policy.biometricEnabled() &&
            BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
        if (canBio) {
            pinSection.visibility = if (pinMode) View.VISIBLE else View.GONE
            note.text = if (pinMode) "Enter your PIN or pattern to continue." else "Use fingerprint to continue."
            val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { complete() }
                    override fun onAuthenticationError(code: Int, message: CharSequence) {
                        showPin()
                        if (code != BiometricPrompt.ERROR_NEGATIVE_BUTTON && code != BiometricPrompt.ERROR_USER_CANCELED) error.text = message
                    }
                })
            val authenticate = {
                pinMode = false
                pinSection.visibility = View.GONE
                pin.text?.clear()
                note.text = "Use fingerprint to continue."
                error.text = ""
                prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle(title.text.toString())
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .setNegativeButtonText("Use PIN").build())
            }
            box.addView(MaterialButton(this).apply {
                text = "Try fingerprint again"
                setIconResource(R.drawable.ic_texto_privacy)
                TextoAppearance.styleSettingsCard(this)
                setOnClickListener { authenticate() }
            })
            box.addView(MaterialButton(this).apply {
                text = "Use PIN"
                setOnClickListener { showPin() }
            })
            if (savedInstanceState == null) autoPrompt = authenticate
        } else showPin()

        box.addView(MaterialButton(this).apply {
            text = "Cancel"
            setIconResource(R.drawable.ic_texto_privacy)
            TextoAppearance.styleSettingsCard(this)
            setOnClickListener { PrivacyGate.session.lock(); finish() }
        })
        setContentView(ScrollView(this).apply { isFillViewport = true; addView(box) })
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("pin_mode", pinMode)
        super.onSaveInstanceState(outState)
    }
    private fun complete() {
        if (!lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
            pendingComplete = true
            return
        }
        pendingComplete = false
        if (!PrivacyGate.session.authenticate()) { finish(); return }
        if (intent.getBooleanExtra("utility", false)) setResult(RESULT_OK)
        else {
            val target = if (intent.getBooleanExtra("bin", false)) TrashActivity::class.java else VaultActivity::class.java
            startActivity(
                android.content.Intent(this, target)
                    .putExtra("propose_lock_picker", intent.getBooleanExtra("propose_lock_picker", false))
            )
        }
        finish()
    }
    @Deprecated("Legacy back compatibility") override fun onBackPressed() { PrivacyGate.session.lock(); finish() }
}
