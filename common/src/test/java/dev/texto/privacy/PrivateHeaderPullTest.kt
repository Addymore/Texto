package dev.texto.privacy

import org.junit.Assert.*
import org.junit.Test

class PrivateHeaderPullTest {
    @Test fun onlyStartedPullCanArm() {
        val pull = PrivateHeaderPull()
        pull.move(0f, 200f, 96f); assertFalse(pull.armed)
        pull.begin(10f, 20f)
        pull.move(10f, 100f, 96f); assertFalse(pull.armed)
        pull.move(10f, 120f, 96f); assertTrue(pull.armed)
    }
    @Test fun reversalAndCancellationDisarm() {
        val pull = PrivateHeaderPull(); pull.begin(0f, 0f)
        pull.move(0f, 100f, 96f); assertTrue(pull.armed)
        pull.move(0f, 10f, 96f); assertFalse(pull.armed)
        pull.reset(); pull.move(0f, 200f, 96f); assertFalse(pull.armed)
    }
    @Test fun sidewaysMotionCancelsUntilNextTouch() {
        val pull = PrivateHeaderPull(); pull.begin(0f, 0f)
        pull.move(60f, 100f, 96f); assertFalse(pull.armed)
        pull.move(0f, 150f, 96f); assertFalse(pull.armed)
    }
}
