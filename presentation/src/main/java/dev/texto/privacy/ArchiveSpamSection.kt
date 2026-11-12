package dev.texto.privacy

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import dev.octoshrimpy.quik.common.Navigator
import dev.octoshrimpy.quik.model.Conversation
import dev.octoshrimpy.quik.model.Message
import io.realm.Realm
import io.realm.Sort

/**
 * Sticky bottom "Spam messages" section inside Archived messages.
 * Excludes spam conversations from the normal archived list above and shows them
 * under the "Spam messages" section title at the bottom of Archive.
 */
object ArchiveSpamSection {
    private data class SpamThreadItem(
        val id: Long,
        val title: String,
        val addresses: List<String>,
        val snippet: String,
        val count: Long
    )

    fun scanAndApplyAutoSpam(activity: Activity): Int {
        val policy = TextoPolicy(activity)
        val autoEnabled = policy.autoSpamEnabled()
        var newlyFlagged = 0
        runCatching {
            Realm.getDefaultInstance().use { realm ->
                val spamSet = policy.entries("spam")
                val candidates = realm.where(Conversation::class.java)
                    .equalTo("textoLocked", false)
                    .findAll()
                val toUpdate = mutableListOf<Conversation>()
                for (conv in candidates) {
                    val body = conv.lastMessage?.getText().orEmpty()
                    val matched = conv.recipients.any { r ->
                        val norm = policy.normalize(r.address)
                        val alreadySpam = norm in spamSet
                        val intelligent = if (autoEnabled) {
                            policy.isSpamSender(r.address, body, isKnownContact = r.contact != null)
                        } else {
                            alreadySpam
                        }
                        if (intelligent && !alreadySpam) {
                            policy.recordSpamSender(r.address)
                            newlyFlagged++
                        }
                        alreadySpam || intelligent
                    }
                    if (matched && (!conv.archived || conv.blockReason != "Texto archive spam")) {
                        toUpdate.add(conv)
                    }
                }
                if (toUpdate.isNotEmpty()) {
                    realm.executeTransaction {
                        toUpdate.forEach { conv ->
                            conv.archived = true
                            if (!conv.blocked) conv.blockReason = "Texto archive spam"
                        }
                    }
                }
            }
        }
        return newlyFlagged
    }

    private fun loadSpamThreads(activity: Activity): List<SpamThreadItem> {
        val policy = TextoPolicy(activity)
        scanAndApplyAutoSpam(activity)
        val spamSet = policy.entries("spam")
        return runCatching {
            Realm.getDefaultInstance().use { realm ->
                realm.where(Conversation::class.java)
                    .equalTo("textoLocked", false)
                    .greaterThan("messageCount", 0)
                    .sort("lastMessage.date", Sort.DESCENDING)
                    .findAll()
                    .filter { conv ->
                        conv.blockReason == "Texto archive spam" ||
                            ((conv.archived || conv.blocked) &&
                                (conv.blocked || conv.recipients.any { policy.normalize(it.address) in spamSet }))
                    }
                    .map { conv ->
                        SpamThreadItem(
                            id = conv.id,
                            title = conv.getTitle().ifBlank { conv.recipients.joinToString { it.address } },
                            addresses = conv.recipients.map { it.address },
                            snippet = conv.snippet.orEmpty().ifBlank { "Spam message" },
                            count = conv.messageCount.toLong()
                        )
                    }
            }
        }.getOrDefault(emptyList())
    }

    fun hide(container: FrameLayout) {
        if (container.visibility != View.GONE) {
            container.visibility = View.GONE
            container.removeAllViews()
        }
    }

    fun bind(activity: Activity, container: FrameLayout, onChanged: () -> Unit) {
        container.visibility = View.VISIBLE
        container.removeAllViews()
        val policy = TextoPolicy(activity)
        val d = activity.resources.displayMetrics.density
        val ink = TextoAppearance.readableText(activity)
        val accent = TextoAppearance.accent(activity)
        val onAccent = TextoAppearance.onAccent(activity)
        val spamThreads = loadSpamThreads(activity)

        val card = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            background = TextoAppearance.card(activity)
            val padH = (14 * d).toInt()
            val padV = (12 * d).toInt()
            setPadding(padH, padV, padH, padV)
        }

