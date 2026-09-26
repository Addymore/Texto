package dev.texto.privacy

/** An ephemeral session, only opened after an explicit pull-down and authentication. */
class VaultSession {
    var pending = false
        private set
    var unlocked = false
        private set
    fun request() { unlocked = false; pending = true }
    fun authenticate(): Boolean {
        if (!pending) return false
        pending = false; unlocked = true
        return true
    }
    fun lock() { pending = false; unlocked = false }
}
