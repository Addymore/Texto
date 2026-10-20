package dev.texto.privacy

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dagger.android.AndroidInjection
import dev.octoshrimpy.quik.common.base.QkThemedActivity
import dev.octoshrimpy.quik.databinding.ConversationListItemBinding
import dev.octoshrimpy.quik.model.Recipient
import dev.octoshrimpy.quik.model.Contact
import dev.octoshrimpy.quik.util.NightModeManager
import javax.inject.Inject

class ThemesActivity : QkThemedActivity() {
    @Inject lateinit var nightModeManager: NightModeManager
    private var imageKey="background_image"
    override fun onSaveInstanceState(outState: Bundle) { outState.putString("image_key",imageKey); super.onSaveInstanceState(outState) }
    private val imagePicker=registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if(uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)
                ArtworkCrop.show(this,uri,imageKey) { render() }
            } catch(e: SecurityException) { Toast.makeText(this,"Could not keep access to this image. Choose another image.",Toast.LENGTH_LONG).show() }
        }
    }
    private fun pickImage(key: String) { imageKey=key; imagePicker.launch(arrayOf("image/*")) }
    private fun customText(title: String,key: String,default: String,max: Int) {
        val field=TextInputEditText(this).apply { setSingleLine(); filters=arrayOf(android.text.InputFilter.LengthFilter(max)); setText(TextoAppearance.prefs(this@ThemesActivity).getString(key,default)); setPadding(dp(24),dp(16),dp(24),dp(16)) }
        dialog=dev.texto.privacy.TextoDialogs.builder(this).setTitle(title).setView(field).setNegativeButton("Cancel",null).setPositiveButton("Save") { _,_ -> TextoAppearance.prefs(this).edit().putString(key,field.text.toString().trim()).apply(); render() }.show()
    }
    private var scroller: ScrollView? = null
    private var dialog: androidx.appcompat.app.AlertDialog? = null
    private fun dp(n: Int) = (n*resources.displayMetrics.density).toInt()
    override fun onCreate(state: Bundle?) { AndroidInjection.inject(this); super.onCreate(state); imageKey=state?.getString("image_key") ?: "background_image"; render() }
    override fun onPause() { dialog?.dismiss(); super.onPause() }
    private fun render() {
        val position = scroller?.scrollY ?: 0
        val appearance = TextoAppearance.prefs(this)
        val accent = TextoAppearance.accent(this)
        val shell = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(16),0,dp(16),dp(12)) }
        val content = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(0,dp(40),0,dp(24)) }
        fun label(text: String,size: Float=16f) { content.addView(TextView(this).apply { this.text=text; textSize=size; setTextColor(if(resources.configuration.uiMode and 0x30 == 0x20) 0xFFE9EDF4.toInt() else 0xFF343D49.toInt()); setPadding(dp(8),dp(16),dp(8),dp(12)) }) }
        fun row(title: String,value: String,action: () -> Unit) {
            content.addView(MaterialButton(this).apply {
                setIconResource(when { title.contains("image",true) || title.contains("art",true) -> dev.octoshrimpy.quik.R.drawable.ic_photo_size_select_actual_black_24dp; title.contains("text",true) || title.contains("title",true) -> dev.octoshrimpy.quik.R.drawable.ic_title_black_24dp; else -> dev.octoshrimpy.quik.R.drawable.ic_palette_black_24dp })
                text=if(value.isEmpty()) title else "$title  ·  $value"; isAllCaps=false; gravity=Gravity.START or Gravity.CENTER_VERTICAL
                minHeight=dp(56); cornerRadius=dp(18); setPadding(dp(16),dp(12),dp(16),dp(12))
                backgroundTintList=ColorStateList.valueOf(androidx.core.graphics.ColorUtils.blendARGB(if(resources.configuration.uiMode and 0x30 == 0x20) 0xFF23262D.toInt() else 0xFFF3F5F9.toInt(),accent,.07f))
                setTextColor(if(resources.configuration.uiMode and 0x30 == 0x20) 0xFFF3F5F9.toInt() else 0xFF20232B.toInt())
                setOnClickListener { action() }
            }.also { TextoAppearance.styleSettingsCard(it) },LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(8) })
        }
        fun choice(title: String,key: String,names: Array<String>,values: Array<String>,default: String) {
            val selected=values.indexOf(appearance.getString(key,default)).coerceAtLeast(0)
            row(title,names[selected]) {
                dialog=dev.texto.privacy.TextoDialogs.builder(this).setTitle(title).setSingleChoiceItems(names,selected) { d,i ->
                    appearance.edit().putString(key,values[i]).apply(); d.dismiss(); render()
                }.setNegativeButton("Cancel",null).show()
            }
        }
        fun toggle(title: String,key: String,default: Boolean) {
            content.addView(MaterialSwitch(this).apply {
                text=title; minHeight=dp(56); setPadding(dp(8),dp(8),dp(8),dp(8)); isChecked=appearance.getBoolean(key,default)
                setOnCheckedChangeListener { _,value -> appearance.edit().putBoolean(key,value).apply(); render() }
            })
        }
        label("Make it yours",34f)
        label("One look for your inbox, messages and private conversations",15f)
        val sample=ConversationListItemBinding.inflate(layoutInflater,content,false)
        sample.title.text="Alex Morgan"; sample.title.setTypeface(null,Typeface.BOLD); sample.date.text="Now"
        sample.snippet.text="See you soon. I’ll bring the photos from our trip."
        sample.avatars.recipients=listOf(Recipient().apply { address="Alex"; contact=Contact().apply { name="Alex Morgan" } })
        TextoAppearance.styleConversation(sample,24,2)
        sample.root.isClickable=false; sample.root.contentDescription="Sample conversation preview"
        content.addView(sample.root)
        val bubbles=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(16),dp(12),dp(16),dp(4)) }
        fun bubble(text: String,outgoing: Boolean) {
            val night=resources.configuration.uiMode and 0x30 == 0x20
            val fill=if(!outgoing) accent else if(night) 0xFF333640.toInt() else 0xFFE8EBF1.toInt()
            bubbles.addView(TextView(this).apply {
                this.text=text; textSize=16f; setTextColor(if(!outgoing) TextoAppearance.onAccent(this@ThemesActivity) else if(night) Color.WHITE else 0xFF20232B.toInt())
                setPadding(dp(16),dp(12),dp(16),dp(12))
                background=GradientDrawable().apply { setColor(fill); cornerRadius=dp(if(appearance.getString("bubbles","fluid")=="fluid") 24 else 16).toFloat() }
                MessageBodyStyle.apply(this,outgoing,accent,false)
            },LinearLayout.LayoutParams(-2,-2).apply { gravity=if(outgoing) Gravity.END else Gravity.START; bottomMargin=dp(6) })
        }
        bubble("See you soon!",false); bubble("On my way  ✓✓",true); content.addView(bubbles)
        label("Live preview · sample messages",12f)
        label("Color & display",22f)
        val paletteNames=arrayOf("Ocean blue","Jade","Coral","Lavender","Graphite","Rose","Amber","Teal")
        val paletteColors=intArrayOf(0xFF375BCD.toInt(),0xFF007E70.toInt(),0xFFB84F43.toInt(),0xFF7953B8.toInt(),0xFF536170.toInt(),0xFFB73873.toInt(),0xFFA76700.toInt(),0xFF006C84.toInt())
        for(group in 0..1) {
            val swatches=LinearLayout(this)
            for(i in group*4 until group*4+4) swatches.addView(MaterialButton(this).apply {
                text=(if(accent==paletteColors[i]) "✓ " else "")+paletteNames[i]; textSize=11f; isAllCaps=false; contentDescription=paletteNames[i]; minWidth=0; minimumWidth=0; setPadding(dp(3),0,dp(3),0); cornerRadius=dp(24)
                backgroundTintList=ColorStateList.valueOf(paletteColors[i]); setTextColor(Color.WHITE)
                setOnClickListener { saveAccent(paletteColors[i]) }
            },LinearLayout.LayoutParams(0,dp(56),1f).apply { marginEnd=dp(4) })
            content.addView(swatches)
        }
        row("Color wheel","Choose any color") { customAccent() }
        if(android.os.Build.VERSION.SDK_INT >= 31) toggle("Use wallpaper colors","dynamic_colors",false)
        val modes=arrayOf("System","Light","Dark","Scheduled")
        row("Appearance",modes[prefs.nightMode.get().coerceIn(0,3)]) {
            dialog=dev.texto.privacy.TextoDialogs.builder(this).setTitle("Appearance").setSingleChoiceItems(modes,prefs.nightMode.get()) { d,i ->
                d.dismiss(); nightModeManager.updateNightMode(i); render()
            }.setNegativeButton("Cancel",null).show()
        }
        content.addView(MaterialSwitch(this).apply { text="Pure black in dark mode"; minHeight=dp(56); isChecked=prefs.black.get(); setOnCheckedChangeListener { _,checked -> prefs.black.set(checked) } })
        label("Cards & surfaces",22f)
        choice("Card shape","card_shape",arrayOf("Soft","Round","Minimal","Square","Pill"),arrayOf("soft","round","minimal","square","pill"),"soft")
        choice("Card finish","card_finish",arrayOf("Tonal cards","Neutral","Accent tint","Outlined","Aurora gradient","Frosted glass","AMOLED outline","Pastel","Two-tone gradient","Bold outline","High contrast","No cards — everywhere"),arrayOf("tonal","neutral","tinted","outlined","gradient","glass","amoled","pastel","duotone","bold","contrast","none"),"tonal")
        label("Card finish, shape, size and spacing also apply to messages inside conversations.",13f)
        choice("Card size","card_size",arrayOf("Small","Medium","Large"),arrayOf("small","medium","large"),"medium")
        choice("Card border","border_width",arrayOf("Follow finish","None","Fine","Medium","Bold"),arrayOf("auto","0","1","2","3"),"auto")
        choice("Gradient direction","gradient_direction",arrayOf("Diagonal","Top to bottom","Left to right"),arrayOf("diagonal","vertical","horizontal"),"diagonal")
        choice("Spacing","density",arrayOf("Compact","Comfortable","Airy"),arrayOf("compact","comfortable","airy"),"comfortable")
        choice("Message preview","preview_lines",arrayOf("Hidden","One line","Two lines","Three lines"),arrayOf("0","1","2","3"),"2")
        choice("Unread indicator","unread_style",arrayOf("Number badge","Dot"),arrayOf("count","dot"),"count")
        toggle("Show contact avatars","list_avatars",true)
        toggle("Show message totals","message_counts",true)
        label("Navigation",22f)
        toggle("Show bottom bar", "bottom_bar_visible", true)
        row("Bottom bar shortcuts", "Choose up to 5") { BottomNavigation.configure(this) { render() } }
        label("The bottom bar is hidden while searching. All tools stay available in the menu.",13f)
        label("Your inbox",22f)
        row("Header title",appearance.getString("inbox_title","Texto").orEmpty()) { customText("Header title","inbox_title","Texto",32) }
        row("Motto",appearance.getString("inbox_motto","Messages, comfortably within reach").orEmpty()) { customText("Motto","inbox_motto","Messages, comfortably within reach",100) }
        row("Background image",if(appearance.contains("background_image")) "Selected" else "None") { pickImage("background_image") }
        row("Header artwork",if(appearance.contains("header_image")) "Selected" else "None") { pickImage("header_image") }
        if(appearance.contains("header_image")) row("Adjust header crop","") { ArtworkCrop.show(this,android.net.Uri.parse(appearance.getString("header_image","")!!),"header_image") { render() } }
        if(appearance.contains("background_image")) row("Adjust background crop","") { ArtworkCrop.show(this,android.net.Uri.parse(appearance.getString("background_image","")!!),"background_image") { render() } }
        row("Remove background image","") { appearance.edit().remove("background_image").apply(); render() }
        row("Remove header artwork","") { appearance.edit().remove("header_image").apply(); render() }
        label("Images remain on your device. A readability overlay keeps messages legible. Long press your header title for Archive.",13f)
        label("Messages & motion",22f)
        fun messageColor(title: String,key: String) {
            row(title,if(appearance.contains(key)) "Custom" else "Follow theme") {
                dialog=ColorRing.show(this,appearance.getInt(key,accent)) { color -> appearance.edit().putInt(key,color).apply(); render() }
            }
        }
        messageColor("Sent bubble color","sent_color")
        messageColor("Received bubble color","received_color")
        row("Use theme bubble colors","") { appearance.edit().remove("sent_color").remove("received_color").apply(); render() }
        label("Custom bubble colors keep text and links readable. No cards still hides bubble surfaces.",13f)
        choice("Message font","message_font",arrayOf("Follow theme","Sans serif","Serif","Monospace"),arrayOf("theme","sans","serif","mono"),"theme")
        choice("Message line spacing","message_line_height",arrayOf("Normal","Relaxed","Spacious"),arrayOf("normal","relaxed","spacious"),"normal")
        choice("Message bubbles","bubbles",arrayOf("Fluid rounded","Classic grouped"),arrayOf("fluid","classic"),"fluid")
        val sizes=arrayOf("Small","Normal","Large","Larger","Largest")
        row("Text size",sizes[prefs.textSize.get().coerceIn(0,4)]) {
            dialog=dev.texto.privacy.TextoDialogs.builder(this).setTitle("Text size").setSingleChoiceItems(sizes,prefs.textSize.get()) { d,i -> d.dismiss(); prefs.textSize.set(i) }.setNegativeButton("Cancel",null).show()
        }
        toggle("Prefer highest display refresh rate","high_refresh",false)
        toggle("Reduce motion","reduce_motion",false)
        label("Changes apply to public and private conversations. Your phone controls the available refresh rate.",13f)
        row("Fonts, scheduled dark mode & more","") { startActivity(Intent(this,dev.octoshrimpy.quik.feature.settings.SettingsActivity::class.java)) }
        label("OEM-inspired presets · optional",22f)
        row("App icon",LauncherIcons.names[LauncherIcons.ids.indexOf(LauncherIcons.selected(this))]) { iconPicker() }
        label("Your current Texto theme stays unchanged. Browse a preset, then choose Apply to use it. Presets include their own palette, heading, conversation list, bubbles and navigation. Your previous theme can be restored; images and privacy settings are kept.",13f)
        row("Browse OEM presets", appearance.getString("skin_preset", "Current Texto theme").orEmpty()) {
            dialog = TextoDialogs.builder(this).setTitle("OEM-inspired presets")
                .setItems(SkinPresets.names) { _, i ->
                    dialog = TextoDialogs.builder(this).setTitle(SkinPresets.names[i])
                        .setMessage(SkinPresets.descriptions[i] + "\n\nApply this complete visual style? You can restore your previous theme here afterward.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Apply") { _, _ -> SkinPresets.apply(this, i); render() }.show()
                }.setNegativeButton("Close", null).show()
        }
        if (appearance.contains("before_oem_preset")) row("Restore my previous theme", "Before OEM preset") { SkinPresets.restore(this); render() }
        label("Presets are editable Texto styling, not native OEM features.",13f)
        row("Reset appearance","") {
            dialog=dev.texto.privacy.TextoDialogs.builder(this).setTitle("Reset appearance?").setMessage("Restore default colors, cards and motion settings. Messages and privacy settings are kept.")
                .setNegativeButton("Cancel",null).setPositiveButton("Reset") { _,_ ->
                    val edit=appearance.edit()
                    listOf("sent_color","received_color","message_font","message_line_height","border_width","gradient_direction","bottom_bar_visible","bottom_bar_items","skin_preset","oem_layout","before_oem_preset","card_shape","card_finish","density","preview_lines","unread_style","list_avatars","message_counts","bubbles","high_refresh","reduce_motion","dynamic_colors","card_size","inbox_title","inbox_motto","background_image","header_image").forEach { edit.remove(it) }; edit.apply()
                    prefs.theme().set(0xFF375BCD.toInt()); prefs.autoColor.set(false); prefs.black.set(false); prefs.textSize.set(1)
                    nightModeManager.updateNightMode(0); render()
                }.show()
        }
        scroller=ScrollView(this).apply { addView(content); isFillViewport=true }
        shell.addView(scroller,LinearLayout.LayoutParams(-1,0,1f))
        if(intent.getBooleanExtra("from_private",false)) shell.addView(MaterialButton(this).apply {
            text="Back to private messages"; setIconResource(dev.octoshrimpy.quik.R.drawable.ic_texto_privacy); TextoAppearance.styleSettingsCard(this); setOnClickListener { finish() }
        },LinearLayout.LayoutParams(-1,dp(56)))
        else if (BottomNavigation.enabled(this)) shell.addView(TextoTabBar(this).apply {
            select(1); onTabSelected = { tab -> if (tab != 1) { BottomNavigation.open(this@ThemesActivity, tab); finish() } }
        },LinearLayout.LayoutParams(-1,dp(66)))
        setContentView(shell); scroller?.post { scroller?.scrollTo(0,position) }
    }
    private fun saveAccent(color: Int) {
        TextoAppearance.prefs(this).edit().putBoolean("dynamic_colors",false).apply()
        prefs.theme().set(color); prefs.autoColor.set(false); render()
    }
    private fun customAccent() { dialog=ColorRing.show(this,TextoAppearance.accent(this),::saveAccent) }
    private fun iconPicker() {
        val content=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(12),dp(8),dp(12),dp(8)) }
        val selected=LauncherIcons.selected(this)
        LauncherIcons.ids.indices.chunked(2).forEach { indices ->
            val row=LinearLayout(this)
            indices.forEach { i -> row.addView(LinearLayout(this).apply {
                orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER
                TextoAppearance.styleSettingsCard(this)
                minimumHeight=dp(148); setPadding(dp(8),dp(12),dp(8),dp(12))
                val label=(if(selected==LauncherIcons.ids[i]) "✓ " else "")+LauncherIcons.names[i]
                contentDescription=label; isFocusable=true; isClickable=true
                addView(ImageView(this@ThemesActivity).apply {
                    setImageResource(LauncherIcons.previews[i]); importantForAccessibility=android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
                },LinearLayout.LayoutParams(dp(56),dp(56)))
                addView(TextView(this@ThemesActivity).apply {
                    text=label; textSize=13f; gravity=Gravity.CENTER; setTextColor(TextoAppearance.readableText(context))
                    importantForAccessibility=android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO
                },LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(8) })
                setOnClickListener {
                    try {
                        LauncherIcons.apply(this@ThemesActivity,LauncherIcons.ids[i]); dialog?.dismiss(); render()
                        Toast.makeText(this@ThemesActivity,"App icon updated. Your launcher may take a moment to refresh.",Toast.LENGTH_LONG).show()
                    } catch(e: RuntimeException) { Toast.makeText(this@ThemesActivity,"Could not change the launcher icon. Please try again.",Toast.LENGTH_LONG).show() }
                }
            },LinearLayout.LayoutParams(0,-2,1f).apply { setMargins(dp(4),dp(4),dp(4),dp(4)) }) }
            content.addView(row)
        }
        dialog=TextoDialogs.builder(this).setTitle("App icon · OEM inspired")
            .setView(ScrollView(this).apply { addView(content) }).setNegativeButton("Close",null).show()
    }
}