        val headerRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleCol = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        titleCol.addView(TextView(activity).apply {
            text = "Spam messages"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ink)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        })
        titleCol.addView(TextView(activity).apply {
            val autoState = if (policy.autoSpamEnabled()) "Auto-filter ON" else "Auto-filter OFF"
            text = "${spamThreads.size} spam · $autoState"
            textSize = 12f
            setTextColor(ColorUtils.setAlphaComponent(ink, 190))
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        })
        headerRow.addView(titleCol)

        fun pillButton(label: String, filled: Boolean, onClick: () -> Unit) = MaterialButton(activity).apply {
            text = label
            textSize = 11f
            isAllCaps = false
            maxLines = 1
            minWidth = 0
            minimumWidth = 0
            minHeight = (34 * d).toInt()
            minimumHeight = (34 * d).toInt()
            insetTop = 0
            insetBottom = 0
            cornerRadius = (17 * d).toInt()
            setPadding((10 * d).toInt(), 0, (10 * d).toInt(), 0)
            backgroundTintList = ColorStateList.valueOf(
                if (filled) accent else ColorUtils.setAlphaComponent(accent, 36)
            )
            setTextColor(if (filled) onAccent else ink)
            setOnClickListener { onClick() }
        }

        headerRow.addView(
            pillButton(if (policy.autoSpamEnabled()) "Auto: ON" else "Auto: OFF", policy.autoSpamEnabled()) {
                val next = !policy.autoSpamEnabled()
                policy.setAutoSpamEnabled(next)
                val caught = if (next) scanAndApplyAutoSpam(activity) else 0
                Toast.makeText(
                    activity,
                    if (next) "Intelligent auto-spam filter enabled ($caught new spam archived)." else "Auto-spam filter paused.",
                    Toast.LENGTH_SHORT
                ).show()
                onChanged()
                bind(activity, container, onChanged)
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = (6 * d).toInt()
            }
        )

        headerRow.addView(
            pillButton("+ Filter", false) {
                showFilterOptionsDialog(activity, container, onChanged)
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )

        card.addView(headerRow)

        val listBox = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, (8 * d).toInt(), 0, 0)
        }
        if (spamThreads.isEmpty()) {
            listBox.addView(TextView(activity).apply {
                text = "No spam messages in Archive. Tap '+ Filter' or long-press a conversation to mark as spam."
                textSize = 12.5f
                setTextColor(ColorUtils.setAlphaComponent(ink, 180))
                setPadding(0, (4 * d).toInt(), 0, (2 * d).toInt())
            })
        } else {
            spamThreads.forEach { item ->
                val row = LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding((12 * d).toInt(), (8 * d).toInt(), (12 * d).toInt(), (8 * d).toInt())
                    background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = 12f * d
                        setColor(ColorUtils.setAlphaComponent(accent, 22))
                    }
                    isClickable = true
                    isFocusable = true
                    setOnClickListener {
                        showSpamThreadDialog(activity, item, container, onChanged)
                    }
                }
                val topLine = LinearLayout(activity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                topLine.addView(TextView(activity).apply {
                    text = "🚫 ${item.title}"
                    textSize = 14f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(ink)
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                topLine.addView(TextView(activity).apply {
                    text = "${item.count} ${if (item.count == 1L) "msg" else "msgs"}"
                    textSize = 11.5f
                    setTextColor(ColorUtils.setAlphaComponent(ink, 180))
                    setPadding((8 * d).toInt(), 0, 0, 0)
                    maxLines = 1
                })
                row.addView(topLine)
                row.addView(TextView(activity).apply {
                    text = item.snippet
                    textSize = 12.5f
                    setTextColor(ColorUtils.setAlphaComponent(ink, 195))
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.END
                    setPadding(0, (2 * d).toInt(), 0, 0)
                })
                listBox.addView(
                    row,
                    LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        bottomMargin = (6 * d).toInt()
                    }
                )
            }
        }
        val scroller = ScrollView(activity).apply {
            addView(listBox)
        }
        val maxListHeight = if (spamThreads.size > 3) (180 * d).toInt() else ViewGroup.LayoutParams.WRAP_CONTENT
        card.addView(scroller, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, maxListHeight))

        container.addView(
            card,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins((12 * d).toInt(), (4 * d).toInt(), (12 * d).toInt(), (6 * d).toInt())
            }
        )
    }

    private fun showSpamThreadDialog(
        activity: Activity,
        item: SpamThreadItem,
        container: FrameLayout,
        onChanged: () -> Unit
    ) {
        val policy = TextoPolicy(activity)
        val actions = arrayOf(
            "Open conversation",
            "Not spam · Restore to normal Inbox",
            "Keep in Archive (remove spam tag)",
            "Move to Recycle Bin"
        )
        TextoDialogs.builder(activity)
            .setTitle(item.title)
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> activity.startActivity(
                        android.content.Intent(activity, dev.octoshrimpy.quik.feature.compose.ComposeActivity::class.java)
                            .putExtra("threadId", item.id)
                    )
                    1 -> {
                        item.addresses.forEach { policy.unmarkSpamSender(it, alsoUnarchive = true) }
                        Realm.getDefaultInstance().use { realm ->
                            realm.executeTransaction {
                                realm.where(Conversation::class.java).equalTo("id", item.id).findFirst()?.apply {
                                    archived = false
                                    blocked = false
                                    blockReason = null
                                }
                            }
                        }
                        Toast.makeText(activity, "Restored to normal Inbox.", Toast.LENGTH_SHORT).show()
                        onChanged()
                        bind(activity, container, onChanged)
                    }
                    2 -> {
                        item.addresses.forEach { policy.unmarkSpamSender(it, alsoUnarchive = false) }
                        Realm.getDefaultInstance().use { realm ->
                            realm.executeTransaction {
                                realm.where(Conversation::class.java).equalTo("id", item.id).findFirst()?.apply {
                                    archived = true
                                    blocked = false
                                    blockReason = null
                                }
                            }
                        }
                        Toast.makeText(activity, "Kept in normal Archive.", Toast.LENGTH_SHORT).show()
                        onChanged()
                        bind(activity, container, onChanged)
                    }
                    3 -> {
                        Thread {
                            Realm.getDefaultInstance().use { realm ->
                                val ids = realm.where(Message::class.java)
                                    .equalTo("threadId", item.id)
                                    .equalTo("trashedAt", 0L)
                                    .findAll()
                                    .map { it.id }
                                TrashStore(activity).move(ids)
                            }
                            activity.runOnUiThread {
                                if (!activity.isDestroyed) {
                                    Toast.makeText(activity, "Moved to Recycle Bin.", Toast.LENGTH_SHORT).show()
                                    onChanged()
                                    bind(activity, container, onChanged)
                                }
                            }
                        }.start()
                    }
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showFilterOptionsDialog(
        activity: Activity,
        container: FrameLayout,
        onChanged: () -> Unit
    ) {
        val policy = TextoPolicy(activity)
        val options = arrayOf(
            "Select conversations to mark as Spam",
            "Add spam number or sender ID manually",
            "Edit spam phrases (${policy.entries("words").size})",
            "Run intelligent spam scan now"
        )
        TextoDialogs.builder(activity)
            .setTitle("Archive Spam Filtering")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showPickConversationForSpam(activity, container, onChanged)
                    1 -> showAddSpamSenderDialog(activity, container, onChanged)
                    2 -> showEditSpamPhrasesDialog(activity, container, onChanged)
                    3 -> {
                        policy.setAutoSpamEnabled(true)
                        val count = scanAndApplyAutoSpam(activity)
                        onChanged()
                        bind(activity, container, onChanged)
                        Toast.makeText(activity, "Intelligent scan complete: $count new spam conversation(s) archived.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPickConversationForSpam(
        activity: Activity,
        container: FrameLayout,
        onChanged: () -> Unit
    ) {
        val policy = TextoPolicy(activity)
        val spamSet = policy.entries("spam")
        val candidates = Realm.getDefaultInstance().use { realm ->
            realm.where(Conversation::class.java)
                .equalTo("textoLocked", false)
                .greaterThan("messageCount", 0)
                .sort("lastMessage.date", Sort.DESCENDING)
                .findAll()
                .filter { conv -> conv.blockReason != "Texto archive spam" && conv.recipients.none { policy.normalize(it.address) in spamSet } }
                .map { conv ->
                    Triple(
                        conv.id,
                        conv.getTitle().ifBlank { conv.recipients.joinToString { it.address } },
                        conv.recipients.map { it.address }
                    )
                }
        }
        if (candidates.isEmpty()) {
            Toast.makeText(activity, "No additional conversations available to mark as spam.", Toast.LENGTH_SHORT).show()
            return
        }
        val labels = candidates.map { it.second }.toTypedArray()
        val checked = BooleanArray(candidates.size)
        TextoDialogs.builder(activity)
            .setTitle("Mark conversations as Spam")
            .setMultiChoiceItems(labels, checked) { _, index, isChecked ->
                checked[index] = isChecked
            }
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Move to Spam") { _, _ ->
                val selectedIds = mutableListOf<Long>()
                candidates.forEachIndexed { idx, (id, _, addresses) ->
                    if (checked[idx]) {
                        selectedIds.add(id)
                        addresses.forEach { policy.recordSpamSender(it) }
                    }
                }
                if (selectedIds.isNotEmpty()) {
                    Realm.getDefaultInstance().use { realm ->
                        realm.executeTransaction {
                            realm.where(Conversation::class.java)
                                .`in`("id", selectedIds.toTypedArray())
                                .findAll()
                                .forEach {
                                    it.archived = true
                                    if (!it.blocked) it.blockReason = "Texto archive spam"
                                }
                        }
                    }
                    onChanged()
                    bind(activity, container, onChanged)
                    Toast.makeText(activity, "Moved ${selectedIds.size} conversation(s) to Spam messages.", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun showAddSpamSenderDialog(
        activity: Activity,
        container: FrameLayout,
        onChanged: () -> Unit
    ) {
        val policy = TextoPolicy(activity)
        val field = TextInputEditText(activity).apply {
            hint = "Phone number or sender name (e.g. +18005550199)"
            inputType = InputType.TYPE_CLASS_TEXT
        }
        TextoDialogs.builder(activity)
            .setTitle("Add spam sender")
            .setMessage("Messages from this sender will stay archived in the Spam messages section even when new messages arrive.")
            .setView(field)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Add to Spam") { _, _ ->
                val raw = field.text?.toString()?.trim().orEmpty()
                if (raw.isNotEmpty()) {
                    policy.recordSpamSender(raw)
                    PrivacyAuthDialogs.applyRulesToRealm(activity)
                    onChanged()
                    bind(activity, container, onChanged)
                }
            }
            .show()
    }

    private fun showEditSpamPhrasesDialog(
        activity: Activity,
        container: FrameLayout,
        onChanged: () -> Unit
    ) {
        val policy = TextoPolicy(activity)
        val field = TextInputEditText(activity).apply {
            minLines = 4
            maxLines = 8
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setText(policy.entries("words").sorted().joinToString("\n"))
            hint = "One spam phrase per line"
        }
        TextoDialogs.builder(activity)
            .setTitle("Spam phrases")
            .setMessage("Messages containing any of these phrases (one per line) will be filtered automatically.")
            .setView(field)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                policy.save("words", field.text?.toString()?.lines().orEmpty())
                PrivacyAuthDialogs.applyRulesToRealm(activity)
                onChanged()
                bind(activity, container, onChanged)
            }
            .show()
    }
}
