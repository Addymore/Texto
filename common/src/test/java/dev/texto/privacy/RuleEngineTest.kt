package dev.texto.privacy

import org.junit.Assert.*
import org.junit.Test

class RuleEngineTest {
    private fun decide(address: String = "+250788123456", text: String = "hello", lock: Set<String> = emptySet(),
        archive: Set<String> = emptySet(), block: Set<String> = emptySet(), allow: Set<String> = emptySet(),
        prefix: Set<String> = emptySet(), words: Set<String> = emptySet()) =
        RuleEngine.decide(address, text, lock, archive, block, allow, prefix, words)
    @Test fun lockedNumbersRemainArchivedForEveryMessage() {
        listOf("hello", "new message", "photo attachment").forEach {
            val d = decide(text = it, lock = setOf("+250788123456"))
            assertTrue(d.locked); assertTrue(d.archived); assertFalse(d.blocked)
        }
    }
    @Test fun archiveDoesNotRequireLock() { val d = decide(archive = setOf("+250788123456")); assertTrue(d.archived); assertFalse(d.locked) }
    @Test fun allowlistOverridesSpamButNeverLock() {
        val d = decide(lock = setOf("+250788123456"), allow = setOf("+250788123456"), prefix = setOf("+250"))
        assertTrue(d.locked); assertFalse(d.blocked)
    }
    @Test fun prefixAndCaseInsensitiveContentMatch() {
        assertTrue(decide(prefix = setOf("+250788")).blocked)
        assertTrue(decide(text = "CLAIM PRIZE now", words = setOf("claim prize")).blocked)
        assertFalse(decide(prefix = setOf("+250789"), words = setOf("", "lottery")).blocked)
    }
    @Test fun exactRulesDoNotMatchSuffixes() { assertFalse(decide(block = setOf("788123456")).blocked) }
    @Test fun emptyRulesNeverBlock() { assertFalse(decide(prefix = setOf(""), words = setOf(" ")).blocked) }
    @Test fun intelligentSpamCatchesScamsAndPreservesOtpAndContacts() {
        assertTrue(RuleEngine.isIntelligentSpam("+18005559999", "CONGRATULATIONS YOU HAVE WON a $1000 reward! Click here to claim"))
        assertTrue(RuleEngine.isIntelligentSpam("PROMO", "Flash sale! 50% off today. Reply STOP to unsubscribe"))
        assertTrue(RuleEngine.isIntelligentSpam("+18005550000", "Urgent: verify your account at https://bit.ly/3xYzAbc before it is suspended"))
        assertFalse(RuleEngine.isIntelligentSpam("+18005559999", "CONGRATULATIONS YOU HAVE WON", isKnownContact = true))
        assertFalse(RuleEngine.isIntelligentSpam("GOOGLE", "Your verification code is 482910. Do not share this OTP."))
        assertEquals("482910", RuleEngine.extractOtpCode("Your verification code is 482910. Do not share this OTP."))
    }
    @Test fun spamSendersStayArchivedEvenOnNewNormalMessage() {
        val d = RuleEngine.decide(
            address = "+250788999000",
            body = "Hey are you still there?",
            locked = emptySet(),
            archived = emptySet(),
            blocked = emptySet(),
            allowed = emptySet(),
            prefixes = emptySet(),
            words = emptySet(),
            spamSenders = setOf("+250788999000"),
            autoSpam = true
        )
        assertTrue(d.archived)
        assertTrue(d.spam)
        assertFalse(d.locked)
    }
}
