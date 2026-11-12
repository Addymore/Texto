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
    fun decision(address: String, body: String = "", isKnownContact: Boolean = false): RuleEngine.Decision = RuleEngine.decide(
        normalize(address), body, entries("locked"), entries("archived"), entries("blocked"),
        entries("allowed"), entries("prefixes"), entries("words"),
        spamSenders = entries("spam"), autoSpam = autoSpamEnabled() && body.isNotBlank(), isKnownContact = isKnownContact
    )
    fun autoSpamEnabled(): Boolean = preferences.getBoolean("auto_spam_filter", true)
    fun setAutoSpamEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("auto_spam_filter", enabled).commit()
    }
    fun recordSpamSender(address: String) {
        val norm = normalize(address)
        if (norm.isBlank() || norm in entries("allowed") || norm in entries("locked")) return
        save("spam", entries("spam") + norm)
        save("archived", entries("archived") + norm)
    }
    fun unmarkSpamSender(address: String, alsoUnarchive: Boolean = true) {
        val norm = normalize(address)
        if (norm.isBlank()) return
        save("spam", entries("spam") - norm)
        if (alsoUnarchive) save("archived", entries("archived") - norm)
    }
    fun isSpamSender(address: String, body: String = "", isKnownContact: Boolean = false): Boolean {
        val norm = normalize(address)
        if (norm.isBlank() || norm in entries("allowed") || norm in entries("locked")) return false
        return norm in entries("spam") || (autoSpamEnabled() && body.isNotBlank() && RuleEngine.isIntelligentSpam(norm, body, isKnownContact))
    }
    fun hasLocks() = entries("locked").isNotEmpty()
    fun hasOnlyPin() = preferences.contains("pin_hash")
    fun hasPattern() = preferences.contains("pattern_hash")
    fun hasPin() = hasOnlyPin() || hasPattern()
    fun biometricEnabled(): Boolean = preferences.getBoolean("biometric_enabled", true)
    fun setBiometricEnabled(enabled: Boolean) {
        preferences.edit().putBoolean("biometric_enabled", enabled).commit()
    }
    fun setPin(pin: String) {
        require(pin.matches(Regex("[0-9]{6,12}")))
        val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
        check(preferences.edit().putString("pin_salt", encode(salt))
            .putString("pin_hash", encode(derive(pin, salt)))
            .putString("preferred_auth", "pin")
            .putInt("attempts", 0)
            .putLong("retry_at", 0).commit())
    }
    fun setPattern(pattern: String) {
        val nodes = pattern.split("-").mapNotNull { it.toIntOrNull() }
        require(nodes.size >= 4 && nodes.distinct().size == nodes.size && nodes.all { it in 0..8 })
        val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
        check(preferences.edit().putString("pattern_salt", encode(salt))
            .putString("pattern_hash", encode(derive(pattern, salt)))
            .putString("preferred_auth", "pattern")
            .putInt("attempts", 0)
            .putLong("retry_at", 0).commit())
    }
    fun retrySeconds(): Long = ((preferences.getLong("retry_at", 0) - System.currentTimeMillis() + 999) / 1000).coerceAtLeast(0)
    @Synchronized fun verifyPin(pin: String): Boolean {
        if (!hasOnlyPin() || retrySeconds() > 0) return false
        val valid = pin.matches(Regex("[0-9]{6,12}")) && runCatching {
            val actual = derive(pin, decode(preferences.getString("pin_salt", "")!!))
            MessageDigest.isEqual(actual, decode(preferences.getString("pin_hash", "")!!))
        }.getOrDefault(false)
        recordAttempt(valid)
        return valid
    }
    @Synchronized fun verifyPattern(pattern: String): Boolean {
        if (!hasPattern() || retrySeconds() > 0) return false
        val nodes = pattern.split("-").mapNotNull { it.toIntOrNull() }
        val valid = nodes.size >= 4 && runCatching {
            val actual = derive(pattern, decode(preferences.getString("pattern_salt", "")!!))
            MessageDigest.isEqual(actual, decode(preferences.getString("pattern_hash", "")!!))
        }.getOrDefault(false)
        recordAttempt(valid)
        return valid
    }
    private fun recordAttempt(valid: Boolean) {
        val attempts = if (valid) 0 else preferences.getInt("attempts", 0) + 1
        preferences.edit().putInt("attempts", attempts).putLong("retry_at",
            if (attempts >= 5) System.currentTimeMillis() + (30_000L * (1L shl (attempts - 5).coerceAtMost(6))) else 0).commit()
    }
    private fun derive(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, 210_000, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
    private fun encode(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun decode(value: String) = Base64.decode(value, Base64.NO_WRAP)
}
