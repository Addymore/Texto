package dev.texto.privacy

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import dev.octoshrimpy.quik.R
import dev.octoshrimpy.quik.model.Conversation
import io.realm.Realm

class ContactProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.TextoTheme)
        super.onCreate(savedInstanceState)
        val id = intent.getLongExtra("threadId", 0)
        val conversation = Realm.getDefaultInstance().use { realm ->
            realm.where(Conversation::class.java).equalTo("id", id).findFirst()?.let { realm.copyFromRealm(it) }
        }
        if (conversation == null) { finish(); return }
        val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 56, 48, 32) }
        fun label(value: String, size: Float) { panel.addView(TextView(this).apply { text = value; textSize = size; setPadding(0, 16, 0, 16) }) }
        fun button(value: String, action: () -> Unit) { panel.addView(MaterialButton(this).apply { text = value; isAllCaps = false; setOnClickListener { action() } }) }
        label(conversation.getTitle().take(1).uppercase(), 64f)
        label(conversation.getTitle(), 30f)
        label(if (conversation.recipients.size > 1) "Group profile" else "Contact profile", 16f)
        conversation.recipients.forEach { recipient ->
            label(recipient.getDisplayName(), 22f)
            label(recipient.address, 16f)
            button("Call ${recipient.getDisplayName()}") { startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", recipient.address, null))) }
            button("Save or edit contact") {
                startActivity(Intent(Intent.ACTION_INSERT_OR_EDIT).setType("vnd.android.cursor.item/contact")
                    .putExtra("phone", recipient.address))
            }
            button("Privacy rules for this number") {
                startActivity(Intent(this, ProtectionActivity::class.java).putExtra("address", recipient.address))
            }
        }
        button("Media, notifications & conversation color") {
            startActivity(Intent(this, dev.octoshrimpy.quik.feature.conversationinfo.ConversationInfoActivity::class.java).putExtra("threadId", id))
        }
        button("Back to conversation") { finish() }
        setContentView(ScrollView(this).apply { addView(panel) })
    }
}
