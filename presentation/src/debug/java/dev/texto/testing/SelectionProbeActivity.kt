package dev.texto.testing

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import dev.octoshrimpy.quik.R
import dev.octoshrimpy.quik.common.widget.TightTextView
import dev.texto.privacy.*

/** Debug-only, synthetic text; never queries messages, contacts or changes the SMS role. */
class SelectionProbeActivity : AppCompatActivity() {
    lateinit var list: RecyclerView
    lateinit var firstBody: TightTextView
    var selectedMessages=0
    var clickedLinks=0
    override fun onCreate(state: Bundle?) {
        setTheme(R.style.TextoTheme); TextoTheme.apply(this); super.onCreate(state)
        list=RecyclerView(this).apply {
            layoutManager=LinearLayoutManager(this@SelectionProbeActivity)
            setPadding(24,48,24,24); clipToPadding=false
            adapter=object : RecyclerView.Adapter<Holder>() {
                override fun getItemCount()=40
                override fun onCreateViewHolder(parent: ViewGroup,type: Int): Holder {
                    val row=FrameLayout(parent.context).apply { setPadding(0,12,0,12) }
                    val text=TightTextView(parent.context).apply {
                        textSize=20f; autoLinkMask=0
                        text=android.text.SpannableString("Select these words inside the message. Native Android handles should copy this paragraph. This is synthetic test text only.").apply {
                            setSpan(object : android.text.style.ClickableSpan() {
                                override fun onClick(widget: View) { clickedLinks++ }
                            },18,24,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                    }
                    row.addView(text,FrameLayout.LayoutParams(-1,-2))
                    row.layoutParams=RecyclerView.LayoutParams(-1,-2)
                    row.setOnLongClickListener { selectedMessages++; true }
                    MessageSelectionGesture.attach(text,row) { false }
                    text.movementMethod=android.text.method.ArrowKeyMovementMethod.getInstance()
                    return Holder(row,text)
                }
                override fun onBindViewHolder(holder: Holder,position: Int) {
                    MessageSelectionGesture.bind(holder.body,position.toLong())
                    MessageBodyStyle.apply(holder.body,position%2==0,TextoAppearance.accent(this@SelectionProbeActivity),false)
                    if(position==0) firstBody=holder.body
                }
            }
        }
        TextoAppearance.smooth(list); setContentView(list)
    }
    class Holder(row: View,val body: TightTextView) : RecyclerView.ViewHolder(row)
}
