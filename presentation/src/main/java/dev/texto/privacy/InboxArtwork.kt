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
        Glide.with(view).asBitmap().load(android.net.Uri.parse(value)).override(1600,1600).fitCenter()
            .into(object: CustomTarget<android.graphics.Bitmap>() {
                override fun onResourceReady(resource: android.graphics.Bitmap, transition: Transition<in android.graphics.Bitmap>?) {
                    view.background=object: Drawable() {
                        val paint=android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)
                        private val matrix=android.graphics.Matrix()
                        override fun onBoundsChange(bounds: android.graphics.Rect) {
                            val scale=maxOf(bounds.width().toFloat()/resource.width,bounds.height().toFloat()/resource.height)
                            matrix.setScale(scale,scale)
                            matrix.postTranslate(bounds.left+(bounds.width()-resource.width*scale)/2,bounds.top+(bounds.height()-resource.height*scale)/2)
                        }
                        override fun draw(canvas: android.graphics.Canvas) {
                            val save=canvas.save();canvas.clipRect(bounds);canvas.drawBitmap(resource,matrix,paint);canvas.drawColor(scrim);canvas.restoreToCount(save)
                        }
                        override fun setAlpha(alpha: Int) { paint.alpha=alpha }
                        override fun setColorFilter(filter: android.graphics.ColorFilter?) { paint.colorFilter=filter }
                        @Suppress("DEPRECATION") override fun getOpacity()=android.graphics.PixelFormat.TRANSLUCENT
                    }
                }
                override fun onLoadCleared(placeholder: Drawable?) { view.background=null }
                override fun onLoadFailed(errorDrawable: Drawable?) { view.background=null }
            })
    }
    fun apply(activity: Activity, root: View, header: View, title: TextView, subtitle: TextView) {
        val p=TextoAppearance.prefs(activity)
        val profile=SkinPresets.id(activity)
        val defaultTitle = if (profile == "xiaomi") "Messages" else "Texto"
        val defaultMotto = "Messages, comfortably within reach"
        title.text=p.getString("inbox_title", defaultTitle).orEmpty().ifBlank { defaultTitle }
        title.contentDescription=title.text.toString()
        val motto=p.getString("inbox_motto", defaultMotto).orEmpty()
        subtitle.text=motto
        (root as? ReachableMessagesLayout)?.restingSubtitle=motto
        if(profile.isNotEmpty()) {
            val centered=profile in listOf("samsung","honor")
            title.gravity=if(centered) android.view.Gravity.CENTER else android.view.Gravity.START
            subtitle.gravity=title.gravity
            title.textSize=when(profile) { "samsung" -> 38f; "xiaomi" -> 34f; "pixel","motorola" -> 28f; "nothing" -> 34f; else -> 36f }
            title.typeface=android.graphics.Typeface.create(if(profile=="nothing") "monospace" else "sans-serif",if(profile in listOf("oppo","xiaomi","vivo")) 1 else 0)
            root.findViewById<View>(dev.octoshrimpy.quik.R.id.toolbarSearch)?.let { search ->
                search.backgroundTintList=null
                if (profile == "xiaomi") {
                    val dark = activity.resources.configuration.uiMode and 0x30 == 0x20
                    val d = activity.resources.displayMetrics.density
                    search.background = android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = 24f * d
                        setColor(if (dark) 0xFF1F2126.toInt() else 0xFFF2F3F7.toInt())
                    }
                } else {
                    search.background=TextoAppearance.card(activity)
                }
            }
        }
        background(root,"background_image"); background(header,"header_image")
    }
}
