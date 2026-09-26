package dev.texto.privacy

object TrashRetention {
    val choices = listOf(30, 60, 90)
    fun days(value: Int) = value.takeIf { it in choices } ?: 30
    fun expiresAt(deletedAt: Long, retention: Int) = deletedAt + days(retention) * 86_400_000L
    fun expired(deletedAt: Long, retention: Int, now: Long) = deletedAt > 0 && now >= expiresAt(deletedAt, retention)
}
