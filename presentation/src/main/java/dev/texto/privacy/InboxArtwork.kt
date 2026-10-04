package dev.texto.privacy

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

object InboxArtwork {
    fun background(view: View,key: String) {
        val value=TextoAppearance.prefs(view.context).getString(key,null) ?: return
        val dark=view.resources.configuration.uiMode and 0x30 == 0x20
        val scrim=if(dark) 0xCB090B10.toInt() else 0xDDF8FAFF.toInt()
        Glide.with(view).load(android.net.Uri.parse(value)).centerCrop().override(1080,1600)
            .into(object: CustomTarget<Drawable>() {
                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) { view.background=LayerDrawable(arrayOf(resource,ColorDrawable(scrim))) }
                override fun onLoadCleared(placeholder: Drawable?) { view.background=null }
                override fun onLoadFailed(errorDrawable: Drawable?) { view.background=null }
            })
    }
    fun apply(activity: Activity, root: View, header: View, title: TextView, subtitle: TextView) {
        val p=TextoAppearance.prefs(activity)
        title.text=p.getString("inbox_title","Texto").orEmpty().ifBlank { "Texto" }
        title.contentDescription=title.text.toString()+". Long press for Archive."
        val motto=p.getString("inbox_motto","Messages, comfortably within reach").orEmpty()
        subtitle.text=motto
        (root as? ReachableMessagesLayout)?.restingSubtitle=motto
        background(root,"background_image"); background(header,"header_image")
    }
}
