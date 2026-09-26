package dev.texto.privacy

import android.content.Context
import android.net.Uri
import androidx.work.*
import dev.octoshrimpy.quik.model.Conversation
import dev.octoshrimpy.quik.model.Message
import io.realm.Realm
import io.realm.Sort
import java.util.concurrent.TimeUnit

/** Soft deletion preserves the provider row (including MMS parts) until expiry. */
class TrashStore(context: Context) {
    private val app = context.applicationContext
    private val marks = app.getSharedPreferences("texto_trash", Context.MODE_PRIVATE)
    private fun key(message: Message) = if (message.contentId > 0) "${message.type}:${message.contentId}:${message.date}:${message.threadId}" else "local:${message.id}"
    fun deletedAt(message: Message) = marks.getLong(key(message), 0)

    fun reconcile(realm: Realm) {
        val changed = mutableSetOf<Long>()
        // Only visit tombstones and existing bin rows on startup, not the entire message history.
        realm.where(Message::class.java).greaterThan("trashedAt", 0L).findAll().toList().forEach { message ->
            val stamp = deletedAt(message)
            if (message.trashedAt != stamp) { message.trashedAt = stamp; changed += message.threadId }
        }
        marks.all.forEach { (key, value) ->
            val parts = key.split(':')
            val query = realm.where(Message::class.java)
            val message = when {
                parts.size == 4 -> query.equalTo("type",parts[0]).equalTo("contentId",parts[1].toLong()).equalTo("date",parts[2].toLong()).equalTo("threadId",parts[3].toLong()).findFirst()
                parts.size == 2 && parts[0] == "local" -> query.equalTo("id",parts[1].toLong()).findFirst()
                else -> null
            }
            if (message != null && value is Long && message.trashedAt != value) { message.trashedAt = value; changed += message.threadId }
        }
        refresh(realm,changed)
    }

    fun move(ids: Collection<Long>) {
        if (ids.isEmpty()) return
        synchronized(lock) {
            Realm.getDefaultInstance().use { realm ->
                realm.refresh()
                val messages = realm.where(Message::class.java).`in`("id", ids.toTypedArray()).equalTo("trashedAt", 0L).findAll()
                val threads = messages.map { it.threadId }.distinct()
                val now = System.currentTimeMillis()
                val edit = marks.edit()
                messages.forEach { edit.putLong(key(it), now) }
                check(edit.commit()) // Persist before hiding; a resync must not resurrect deleted messages.
                realm.executeTransaction {
                    messages.forEach { it.trashedAt = now }
                    refresh(realm, threads)
                }
            }
        }
    }

    fun restore(ids: Collection<Long>) {
        synchronized(lock) {
            Realm.getDefaultInstance().use { realm ->
                val messages = realm.where(Message::class.java).`in`("id", ids.toTypedArray()).greaterThan("trashedAt", 0L).findAll()
                val threads = messages.map { it.threadId }.distinct()
                val edit = marks.edit(); messages.forEach { edit.remove(key(it)) }; check(edit.commit())
                realm.executeTransaction { messages.forEach { it.trashedAt = 0 }; refresh(realm, threads) }
            }
        }
    }

    fun purgeExpired(): Boolean = synchronized(lock) {
        val days = TrashRetention.days(TextoPolicy(app).preferences.getInt("bin_days", 30))
        var success = true
        Realm.getDefaultInstance().use { realm ->
            val expired = realm.where(Message::class.java).greaterThan("trashedAt", 0L).findAll()
                .filter { TrashRetention.expired(it.trashedAt, days, System.currentTimeMillis()) }
            for (message in expired) {
                try {
                    val uri = message.getUri()
                    if (uri != Uri.EMPTY) {
                        app.contentResolver.delete(uri, null, null)
                        val removed = app.contentResolver.query(uri,arrayOf("_id"),null,null,null)?.use { !it.moveToFirst() } ?: false
                        check(removed) { "Provider retained the message; retry cleanup later" }
                    }
                    val mark = key(message); val thread = message.threadId
                    realm.executeTransaction {
                        message.parts.deleteAllFromRealm()
                        message.deleteFromRealm()
                        refresh(realm, listOf(thread))
                    }
                    check(marks.edit().remove(mark).commit())
                } catch (_: Exception) { success = false }
            }
        }
        success
    }

    companion object {
        private val lock = Any()
        /** Call inside the caller's write transaction. Counters are cached, never queried while scrolling. */
        fun refresh(realm: Realm, threads: Collection<Long>? = null) {
            val query = realm.where(Conversation::class.java)
            if (threads != null) {
                if (threads.isEmpty()) return
                query.`in`("id", threads.toTypedArray())
            }
            query.findAll().forEach { conversation ->
                val live = realm.where(Message::class.java).equalTo("threadId", conversation.id).equalTo("trashedAt", 0L).equalTo("isEmojiReaction", false)
                conversation.messageCount = live.count()
                conversation.unreadCount = realm.where(Message::class.java).equalTo("threadId", conversation.id).equalTo("trashedAt", 0L).equalTo("isEmojiReaction", false).equalTo("read", false).count()
                conversation.lastMessage = live.sort("date", Sort.DESCENDING).findFirst()
            }
        }
        fun schedule(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("texto-bin-expiry", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<TrashExpiryWorker>(24, TimeUnit.HOURS).build())
            WorkManager.getInstance(context).enqueueUniqueWork("texto-bin-startup", ExistingWorkPolicy.KEEP, OneTimeWorkRequestBuilder<TrashExpiryWorker>().build())
        }
    }
}

class TrashExpiryWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result = try {
        if (TrashStore(applicationContext).purgeExpired()) Result.success() else Result.retry()
    } catch (_: Exception) { Result.retry() }
}
