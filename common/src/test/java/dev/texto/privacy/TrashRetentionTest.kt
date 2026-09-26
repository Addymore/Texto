package dev.texto.privacy

import org.junit.Assert.*
import org.junit.Test

class TrashRetentionTest {
    @Test fun expiresExactlyAtEachDeadline() { for (days in TrashRetention.choices) {
        val deadline = 1000L + days * 86_400_000L
        assertFalse(TrashRetention.expired(1000, days, deadline - 1))
        assertTrue(TrashRetention.expired(1000, days, deadline))
    } }
    @Test fun liveMessagesNeverExpire() { assertFalse(TrashRetention.expired(0, 30, Long.MAX_VALUE)) }
    @Test fun changingRetentionUsesOriginalDeletionDate() {
        val now = 1000L + 45 * 86_400_000L
        assertTrue(TrashRetention.expired(1000, 30, now)); assertFalse(TrashRetention.expired(1000, 60, now))
    }
    @Test fun invalidChoiceFallsBackToThirtyDays() { assertEquals(30, TrashRetention.days(-1)) }
}
