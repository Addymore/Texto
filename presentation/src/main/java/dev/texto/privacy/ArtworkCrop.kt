package dev.texto.privacy

import android.content.Context
import android.graphics.*
import android.view.*
import android.widget.*
import android.net.Uri
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

class ArtworkCrop(context: Context,private val bitmap: Bitmap) : View(context) {
    private var zoom=1f;private var dx=0f;private var dy=0f;private var x=0f;private var y=0f
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val detector=ScaleGestureDetector(context,object: ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(d: ScaleGestureDetector): Boolean { zoom=(zoom*d.scaleFactor).coerceIn(1f,5f);invalidate();return true }
    })
    fun drawImage(canvas: Canvas,w: Int,h: Int) {
        val scale=maxOf(w.toFloat()/bitmap.width,h.toFloat()/bitmap.height)*zoom
        val extraX=(bitmap.width*scale-w)/2;val extraY=(bitmap.height*scale-h)/2
        dx=dx.coerceIn(-extraX,extraX);dy=dy.coerceIn(-extraY,extraY)
        val matrix=Matrix().apply { setScale(scale,scale);postTranslate(-extraX+dx,-extraY+dy) }
        canvas.drawBitmap(bitmap,matrix,paint)
    }
    override fun onDraw(canvas: Canvas) { drawImage(canvas,width,height) }
    override fun onTouchEvent(e: MotionEvent): Boolean {
        detector.onTouchEvent(e)
        if(e.actionMasked==MotionEvent.ACTION_DOWN) { x=e.x;y=e.y }
        if(e.actionMasked==MotionEvent.ACTION_MOVE) { if(!detector.isInProgress && e.pointerCount==1) { dx+=e.x-x;dy+=e.y-y;invalidate() };x=e.x;y=e.y }
        parent?.requestDisallowInterceptTouchEvent(true);return true
    }
    fun result(): Bitmap = Bitmap.createBitmap(width.coerceAtLeast(1),height.coerceAtLeast(1),Bitmap.Config.ARGB_8888).also { drawImage(Canvas(it),it.width,it.height) }
    companion object {
        fun show(activity: android.app.Activity,uri: Uri,key: String,done: ()->Unit) {
            Glide.with(activity).asBitmap().load(uri).override(2048,2048).into(object: CustomTarget<Bitmap>() {
                override fun onResourceReady(bitmap: Bitmap,transition: Transition<in Bitmap>?) {
                    if(activity.isFinishing || activity.isDestroyed)return
                    val crop=ArtworkCrop(activity,bitmap)
                    val panel=LinearLayout(activity).apply { orientation=LinearLayout.VERTICAL;addView(TextView(activity).apply { setTextColor(TextoAppearance.readableText(activity));text="Drag to position. Pinch to zoom. The preview is the saved crop.";setPadding(24,16,24,16) });addView(crop,LinearLayout.LayoutParams(-1,((if(key=="header_image") 220 else 420)*resources.displayMetrics.density).toInt())) }
                    TextoDialogs.builder(activity).setTitle(if(key=="header_image") "Crop header artwork" else "Crop background").setView(panel).setNegativeButton("Cancel",null).setPositiveButton("Use image") { _,_->
                        runCatching {
                            val result=crop.result();val file=java.io.File(activity.filesDir,"$key-${System.currentTimeMillis()}.jpg")
                            file.outputStream().use { result.compress(Bitmap.CompressFormat.JPEG,92,it) };result.recycle()
                            TextoAppearance.prefs(activity).edit().putString(key,Uri.fromFile(file).toString()).apply();done()
                        }.onFailure { Toast.makeText(activity,"Could not save image. Please try another image.",Toast.LENGTH_LONG).show() }
                    }.show()
                }
                override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
                override fun onLoadFailed(errorDrawable: android.graphics.drawable.Drawable?) { Toast.makeText(activity,"Could not open image",Toast.LENGTH_LONG).show() }
            })
        }
    }
}
