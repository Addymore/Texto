package dev.texto.privacy

import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.octoshrimpy.quik.model.Conversation
import io.realm.Realm

object ThreadActions {
    fun show(activity: Activity, id: Long, select: () -> Unit) {
        val state = Realm.getDefaultInstance().use { r -> r.where(Conversation::class.java).equalTo("id",id).findFirst()?.let { it.textoLocked to it.archived } } ?: return
        if(state.first && !PrivacyGate.unlocked) return
        val labels = if(state.first) arrayOf("Move to normal messages", "Move to archive (remove lock)", "Move to recycle bin")
            else if(state.second) arrayOf("Move to normal messages", "Lock & archive this number", "Move to recycle bin", "Select multiple")
            else arrayOf("Archive", "Lock & archive this number", "Move to recycle bin", "Select multiple")
        dev.texto.privacy.TextoDialogs.builder(activity).setTitle("Conversation actions")
            .setItems(labels) { _, which ->
                if (which == 3) { select(); return@setItems }
                if ((state.first && which < 2) || (state.second && which == 0)) {
                    TextoDialogs.builder(activity).setTitle(labels[which]).setMessage("This changes the number rules for every conversation containing these participants, including future messages.")
                        .setNegativeButton("Cancel",null).setPositiveButton("Move") { _,_ -> moveOut(activity,id,state.first && which == 1) }.show()
                    return@setItems
                }
                val policy = TextoPolicy(activity)
                if (which == 1 && !policy.hasPin()) {
                    Toast.makeText(activity,"Create a privacy PIN, then long-press the conversation to lock it.",Toast.LENGTH_LONG).show()
                    activity.startActivity(Intent(activity,ProtectionActivity::class.java)); return@setItems
                }
                Thread {
                    val result = runCatching {
                        Realm.getDefaultInstance().use { realm ->
                            val thread = realm.where(Conversation::class.java).equalTo("id",id).findFirst() ?: return@use
                            check(!thread.textoLocked || PrivacyGate.unlocked)
                            when(which) {
                                0 -> { policy.save("archived",policy.entries("archived") + thread.recipients.map { it.address }); realm.executeTransaction { thread.archived = true } }
                                1 -> {
                                    policy.save("locked",policy.entries("locked") + thread.recipients.map { it.address })
                                    realm.executeTransaction {
                                        realm.where(Conversation::class.java).findAll().forEach { conversation ->
                                            if (conversation.recipients.any { policy.decision(it.address).locked }) { conversation.textoLocked=true; conversation.archived=true
                                                if (android.os.Build.VERSION.SDK_INT >= 26) {
                                                    val manager = activity.getSystemService(NotificationManager::class.java)
                                                    manager.getNotificationChannel("notifications_${conversation.id}")?.let { channel -> channel.name="Protected conversation"; channel.description="Content hidden by Texto"; manager.createNotificationChannel(channel) }
                                                } }
                                        }
                                    }
                                }
                                2 -> {
                                    val ids = realm.where(dev.octoshrimpy.quik.model.Message::class.java).equalTo("threadId",id).equalTo("trashedAt",0L).findAll().map { it.id }
                                    realm.executeTransaction { thread.draft="" }
                                    TrashStore(activity).move(ids)
                                }
                            }
                        }
                        activity.getSystemService(NotificationManager::class.java).cancelAll()
                        if (android.os.Build.VERSION.SDK_INT >= 25 && which == 1) {
                            val manager = activity.getSystemService(android.content.pm.ShortcutManager::class.java)
                            manager.removeAllDynamicShortcuts(); manager.disableShortcuts(manager.pinnedShortcuts.map { it.id })
                        }
                        activity.sendBroadcast(Intent(activity,dev.octoshrimpy.quik.feature.widget.WidgetProvider::class.java).setAction("${activity.packageName}.action.ACTION_NOTIFY_DATASET_CHANGED"))
                    }
                    activity.runOnUiThread { if (!activity.isDestroyed) Toast.makeText(activity,
                        if(result.isSuccess) arrayOf("Archived", "Conversation locked.","Moved to recycle bin")[which] else "Could not apply the action. Please try again.",Toast.LENGTH_LONG).show() }
                }.start()
            }.show()
    }
    private fun moveOut(activity: Activity, id: Long, archive: Boolean) {
        Thread {
            val result=runCatching {
                Realm.getDefaultInstance().use { r ->
                    val thread=r.where(Conversation::class.java).equalTo("id",id).findFirst() ?: error("Conversation missing")
                    val policy=TextoPolicy(activity)
                    check(!thread.textoLocked || PrivacyGate.unlocked)
                    val numbers=thread.recipients.map { policy.normalize(it.address) }.toSet()
                    policy.save("locked",policy.entries("locked")-numbers)
                    policy.save("archived",if(archive) policy.entries("archived")+numbers else policy.entries("archived")-numbers)
                    r.executeTransaction {
                        r.where(Conversation::class.java).findAll().filter { c -> c.recipients.any { policy.normalize(it.address) in numbers } }.forEach { c ->
                            c.textoLocked=c.recipients.any { policy.decision(it.address).locked }
                            c.archived=c.recipients.any { policy.decision(it.address).archived }
                        }
                    }
                }
                activity.getSystemService(NotificationManager::class.java).cancelAll()
                activity.sendBroadcast(Intent(activity,dev.octoshrimpy.quik.feature.widget.WidgetProvider::class.java).setAction("${activity.packageName}.action.ACTION_NOTIFY_DATASET_CHANGED"))
            }
            activity.runOnUiThread { if(!activity.isDestroyed) Toast.makeText(activity,if(result.isSuccess) "Conversation moved" else "Could not move conversation. Unlock and try again.",Toast.LENGTH_LONG).show() }
        }.start()
    }

}
