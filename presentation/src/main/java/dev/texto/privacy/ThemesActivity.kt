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
                TextoAppearance.prefs(this).edit().putString(imageKey,uri.toString()).apply(); render()
            } catch(e: SecurityException) { Toast.makeText(this,"Could not keep access to this image. Choose another image.",Toast.LENGTH_LONG).show() }
        }
    }
    private fun pickImage(key: String) { imageKey=key; imagePicker.launch(arrayOf("image/*")) }
    private fun customText(title: String,key: String,default: String,max: Int) {
        val field=TextInputEditText(this).apply { setSingleLine(); filters=arrayOf(android.text.InputFilter.LengthFilter(max)); setText(TextoAppearance.prefs(this@ThemesActivity).getString(key,default)); setPadding(dp(24),dp(16),dp(24),dp(16)) }
        dialog=MaterialAlertDialogBuilder(this).setTitle(title).setView(field).setNegativeButton("Cancel",null).setPositiveButton("Save") { _,_ -> TextoAppearance.prefs(this).edit().putString(key,field.text.toString().trim()).apply(); render() }.show()
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
                dialog=MaterialAlertDialogBuilder(this).setTitle(title).setSingleChoiceItems(names,selected) { d,i ->
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
        label("One look for your inbox and private conversations",15f)
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
        row("Custom accent",String.format("#%06X",accent and 0xFFFFFF)) { customAccent() }
        if(android.os.Build.VERSION.SDK_INT >= 31) toggle("Use wallpaper colors","dynamic_colors",false)
        val modes=arrayOf("System","Light","Dark","Scheduled")
        row("Appearance",modes[prefs.nightMode.get().coerceIn(0,3)]) {
            dialog=MaterialAlertDialogBuilder(this).setTitle("Appearance").setSingleChoiceItems(modes,prefs.nightMode.get()) { d,i ->
                d.dismiss(); nightModeManager.updateNightMode(i); render()
            }.setNegativeButton("Cancel",null).show()
        }
        content.addView(MaterialSwitch(this).apply { text="Pure black in dark mode"; minHeight=dp(56); isChecked=prefs.black.get(); setOnCheckedChangeListener { _,checked -> prefs.black.set(checked) } })
        label("Cards & surfaces",22f)
        choice("Card shape","card_shape",arrayOf("Soft","Round","Minimal"),arrayOf("soft","round","minimal"),"soft")
        choice("Card finish","card_finish",arrayOf("Tonal cards","Neutral","Accent tint","Outlined","Aurora gradient","Frosted glass","AMOLED outline","No cards — everywhere"),arrayOf("tonal","neutral","tinted","outlined","gradient","glass","amoled","none"),"tonal")
        choice("Card size","card_size",arrayOf("Small","Medium","Large"),arrayOf("small","medium","large"),"medium")
        choice("Spacing","density",arrayOf("Compact","Comfortable","Airy"),arrayOf("compact","comfortable","airy"),"comfortable")
        choice("Message preview","preview_lines",arrayOf("Hidden","One line","Two lines","Three lines"),arrayOf("0","1","2","3"),"2")
        choice("Unread indicator","unread_style",arrayOf("Number badge","Dot"),arrayOf("count","dot"),"count")
        toggle("Show contact avatars","list_avatars",true)
        toggle("Show message totals","message_counts",true)
        label("Your inbox",22f)
        row("Header title",appearance.getString("inbox_title","Texto").orEmpty()) { customText("Header title","inbox_title","Texto",32) }
        row("Motto",appearance.getString("inbox_motto","Messages, comfortably within reach").orEmpty()) { customText("Motto","inbox_motto","Messages, comfortably within reach",100) }
        row("Background image",if(appearance.contains("background_image")) "Selected" else "None") { pickImage("background_image") }
        row("Header artwork",if(appearance.contains("header_image")) "Selected" else "None") { pickImage("header_image") }
        row("Remove background image","") { appearance.edit().remove("background_image").apply(); render() }
        row("Remove header artwork","") { appearance.edit().remove("header_image").apply(); render() }
        label("Images remain on your device. A readability overlay keeps messages legible. Long press your header title for Archive.",13f)
        label("Messages & motion",22f)
        choice("Message bubbles","bubbles",arrayOf("Fluid rounded","Classic grouped"),arrayOf("fluid","classic"),"fluid")
        val sizes=arrayOf("Small","Normal","Large","Larger","Largest")
        row("Text size",sizes[prefs.textSize.get().coerceIn(0,4)]) {
            dialog=MaterialAlertDialogBuilder(this).setTitle("Text size").setSingleChoiceItems(sizes,prefs.textSize.get()) { d,i -> d.dismiss(); prefs.textSize.set(i) }.setNegativeButton("Cancel",null).show()
        }
        toggle("Prefer highest display refresh rate","high_refresh",false)
        toggle("Reduce motion","reduce_motion",false)
        label("Changes apply to public and private conversations. Your phone controls the available refresh rate.",13f)
        row("Fonts, scheduled dark mode & more","") { startActivity(Intent(this,dev.octoshrimpy.quik.feature.settings.SettingsActivity::class.java)) }
        row("Reset appearance","") {
            dialog=MaterialAlertDialogBuilder(this).setTitle("Reset appearance?").setMessage("Restore default colors, cards and motion settings. Messages and privacy settings are kept.")
                .setNegativeButton("Cancel",null).setPositiveButton("Reset") { _,_ ->
                    val edit=appearance.edit()
                    listOf("card_shape","card_finish","density","preview_lines","unread_style","list_avatars","message_counts","bubbles","high_refresh","reduce_motion","dynamic_colors","card_size","inbox_title","inbox_motto","background_image","header_image").forEach { edit.remove(it) }; edit.apply()
                    prefs.theme().set(0xFF375BCD.toInt()); prefs.autoColor.set(false); prefs.black.set(false); prefs.textSize.set(1)
                    nightModeManager.updateNightMode(0); render()
                }.show()
        }
        scroller=ScrollView(this).apply { addView(content); isFillViewport=true }
        shell.addView(scroller,LinearLayout.LayoutParams(-1,0,1f))
        if(intent.getBooleanExtra("from_private",false)) shell.addView(MaterialButton(this).apply {
            text="Back to private messages"; backgroundTintList=ColorStateList.valueOf(accent); setTextColor(TextoAppearance.onAccent(this@ThemesActivity)); setOnClickListener { finish() }
        },LinearLayout.LayoutParams(-1,dp(56)))
        else shell.addView(TextoTabBar(this).apply { select(1); onTabSelected={ tab -> when(tab) {
            0 -> { startActivity(Intent(this@ThemesActivity,dev.octoshrimpy.quik.feature.main.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)); finish() }
        } } },LinearLayout.LayoutParams(-1,dp(66)))
        setContentView(shell); scroller?.post { scroller?.scrollTo(0,position) }
    }
    private fun saveAccent(color: Int) {
        TextoAppearance.prefs(this).edit().putBoolean("dynamic_colors",false).apply()
        prefs.theme().set(color); prefs.autoColor.set(false); render()
    }
    private fun customAccent() {
        val input=TextInputEditText(this).apply { hint="#RRGGBB"; setSingleLine(); setText(String.format("#%06X",TextoAppearance.accent(this@ThemesActivity) and 0xFFFFFF)); setPadding(dp(24),dp(16),dp(24),dp(16)) }
        val prompt=MaterialAlertDialogBuilder(this).setTitle("Custom accent color").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Apply",null).create()
        dialog=prompt
        prompt.setOnShowListener { prompt.getButton(-1).setOnClickListener {
            val value=input.text.toString().trim().removePrefix("#")
            if(!value.matches(Regex("[0-9a-fA-F]{6}"))) input.error="Enter six hexadecimal digits, such as 375BCD"
            else { prompt.dismiss(); saveAccent(Color.parseColor("#$value")) }
        } }; prompt.show()
    }
}
