package dev.texto.privacy
import org.junit.Assert.*
import org.junit.Test
class TwoFingerPullTest {
    @Test fun ordinaryScrollCannotArm() { val g = TwoFingerPull(); g.move(0,300f,1,300f,72f); assertFalse(g.armed) }
    @Test fun bothFingersMustMove() { val g = TwoFingerPull(); g.begin(0,10f,1,20f); g.move(0,100f,1,20f,72f); assertFalse(g.armed); g.move(0,100f,1,110f,72f); assertTrue(g.armed) }
    @Test fun cancellationAndNewGestureClearAccess() { val g = TwoFingerPull(); g.begin(0,0f,1,0f); g.move(0,100f,1,100f,72f); g.reset(); assertFalse(g.armed) }
    @Test fun reversingPullDisarms() { val g = TwoFingerPull(); g.begin(0,0f,1,0f); g.move(0,100f,1,100f,72f); g.move(0,20f,1,20f,72f); assertFalse(g.armed) }
}
