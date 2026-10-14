package dev.texto.testing

import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.octoshrimpy.quik.R
import dev.octoshrimpy.quik.databinding.ConversationListItemBinding
import dev.octoshrimpy.quik.databinding.SettingsControllerBinding
import dev.octoshrimpy.quik.feature.main.MainActivity
import dev.octoshrimpy.quik.model.Conversation
import dev.octoshrimpy.quik.model.Message
import dev.octoshrimpy.quik.model.Recipient
import dev.texto.privacy.*
import io.realm.Realm

/** Synthetic rows only. Does not read or modify the device SMS provider. */
class CompactLayoutInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    private fun main(block: () -> Unit) {
        var error: Throwable?=null
        runOnMainSync { try { block() } catch(e: Throwable) { error=e } }
        error?.let { throw it }
    }
    override fun onStart() {
        val result=Bundle()
        val prefs=TextoAppearance.prefs(targetContext)
        val keys=listOf("card_finish","card_shape","card_size","density","preview_lines","unread_style")
        val previousCounts=prefs.getBoolean("message_counts",true)
        val countsPresent=prefs.contains("message_counts")
        val previous=keys.associateWith { prefs.getString(it,null) }
        var activity: android.app.Activity?=null
        var realm: Realm?=null
        var adapter: dev.octoshrimpy.quik.feature.conversations.ConversationsAdapter?=null
        try {
            check(android.os.Build.MODEL.contains("sdk",true)) { "Emulator only" }
            // RemoteViews/notification images must survive releasing Glide's request and bitmap pool.
            val avatarFile=java.io.File(targetContext.cacheDir,"system-avatar-test.png")
            try {
                val input=android.graphics.Bitmap.createBitmap(1024,768,android.graphics.Bitmap.Config.ARGB_8888)
                input.eraseColor(android.graphics.Color.MAGENTA)
                avatarFile.outputStream().use { input.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) }
                input.recycle()
                for(circular in listOf(false,true)) {
                    val image=requireNotNull(SystemAvatar.load(targetContext,avatarFile,64,64,circular))
                    main { dev.octoshrimpy.quik.util.GlideApp.get(targetContext).clearMemory() }
                    check(!image.isRecycled && image.width <= 64 && image.height <= 64)
                    check(image.getPixel(image.width/2,image.height/2)==android.graphics.Color.MAGENTA)
                    image.recycle()
                }
                check(SystemAvatar.load(targetContext,null,64,64)==null)
                check(SystemAvatar.load(targetContext,java.io.File(targetContext.cacheDir,"missing-avatar.png"),64,64)==null)
            } finally { avatarFile.delete() }
            val source=startActivitySync(Intent(targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as MainActivity
            adapter=source.conversationsAdapter
            main { source.finish() }; waitForIdleSync()
            activity=startActivitySync(Intent(targetContext,dev.octoshrimpy.quik.feature.settings.SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            val screen=requireNotNull(activity)
            val rows=requireNotNull(adapter)
            var standardHeight=0
            main {
                val parent=LinearLayout(screen)
                val row=ConversationListItemBinding.inflate(screen.layoutInflater,parent,false)
                row.title.text="Alex Morgan"; row.date.text="12:30"
                row.snippet.text="A short preview of our conversation."
                val d=screen.resources.displayMetrics.density
                fun measure() {
                    row.root.measure(View.MeasureSpec.makeMeasureSpec((360*d).toInt(),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED))
                    row.root.layout(0,0,row.root.measuredWidth,row.root.measuredHeight)
                }
                for(finish in listOf("tonal","neutral","tinted","outlined","gradient","glass","amoled","pastel","duotone","bold","contrast","none")) {
                    for(size in listOf("small","medium","large")) for(spacing in listOf("compact","comfortable","airy")) {
                        prefs.edit().putString("card_finish",finish).putString("card_size",size).putString("density",spacing).putString("preview_lines","2").commit()
                        TextoAppearance.styleConversation(row,42,3); measure()
                        check(row.root.height >= 48*d && row.root.height <= 110*d) { "Bulky or inaccessible row: $finish/$size/$spacing ${row.root.height/d}dp" }
                        check(row.messageCount.bottom <= row.root.height-row.root.paddingBottom)
                        val background=row.root.background
                        TextoAppearance.styleConversation(row,42,3)
                        check(row.root.background === background && !row.root.isLayoutRequested) { "Unchanged bind allocated or requested layout" }
                    }
                }
                prefs.edit().putString("card_finish","tonal").putString("card_size","medium").putString("density","comfortable").commit()
                TextoAppearance.styleConversation(row,42,3); measure(); standardHeight=(row.root.height/d).toInt()
                TextoAppearance.styleConversation(row,50,0)
                check(row.messageCount.text=="50 messages" && row.unread.visibility==View.GONE)
                TextoAppearance.styleConversation(row,50,9,true)
                check(row.unread.visibility==View.VISIBLE && row.unread.text=="9 new")
                // Accessibility sizes grow naturally, without a fixed-height clipped container.
                row.title.textSize=30f; row.snippet.textSize=26f; row.messageCount.textSize=20f; row.unread.textSize=22f
                TextoAppearance.styleConversation(row,50,9); measure()
                check(row.messageCount.bottom <= row.root.height-row.root.paddingBottom)
                check(row.root.height/d > standardHeight)
                check(row.unread.height >= row.unread.paint.fontSpacing) { "Large unread badge text clipped" }
                val body=TextView(screen).apply { text="Sample" }
                MessageBodyStyle.apply(body,false,0xFF007E70.toInt(),false)
                val first=body.background
                MessageBodyStyle.apply(body,false,0xFF007E70.toInt(),false)
                check(body.background === first)
                prefs.edit().putString("card_finish","none").commit()
                MessageBodyStyle.apply(body,false,0xFF007E70.toInt(),false)
                check(body.background !== first)
                prefs.edit().putString("card_finish","tonal").commit()
                // Narrow screens, long previews, maximum badge, both status icons, and RTL.
                row.snippet.text="A longer preview that wraps across several words without touching unread or status indicators."
                row.pinned.visibility=View.VISIBLE; row.scheduled.visibility=View.VISIBLE
                for(direction in listOf(View.LAYOUT_DIRECTION_LTR,View.LAYOUT_DIRECTION_RTL)) {
                    row.root.layoutDirection=direction
                    for(lines in listOf("0","1","2","3")) for(dot in listOf("dot","count")) for(counts in listOf(false,true)) {
                        prefs.edit().putString("preview_lines",lines).putString("unread_style",dot).putBoolean("message_counts",counts).commit()
                        TextoAppearance.styleConversation(row,123456,999)
                        row.unread.textSize=18f
                        row.root.measure(View.MeasureSpec.makeMeasureSpec((320*d).toInt(),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED))
                        row.root.layout(0,0,row.root.measuredWidth,row.root.measuredHeight)
                        val rail=row.conversationIndicators
                        check(rail.top>=row.title.bottom && rail.bottom<=row.root.height-row.root.paddingBottom) { "Status column exceeds row bounds" }
                        val snippet=row.snippet
                        if(snippet.visibility==View.VISIBLE) check(if(direction==View.LAYOUT_DIRECTION_LTR) snippet.right<=rail.left else snippet.left>=rail.right) { "Unread indicator overlaps preview" }
                    }
                }
                row.root.layoutDirection=View.LAYOUT_DIRECTION_LTR
                prefs.edit().putBoolean("message_counts",previousCounts).commit()
                val settings=SettingsControllerBinding.inflate(screen.layoutInflater)
                val dialing=com.google.android.material.button.MaterialButton(screen).apply { id=R.id.textoDialingSettings }
                settings.preferences.addView(dialing)
                val original=(0 until settings.preferences.childCount).map { settings.preferences.getChildAt(it) }.filter { it.id!=View.NO_ID }
                var clicks=0
                original.filter { it.id!=R.id.syncingProgress }.forEach { it.setOnClickListener { clicks++ } }
                SettingsSections.organize(settings.preferences)
                val reordered=(0 until settings.preferences.childCount).map { settings.preferences.getChildAt(it) }
                check(reordered.filter { it.id!=View.NO_ID }.toSet()==original.toSet())
                check(reordered.filterIsInstance<TextView>().filter { it.id==View.NO_ID }.size==5)
                check(reordered.indexOf(settings.unreadAtTop)>reordered.indexOf(settings.textoContactsSettings))
                check(reordered.indexOf(settings.delayed)>reordered.indexOf(dialing))
                original.filter { it.id!=R.id.syncingProgress }.forEach { it.performClick() }
                check(clicks==original.size-1) { "Regrouping broke a settings click" }
            }
            lateinit var list: RecyclerView
            main {
                realm=Realm.getInstance(io.realm.RealmConfiguration.Builder().name("compact-164").inMemory().allowWritesOnUiThread(true).build())
                realm!!.executeTransaction { r ->
                    for(i in 1L..1000L) r.insert(Conversation().apply {
                        id=i; name="Sample contact $i"; messageCount=i; unreadCount=if(i%3==0L) 2 else 0
                        recipients.add(Recipient().apply { id=i; address="+1555010${i.toString().padStart(4,'0')}" })
                        lastMessage=Message().apply { id=i; type=Message.TYPE_SMS; body="Sample message $i"; date=System.currentTimeMillis() }
                    })
                }
                list=RecyclerView(screen).apply { layoutManager=LinearLayoutManager(screen); this.adapter=rows }
                TextoAppearance.smooth(list); screen.setContentView(list)
                rows.updateData(realm!!.where(Conversation::class.java).findAll())
            }
            waitForIdleSync()
            for(i in 0..30) { main { list.scrollBy(0,150) }; SystemClock.sleep(17) }
            main { check((list.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()>0); list.scrollToPosition(999) }
            waitForIdleSync()
            main { check(list.findViewHolderForAdapterPosition(999)!=null); list.scrollToPosition(0) }
            waitForIdleSync()
            main {
                val optionalKeys = listOf("card_shape", "card_finish", "card_size", "density", "bubbles", "skin_preset", "oem_layout", "preview_lines", "unread_style", "before_oem_preset", "bottom_bar_items")
                val savedOptional = optionalKeys.associateWith { prefs.getString(it, null) }
                try {
                    prefs.edit().remove("before_oem_preset").putString("card_shape", "square").putString("card_finish", "none").putString("bottom_bar_items", "0,1,2,3,4,5,99,0").commit()
                    check(BottomNavigation.items(screen) == listOf(0,1,2,3,4))
                    val bar = TextoTabBar(screen)
                    check(bar.childCount == 5)
                    val firstButton = bar.getChildAt(0)
                    bar.rebuild()
                    check(bar.getChildAt(0) === firstButton) { "Unchanged navigation was reinflated" }
                    bar.select(3)
                    check((0 until bar.childCount).count { bar.getChildAt(it).isSelected } == 1)
                    val before = listOf("card_shape", "card_finish", "card_size", "density", "bubbles").associateWith { prefs.getString(it,null) }
                    SkinPresets.apply(screen, 1)
                    check(prefs.getString("card_finish", "") == "glass")
                    val palette=TextoAppearance.accent(screen)
                    val incoming=TextView(screen); val outgoing=TextView(screen)
                    MessageBodyStyle.apply(incoming,false,palette,false)
                    MessageBodyStyle.apply(outgoing,true,palette,false)
                    fun fill(v: View): Int {
                        val bitmap=android.graphics.Bitmap.createBitmap(100,60,android.graphics.Bitmap.Config.ARGB_8888)
                        v.background.setBounds(0,0,100,60); v.background.draw(android.graphics.Canvas(bitmap))
                        return bitmap.getPixel(50,30).also { bitmap.recycle() }
                    }
                    check(fill(incoming)!=fill(outgoing)) { "OEM sent and received bubbles are identical" }
                    for(index in SkinPresets.names.indices) {
                        SkinPresets.apply(screen,index)
                        check(SkinPresets.id(screen)==SkinPresets.ids[index])
                        MessageBodyStyle.apply(outgoing,true,TextoAppearance.accent(screen),false)
                        check(androidx.core.graphics.ColorUtils.calculateContrast(outgoing.currentTextColor,fill(outgoing))>=4.5)
                    }
                    SkinPresets.restore(screen)
                    check(before.all { (key,value) -> prefs.getString(key,null) == value }) { "Preset restore lost the user's original settings" }
                } finally {
                    val restore = prefs.edit()
                    savedOptional.forEach { (key,value) -> if(value==null) restore.remove(key) else restore.putString(key,value) }
                    restore.commit()
                }
            }
            result.putString("result","PASS: 108 finish/size/spacing combinations; default row ${standardHeight}dp; unchanged binds reuse backgrounds without layout; live counters/selection; larger text; five settings groups retain every row and click; 1000 synthetic conversations scroll and recycle")
        } catch(e: Throwable) { result.putString("error",e.stackTraceToString()) }
        finally {
            main { adapter?.updateData(null); realm?.close(); activity?.finish() }
            prefs.edit().apply { if(countsPresent) putBoolean("message_counts",previousCounts) else remove("message_counts"); previous.forEach { (key,value) -> if(value==null) remove(key) else putString(key,value) } }.commit()
        }
        finish(if(result.containsKey("error")) 1 else 0,result)
    }
}
