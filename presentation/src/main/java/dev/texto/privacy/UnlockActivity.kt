package dev.texto.privacy

import android.os.Bundle
import android.text.InputType
import android.view.WindowManager
import android.widget.LinearLayout
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
    override fun onPostResume() {
        super.onPostResume()
        autoPrompt?.also { autoPrompt = null; it() }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.TextoTheme)
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if (!PrivacyGate.session.pending) { finish(); return }
        val policy = TextoPolicy(this)
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 100, 48, 48) }
        val title = TextView(this).apply { text = if (intent.getBooleanExtra("bin", false)) "Recycle bin" else "Private messages"; textSize = 32f }
        val note = TextView(this).apply { text = "Your protected conversations stay private. Enter your PIN to continue."; textSize = 16f }
        val pin = TextInputEditText(this).apply { hint = "6–12 digit PIN"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD }
        val error = TextView(this)
        box.addView(title); box.addView(note); box.addView(pin); box.addView(error)
        box.addView(MaterialButton(this).apply {
            text = "Unlock"
            setOnClickListener {
                isEnabled = false
                val value = pin.text.toString()
                Thread {
                    val valid = policy.verifyPin(value)
                    runOnUiThread {
                        isEnabled = true
                        if (valid) complete() else { pin.text?.clear(); error.text =
                            if (policy.retrySeconds() > 0) "Try again in ${policy.retrySeconds()} seconds." else "Incorrect PIN." }
                    }
                }.start()
            }
        })
        if (BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
            val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { complete() }
                    override fun onAuthenticationError(code: Int, message: CharSequence) {
                        if (code != BiometricPrompt.ERROR_NEGATIVE_BUTTON && code != BiometricPrompt.ERROR_USER_CANCELED) error.text = message
                    }
                })
            val authenticate = {
                prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle(title.text.toString())
                    .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    .setNegativeButtonText("Use PIN").build())
            }
            box.addView(MaterialButton(this).apply {
                text = "Try fingerprint again"
                setOnClickListener { authenticate() }
            })
            if (savedInstanceState == null) autoPrompt = authenticate
        }

        box.addView(MaterialButton(this).apply {
            text = "Cancel"
            setOnClickListener { PrivacyGate.session.lock(); finish() }
        })
        setContentView(box)
    }
    private fun complete() {
        if (!lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) return
        if (!PrivacyGate.session.authenticate()) { finish(); return }
        startActivity(android.content.Intent(this, if (intent.getBooleanExtra("bin", false)) TrashActivity::class.java else VaultActivity::class.java))
        finish()
    }
    @Deprecated("Legacy back compatibility") override fun onBackPressed() { PrivacyGate.session.lock(); finish() }
}
