package dev.texto.privacy

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.widget.*
import kotlin.math.*

class ColorRing(context: Context, initial: Int) : View(context) {
    val hsv=FloatArray(3).also { Color.colorToHSV(initial,it) }
    var changed: ((Int)->Unit)?=null
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    val color get()=Color.HSVToColor(hsv)
    init { contentDescription="Hue color ring. Drag around the ring to choose a color";isFocusable=true }
    override fun onDraw(canvas: Canvas) {
        val cx=width/2f;val cy=height/2f;val radius=min(cx,cy)*.75f
        paint.style=Paint.Style.STROKE;paint.strokeWidth=radius*.28f
        paint.shader=SweepGradient(cx,cy,intArrayOf(Color.RED,Color.YELLOW,Color.GREEN,Color.CYAN,Color.BLUE,Color.MAGENTA,Color.RED),null)
        canvas.drawCircle(cx,cy,radius,paint);paint.shader=null
        paint.style=Paint.Style.FILL;paint.color=color;canvas.drawCircle(cx,cy,radius*.62f,paint)
        val angle=Math.toRadians(hsv[0].toDouble());paint.color=Color.WHITE;paint.style=Paint.Style.STROKE;paint.strokeWidth=4*resources.displayMetrics.density
        canvas.drawCircle(cx+cos(angle).toFloat()*radius,cy+sin(angle).toFloat()*radius,radius*.14f,paint)
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if(event.actionMasked in listOf(MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE,MotionEvent.ACTION_UP)) {
            parent?.requestDisallowInterceptTouchEvent(true)
            hsv[0]=((atan2(event.y-height/2f,event.x-width/2f)*180f/PI.toFloat())+360f)%360f
            invalidate();changed?.invoke(color)
            if(event.actionMasked==MotionEvent.ACTION_UP) { performClick();parent?.requestDisallowInterceptTouchEvent(false) };return true
        };return true
    }
    override fun performClick(): Boolean { super.performClick();return true }
    companion object {
        fun show(context: Context,initial: Int,save: (Int)->Unit): androidx.appcompat.app.AlertDialog {
            val panel=LinearLayout(context).apply { orientation=LinearLayout.VERTICAL;setPadding(24,8,24,8) }
            val ring=ColorRing(context,initial);panel.addView(ring,LinearLayout.LayoutParams(-1,(240*context.resources.displayMetrics.density).toInt()))
            fun slider(label: String,index: Int) {
                panel.addView(TextView(context).apply { text=label;setTextColor(TextoAppearance.readableText(context)) })
                panel.addView(SeekBar(context).apply { max=100;progress=(ring.hsv[index]*100).toInt();contentDescription=label
                    setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(bar: SeekBar?,value: Int,user: Boolean) { ring.hsv[index]=value/100f;ring.invalidate() }
                        override fun onStartTrackingTouch(bar: SeekBar?) {};override fun onStopTrackingTouch(bar: SeekBar?) {}
                    })
                })
            }
            slider("Color intensity",1);slider("Brightness",2)
            return TextoDialogs.builder(context).setTitle("Choose a color").setView(panel).setNegativeButton("Cancel",null).setPositiveButton("Apply") { _,_->save(ring.color) }.show()
        }
    }
}
