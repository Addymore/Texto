package dev.texto.privacy

/** Deterministic, offline rules. Exact allow entries override spam rules, never privacy. */
object RuleEngine {
    data class Decision(val archived: Boolean, val locked: Boolean, val blocked: Boolean)
    fun decide(address: String, body: String, locked: Set<String>, archived: Set<String>,
               blocked: Set<String>, allowed: Set<String>, prefixes: Set<String>, words: Set<String>): Decision {
        val private = address in locked
        val spam = address !in allowed && (address in blocked ||
            prefixes.any { it.isNotBlank() && address.startsWith(it) } ||
            words.any { it.isNotBlank() && body.contains(it, ignoreCase = true) })
        return Decision(private || address in archived, private, spam)
    }
}
