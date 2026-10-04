package dev.texto.privacy

import android.content.Context
import dev.octoshrimpy.quik.model.Conversation
import dev.octoshrimpy.quik.model.Message
import io.realm.Realm

/** Snapshot at operation start; background jobs always use the ordinary scope. */
class ToolScope(context: Context, realm: Realm, private val protectedOnly: Boolean = false) {
    private val policy = TextoPolicy(context)
    private val extra = policy.entries("protected_tools")
    private val protectedThreads = realm.where(Conversation::class.java).findAll().filter { c ->
        c.archived || c.textoLocked || c.recipients.any { protectedAddress(it.address) }
    }.map { it.id }.toSet()
    fun protectedAddress(address: String): Boolean = policy.decision(address).let {
        it.locked || it.archived || policy.normalize(address) in extra
    }
    private val protectedAddresses = realm.where(Conversation::class.java).findAll().filter { it.id in protectedThreads }
        .flatMap { it.recipients.map { r -> policy.normalize(r.address) } }.toSet()
    fun accepts(message: Message) = accepts(message.threadId, listOf(message.address))
    fun accepts(threadId: Long, addresses: Collection<String>): Boolean =
        (threadId in protectedThreads || addresses.any { protectedAddress(it) || policy.normalize(it) in protectedAddresses }) == protectedOnly
}
