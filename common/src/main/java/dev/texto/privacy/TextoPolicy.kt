package dev.texto.privacy

import android.content.Context
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Locale
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class TextoPolicy(context: Context) {
    private val app = context.applicationContext
    val preferences = app.getSharedPreferences("texto_privacy", Context.MODE_PRIVATE)
    private val country: String get() = runCatching {
        (app.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).networkCountryIso
    }.getOrDefault("").ifBlank { Locale.getDefault().country }.uppercase(Locale.ROOT)

    fun normalize(value: String): String {
        val raw = value.trim()
        if (raw.any { it.isLetter() }) return raw.lowercase(Locale.ROOT)
        return PhoneNumberUtils.formatNumberToE164(raw, country)
            ?: PhoneNumberUtils.normalizeNumber(raw).let { if (it.startsWith("00")) "+" + it.drop(2) else it }
    }
    fun entries(key: String): Set<String> = preferences.getStringSet(key, emptySet())!!.toSet()
    fun save(key: String, values: Collection<String>) {
        val normalized = values.map { if (key == "words") it.trim() else if (key == "prefixes")
            PhoneNumberUtils.normalizeNumber(it.trim()) else normalize(it) }.filter { it.isNotBlank() }.toSet()
        check(preferences.edit().putStringSet(key, normalized).commit())
    }
    fun decision(address: String, body: String = ""): RuleEngine.Decision = RuleEngine.decide(
        normalize(address), body, entries("locked"), entries("archived"), entries("blocked"),
        entries("allowed"), entries("prefixes"), entries("words"))
    fun hasLocks() = entries("locked").isNotEmpty()
    fun hasPin() = preferences.contains("pin_hash")
    fun setPin(pin: String) {
        require(pin.matches(Regex("[0-9]{6,12}")))
        val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
        check(preferences.edit().putString("pin_salt", encode(salt))
            .putString("pin_hash", encode(derive(pin, salt))).putInt("attempts", 0)
            .putLong("retry_at", 0).commit())
    }
    fun retrySeconds(): Long = ((preferences.getLong("retry_at", 0) - System.currentTimeMillis() + 999) / 1000).coerceAtLeast(0)
    @Synchronized fun verifyPin(pin: String): Boolean {
        if (!hasPin() || retrySeconds() > 0) return false
        val valid = pin.matches(Regex("[0-9]{6,12}")) && runCatching {
            val actual = derive(pin, decode(preferences.getString("pin_salt", "")!!))
            MessageDigest.isEqual(actual, decode(preferences.getString("pin_hash", "")!!))
        }.getOrDefault(false)
        val attempts = if (valid) 0 else preferences.getInt("attempts", 0) + 1
        preferences.edit().putInt("attempts", attempts).putLong("retry_at",
            if (attempts >= 5) System.currentTimeMillis() + (30_000L * (1L shl (attempts - 5).coerceAtMost(6))) else 0).commit()
        return valid
    }
    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 210_000, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
    private fun encode(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun decode(value: String) = Base64.decode(value, Base64.NO_WRAP)
}
