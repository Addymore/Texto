package dev.texto.privacy

/** Deterministic, offline rules. Exact allow entries override spam rules, never privacy. */
object RuleEngine {
    data class Decision(
        val archived: Boolean,
        val locked: Boolean,
        val blocked: Boolean,
        val spam: Boolean = false
    )

    private val otpRegex = Regex(
        "\\b(otp|verification code|security code|login code|passcode|one-time password|auth code|2fa)\\b",
        RegexOption.IGNORE_CASE
    )
    private val otpDigitsRegex = Regex("\\b\\d{4,8}\\b")
    private val highConfidenceScamPhrases = listOf(
        "congratulations you have won",
        "congrats! you won",
        "claim your prize",
        "claim your reward",
        "unclaimed reward",
        "lottery winner",
        "cash prize",
        "crypto giveaway",
        "bitcoin giveaway",
        "guaranteed loan",
        "instant loan approved",
        "pre-approved loan",
        "urgent: your account",
        "account has been suspended",
        "verify your bank",
        "unpaid toll",
        "package delivery suspended",
        "customs fee required",
        "free gift card",
        "click here to claim",
        "act now before it expires",
        "earn $",
        "work from home and earn"
    )
    private val promoOptOutPhrases = listOf(
        "reply stop",
        "text stop to",
        "txt stop to",
        "stop to opt out",
        "stop to unsubscribe",
        "opt-out",
        "unsubscribe at",
        "t&c apply",
        "terms and conditions apply",
        "limited time offer",
        "flash sale",
        "exclusive deal"
    )
    private val suspiciousUrlRegex = Regex(
        "(https?://(\\d{1,3}\\.){3}\\d{1,3}|\\b(bit\\.ly|tinyurl\\.com|t\\.co|cutt\\.ly|is\\.gd|rb\\.gy|shorturl\\.at)/\\S+|https?://\\S+\\.(xyz|top|click|buzz|loan|win|vip|icu|shop)(/|\\b))",
        RegexOption.IGNORE_CASE
    )
    private val urgencyLureRegex = Regex(
        "\\b(urgent|verify|suspended|locked|claim|winner|won|prize|free|bonus|expire|alert|limited|offer|discount|cashback)\\b",
        RegexOption.IGNORE_CASE
    )

    fun extractOtpCode(body: String): String? {
        if (body.isBlank()) return null
        if (!otpRegex.containsMatchIn(body)) return null
        return otpDigitsRegex.find(body)?.value
    }

    /**
     * Offline heuristic spam detector for unknown senders.
     * Preserves legitimate 2FA/OTP messages while catching phishing links, scams, and bulk promos.
     */
    fun isIntelligentSpam(address: String, body: String, isKnownContact: Boolean = false): Boolean {
        if (isKnownContact || body.isBlank()) return false
        val lower = body.lowercase()
        if (highConfidenceScamPhrases.any { lower.contains(it) }) return true
        // Preserve legitimate OTP codes unless accompanied by a high-confidence scam phrase above
        if (otpRegex.containsMatchIn(body) && otpDigitsRegex.containsMatchIn(body)) return false
        if (suspiciousUrlRegex.containsMatchIn(body) && urgencyLureRegex.containsMatchIn(body)) return true
        if (promoOptOutPhrases.any { lower.contains(it) }) return true
        val isAlphanumericOrShortSender = address.any { it.isLetter() } || (address.filter { it.isDigit() }.length in 3..6)
        if (isAlphanumericOrShortSender && urgencyLureRegex.findAll(body).count() >= 2) return true
        return false
    }

    fun decide(
        address: String,
        body: String,
        locked: Set<String>,
        archived: Set<String>,
        blocked: Set<String>,
        allowed: Set<String>,
        prefixes: Set<String>,
        words: Set<String>,
        spamSenders: Set<String> = emptySet(),
        autoSpam: Boolean = false,
        isKnownContact: Boolean = false
    ): Decision {
        val private = address in locked
        val matchedRuleBlock = address !in allowed && (
            address in blocked ||
            prefixes.any { it.isNotBlank() && address.startsWith(it) } ||
            words.any { it.isNotBlank() && body.contains(it, ignoreCase = true) }
        )
        val matchedArchiveSpam = !private && address !in allowed && (
            address in spamSenders ||
            (autoSpam && isIntelligentSpam(address, body, isKnownContact))
        )
        return Decision(
            archived = private || address in archived || matchedArchiveSpam,
            locked = private,
            blocked = matchedRuleBlock,
            spam = matchedArchiveSpam || matchedRuleBlock
        )
    }
}
