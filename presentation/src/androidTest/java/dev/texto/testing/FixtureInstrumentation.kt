package dev.texto.testing

import android.app.Instrumentation
import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import dev.octoshrimpy.quik.model.Message
import dev.texto.privacy.TrashStore
import io.realm.Realm

/** Test APK only. Never included in the installable Texto application. Uses disposable emulator data. */
class FixtureInstrumentation : Instrumentation() {
    private var action = "seed"
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); action = arguments?.getString("action") ?: "seed"; start() }
    override fun onStart() {
        val result = Bundle()
        try {
            check(android.os.Build.MODEL.contains("sdk",true) || android.os.Build.FINGERPRINT.contains("generic")) { "Disposable emulator required" }
            // Custom Instrumentation must wait for Application.onCreate before touching Realm.
            waitForIdleSync()
            val resolver = targetContext.contentResolver
            if (action == "gesture") {
                val intent = targetContext.packageManager.getLaunchIntentForPackage(targetContext.packageName)!!
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivitySync(intent)
                android.os.SystemClock.sleep(1500)
                val monitor = addMonitor("dev.texto.privacy.UnlockActivity",null,false)
                val start = android.os.SystemClock.uptimeMillis()
                fun send(kind: Int, ys: Float, count: Int) {
                    val properties = Array(count) { i -> android.view.MotionEvent.PointerProperties().apply { id=i; toolType=android.view.MotionEvent.TOOL_TYPE_FINGER } }
                    val coordinates = Array(count) { i -> android.view.MotionEvent.PointerCoords().apply { x=350f+i*300; y=ys; pressure=1f; size=1f } }
                    val event = android.view.MotionEvent.obtain(start,android.os.SystemClock.uptimeMillis(),kind,count,properties,coordinates,0,0,1f,1f,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0)
                    sendPointerSync(event); event.recycle()
                }
                send(android.view.MotionEvent.ACTION_DOWN,750f,1)
                send(android.view.MotionEvent.ACTION_POINTER_DOWN or (1 shl android.view.MotionEvent.ACTION_POINTER_INDEX_SHIFT),750f,2)
                for (step in 1..20) { send(android.view.MotionEvent.ACTION_MOVE,750f+step*30,2); android.os.SystemClock.sleep(25) }
                send(android.view.MotionEvent.ACTION_POINTER_UP or (1 shl android.view.MotionEvent.ACTION_POINTER_INDEX_SHIFT),1350f,2)
                send(android.view.MotionEvent.ACTION_UP,1350f,1)
                android.os.SystemClock.sleep(1500)
                check(waitForMonitorWithTimeout(monitor,5000) != null) { "Two-finger pull did not open authentication" }
                result.putString("result","PASS: two-finger pull opens authentication")
            } else if (action == "seed") {
                val thread = android.provider.Telephony.Threads.getOrCreateThreadId(targetContext, setOf("+15550101999"))
                val message = requireNotNull(resolver.insert(Uri.parse("content://mms"), ContentValues().apply {
                    put("thread_id",thread); put("date",System.currentTimeMillis()/1000); put("msg_box",1); put("m_type",132)
                    put("ct_t","application/vnd.wap.multipart.related"); put("read",0); put("seen",0)
                }))
                val id = android.content.ContentUris.parseId(message)
                resolver.insert(Uri.parse("content://mms/$id/addr"),ContentValues().apply { put("address","+15550101999"); put("type",137); put("charset",106) })
                resolver.insert(Uri.parse("content://mms/$id/part"),ContentValues().apply { put("ct","text/plain"); put("text","Texto MMS recovery fixture"); put("chset",106) })
                val part = requireNotNull(resolver.insert(Uri.parse("content://mms/$id/part"),ContentValues().apply { put("ct","image/png"); put("name","texto-test.png"); put("cl","texto-test.png") }))
                resolver.openOutputStream(part)!!.use { output -> Bitmap.createBitmap(32,32,Bitmap.Config.ARGB_8888).apply { eraseColor(0xFF375BCD.toInt()) }.compress(Bitmap.CompressFormat.PNG,100,output) }
                result.putString("result","Seeded synthetic MMS $id; run normal message sync before storage checks")
            } else {
                Realm.getDefaultInstance().use { realm ->
                    val message = requireNotNull(realm.where(Message::class.java).equalTo("address","+15550101999").equalTo("type","mms").findFirst())
                    val id = message.id; val providerUri = message.getUri(); val part = message.parts.first { it.type == "image/png" }
                    val partUri = Uri.parse("content://mms/part/${part.id}")
                    val before = resolver.openInputStream(partUri)!!.use { it.readBytes() }
                    check(before.isNotEmpty())
                    val store = TrashStore(targetContext)
                    store.move(listOf(id)); realm.refresh(); check(message.trashedAt > 0)
                    check(realm.where(Message::class.java).equalTo("id",id).equalTo("trashedAt",0L).count() == 0L)
                    store.restore(listOf(id)); realm.refresh(); check(message.trashedAt == 0L)
                    check(before.contentEquals(resolver.openInputStream(partUri)!!.use { it.readBytes() }))
                    store.move(listOf(id)); check(store.purgeExpired()); realm.refresh(); check(message.isValid)
                    val key = "${message.type}:${message.contentId}:${message.date}:${message.threadId}"
                    val expired = System.currentTimeMillis()-91*86_400_000L
                    targetContext.getSharedPreferences("texto_trash",0).edit().putLong(key,expired).commit()
                    realm.executeTransaction { message.trashedAt=expired }
                    check(store.purgeExpired()); realm.refresh()
                    check(realm.where(Message::class.java).equalTo("id",id).count()==0L)
                    check(resolver.query(providerUri,arrayOf("_id"),null,null,null)!!.use { !it.moveToFirst() })
                    result.putString("result","PASS: MMS bin hide, restore with byte-identical image, retain before expiry, and permanent provider deletion after expiry")
                }
            }
            finish(0,result)
        } catch (error: Throwable) { result.putString("error",error.stackTraceToString()); finish(1,result) }
    }
}
