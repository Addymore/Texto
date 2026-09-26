package dev.texto.privacy
import org.junit.Assert.*
import org.junit.Test
class VaultSessionTest {
    @Test fun authenticationWithoutPullDownFails() { assertFalse(VaultSession().authenticate()) }
    @Test fun pullDownStillRequiresAuthentication() { val s = VaultSession(); s.request(); assertFalse(s.unlocked); assertTrue(s.authenticate()); assertTrue(s.unlocked) }
    @Test fun returningToInboxRevokesAccess() { val s = VaultSession(); s.request(); s.authenticate(); s.lock(); assertFalse(s.unlocked); assertFalse(s.authenticate()) }
    @Test fun lateAuthenticationAfterBackgroundIsRejected() { val s = VaultSession(); s.request(); s.lock(); assertFalse(s.authenticate()) }
    @Test fun aNewPullRequiresFreshAuthentication() { val s = VaultSession(); s.request(); s.authenticate(); s.request(); assertFalse(s.unlocked) }
}
