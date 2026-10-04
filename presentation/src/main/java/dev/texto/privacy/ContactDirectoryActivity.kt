package dev.texto.privacy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dev.octoshrimpy.quik.R

class ContactDirectoryActivity : AppCompatActivity() {
    data class Entry(val name: String, val number: String, val photo: String?, val lookup: String, val id: Long)
    private var entries = emptyList<Entry>()
    private var visibleEntries = emptyList<Entry>()
    private lateinit var adapter: PeopleAdapter
    private lateinit var status: TextView
    private val settings by lazy { TextoPolicy(this).preferences }
    private var showPhotos = true
    private val dark get() = resources.configuration.uiMode and 0x30 == 0x20
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.TextoTheme); TextoTheme.apply(this); super.onCreate(savedInstanceState)
        showPhotos = settings.getBoolean("contact_photos", true)
        render()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), 7)
        else load()
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) load()
        else status.text = "Allow Contacts access in Android settings to see your people."
    }
    private fun render() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), 0, dp(20), dp(12)) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.BOTTOM; setPadding(dp(8), 0, dp(8), dp(18)) }
        header.addView(TextView(this).apply { text = "Contacts"; textSize = 36f; setTypeface(null, Typeface.BOLD) })
        status = TextView(this).apply { text = "Your people, a message away"; textSize = 14f; setPadding(0, dp(6), 0, 0) }
        header.addView(status)
        val headerHeight = if (resources.configuration.orientation == 2) dp(88) else (resources.displayMetrics.heightPixels * .25f).toInt().coerceAtMost(dp(230))
        root.addView(header, LinearLayout.LayoutParams(-1, headerHeight))
        val searchBox = TextInputLayout(this).apply { boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE; setBoxCornerRadii(dp(24).toFloat(),dp(24).toFloat(),dp(24).toFloat(),dp(24).toFloat()); hint = "Search names or numbers" }
        val search = TextInputEditText(searchBox.context).apply { setSingleLine(); textSize = 16f }
        searchBox.addView(search); root.addView(searchBox)
        root.addView(MaterialSwitch(this).apply { text = "Contact photos"; isChecked = showPhotos; setOnCheckedChangeListener { _, value -> showPhotos = value; settings.edit().putBoolean("contact_photos", value).apply(); adapter.notifyDataSetChanged() } })
        val list = RecyclerView(this).apply { layoutManager = LinearLayoutManager(this@ContactDirectoryActivity); clipToPadding = false; setPadding(0, dp(4), 0, dp(16)) }
        adapter = PeopleAdapter(); list.adapter = adapter
        TextoAppearance.smooth(list); TextoAppearance.refreshRate(this)
        // The title contracts as the list scrolls, bringing more contacts into view.
        list.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(view: RecyclerView, dx: Int, dy: Int) {
                if (dy != 0) header.layoutParams = header.layoutParams.apply { height = (height - dy).coerceIn(dp(80), headerHeight.coerceAtLeast(dp(80))) }
            }
        })
        root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(TextoTabBar(this).apply { select(-1); onTabSelected = { tab ->
            when (tab) {
                0 -> { startActivity(Intent(this@ContactDirectoryActivity, dev.octoshrimpy.quik.feature.main.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)); finish() }
                1 -> startActivity(Intent(this@ContactDirectoryActivity, ThemesActivity::class.java))
                2 -> startActivity(Intent(this@ContactDirectoryActivity, dev.octoshrimpy.quik.feature.settings.SettingsActivity::class.java))
            }
        } }, LinearLayout.LayoutParams(-1, dp(66)))
        setContentView(root)
        search.doAfterTextChanged { filter(it.toString()) }
    }
    private fun filter(query: String) {
        visibleEntries = entries.filter { it.name.contains(query, true) || it.number.contains(query, true) }
        adapter.notifyDataSetChanged()
        status.text = when { entries.isEmpty() -> "No contacts with phone numbers yet"; visibleEntries.isEmpty() -> "No matching contacts"; else -> "${visibleEntries.size} numbers · tap a photo for contact details" }
    }
    private fun load() {
        Thread {
            val result = runCatching {
                val rows = mutableListOf<Entry>()
                contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf("display_name", "data1", "photo_thumb_uri", "lookup", "contact_id"), null, null, "display_name COLLATE LOCALIZED ASC")?.use { cursor ->
                    while (cursor.moveToNext()) rows += Entry(cursor.getString(0).orEmpty(), cursor.getString(1).orEmpty(), cursor.getString(2), cursor.getString(3).orEmpty(), cursor.getLong(4))
                }
                rows.distinctBy { Pair(it.id, it.number) }
            }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                result.onSuccess { entries = it; filter("") }.onFailure { status.text = "Could not load contacts. Check Contacts permission and reopen this screen." }
            }
        }.start()
    }
    private inner class PeopleAdapter : RecyclerView.Adapter<PersonHolder>() {
        override fun getItemCount() = visibleEntries.size
        override fun onCreateViewHolder(parent: ViewGroup, type: Int): PersonHolder {
            val row = LinearLayout(this@ContactDirectoryActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(14), dp(14), dp(14), dp(14)); background = TextoAppearance.card(context) }
            row.layoutParams = RecyclerView.LayoutParams(-1, -2).apply { bottomMargin = dp(8) }
            val avatar = FrameLayout(this@ContactDirectoryActivity)
            val initial = TextView(this@ContactDirectoryActivity).apply { textSize = 21f; gravity = Gravity.CENTER; setTextColor(TextoTheme.onColor(TextoAppearance.accent(this@ContactDirectoryActivity))); background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(TextoAppearance.accent(this@ContactDirectoryActivity)) } }
            val image = ImageView(this@ContactDirectoryActivity).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
            avatar.addView(initial, FrameLayout.LayoutParams(-1,-1)); avatar.addView(image, FrameLayout.LayoutParams(-1,-1)); row.addView(avatar, LinearLayout.LayoutParams(dp(54),dp(54)))
            val words = LinearLayout(this@ContactDirectoryActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14),0,dp(4),0) }
            val name = TextView(this@ContactDirectoryActivity).apply { textSize = 17f; setTypeface(null,Typeface.BOLD); maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END }
            val number = TextView(this@ContactDirectoryActivity).apply { textSize = 14f; setPadding(0,dp(4),0,0); setTextColor(if (dark) 0xFFBDC1CE.toInt() else 0xFF676E7E.toInt()) }
            words.addView(name); words.addView(number); row.addView(words,LinearLayout.LayoutParams(0,-2,1f))
            return PersonHolder(row, avatar, initial, image, name, number)
        }
        override fun onBindViewHolder(holder: PersonHolder, position: Int) {
            val person = visibleEntries[position]
            holder.name.text = person.name.ifBlank { person.number }; holder.number.text = person.number
            holder.initial.text = person.name.split(' ').filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() }.ifEmpty { "#" }
            Glide.with(holder.image).clear(holder.image); holder.image.setImageDrawable(null)
            if (showPhotos && !person.photo.isNullOrBlank()) Glide.with(holder.image).load(Uri.parse(person.photo)).circleCrop().into(holder.image)
            holder.avatar.contentDescription = "View contact details for ${person.name}"
            holder.avatar.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.getLookupUri(person.id, person.lookup))) }
            holder.itemView.setOnClickListener { startActivity(Intent(this@ContactDirectoryActivity, dev.octoshrimpy.quik.feature.compose.ComposeActivity::class.java).setAction(Intent.ACTION_SENDTO).setData(Uri.fromParts("smsto",person.number,null))) }
        }
    }
    private class PersonHolder(view: View, val avatar: FrameLayout, val initial: TextView, val image: ImageView, val name: TextView, val number: TextView) : RecyclerView.ViewHolder(view)
}
