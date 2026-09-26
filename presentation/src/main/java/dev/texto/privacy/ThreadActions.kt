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
        MaterialAlertDialogBuilder(activity).setTitle("Conversation actions")
            .setItems(arrayOf("Archive", "Lock & archive this number", "Move to recycle bin", "Select multiple")) { _, which ->
                if (which == 3) { select(); return@setItems }
                val policy = TextoPolicy(activity)
                if (which == 1 && !policy.hasPin()) {
                    Toast.makeText(activity,"Create a privacy PIN, then long-press the conversation to lock it.",Toast.LENGTH_LONG).show()
                    activity.startActivity(Intent(activity,ProtectionActivity::class.java)); return@setItems
                }
                Thread {
                    val result = runCatching {
                        Realm.getDefaultInstance().use { realm ->
                            val thread = realm.where(Conversation::class.java).equalTo("id",id).findFirst() ?: return@use
                            when(which) {
                                0 -> realm.executeTransaction { thread.archived = true }
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
}
