package dev.texto.privacy

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.view.Gravity
import android.widget.*
import com.google.android.material.button.MaterialButton

object RuleContactCards {
    private val loader=java.util.concurrent.Executors.newSingleThreadExecutor()
    fun show(context: Context,key: String,title: String,edit: () -> Unit): androidx.appcompat.app.AlertDialog {
        val policy=TextoPolicy(context)
        val list=LinearLayout(context).apply { orientation=LinearLayout.VERTICAL; setPadding(16,8,16,8) }
        val dialog=TextoDialogs.builder(context).setTitle(title).setView(ScrollView(context).apply { addView(list) })
            .setPositiveButton("Add or edit numbers") { _,_ -> edit() }.setNegativeButton("Close",null).create()
        val d=context.resources.displayMetrics.density
        policy.entries(key).sorted().forEach { number ->
            val row=LinearLayout(context).apply { gravity=Gravity.CENTER_VERTICAL; setPadding((16*d).toInt(),(12*d).toInt(),(16*d).toInt(),(12*d).toInt()); background=TextoAppearance.card(context) }
            val avatar=FrameLayout(context)
            val initial=TextView(context).apply { text=number.takeLast(2); textSize=20f; gravity=Gravity.CENTER; setTextColor(TextoAppearance.onAccent(context)); background=android.graphics.drawable.GradientDrawable().apply { shape=1; setColor(TextoAppearance.accent(context)) } }
            val photo=ImageView(context).apply { scaleType=ImageView.ScaleType.CENTER_CROP }
            avatar.addView(initial,FrameLayout.LayoutParams(-1,-1));avatar.addView(photo,FrameLayout.LayoutParams(-1,-1))
            row.addView(avatar,LinearLayout.LayoutParams((48*d).toInt(),(48*d).toInt()))
            val label=TextView(context).apply { text=number;setTextColor(TextoAppearance.readableText(context)); textSize=16f; setPadding((16*d).toInt(),0,0,0) }
            row.addView(label,LinearLayout.LayoutParams(0,-2,1f))
            list.addView(row,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=(8*d).toInt() })
            loader.execute {
                val contact=runCatching {
                    context.contentResolver.query(Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI,Uri.encode(number)),arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME,ContactsContract.PhoneLookup.PHOTO_URI),null,null,null)?.use { if(it.moveToFirst()) it.getString(0) to it.getString(1) else null }
                }.getOrNull()
                row.post {
                    if(!dialog.isShowing || (key in listOf("locked","archived","protected_tools") && policy.hasPin() && !PrivacyGate.unlocked)) return@post
                    contact?.let { (name,uri) -> label.text="$name\n$number";initial.text=name.take(1).uppercase();if(uri!=null) com.bumptech.glide.Glide.with(photo).load(uri).circleCrop().into(photo) }
                }
            }
        }
        if(list.childCount==0) list.addView(TextView(context).apply { text="No numbers added";setTextColor(TextoAppearance.readableText(context));setPadding(24,24,24,24) })
        dialog.show();return dialog
    }
}
