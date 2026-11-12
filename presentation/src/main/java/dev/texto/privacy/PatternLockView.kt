package dev.texto.privacy

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.ColorUtils
import kotlin.math.hypot
import kotlin.math.min

/** Smooth 3x3 pattern lock view mimicking Xiaomi Messages / HyperOS private space pattern input. */
class PatternLockView(context: Context) : View(context) {
    private val d = resources.displayMetrics.density
    private val selectedNodes = mutableListOf<Int>()
    private var currentX = 0f
    private var currentY = 0f
    private var dragging = false
    var onPatternDetected: ((String) -> Unit)? = null

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f * d
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    init {
        minimumHeight = (240 * d).toInt()
        minimumWidth = (240 * d).toInt()
        contentDescription = "Pattern lock grid"
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desired = (240 * d).toInt()
        val w = resolveSize(desired, widthMeasureSpec)
        val h = resolveSize(desired, heightMeasureSpec)
        val size = min(w, h)
        setMeasuredDimension(w, size)
    }

    private fun nodeCenter(index: Int): Pair<Float, Float> {
        val row = index / 3
        val col = index % 3
        val gridSize = min(width, height) - 48f * d
        val startX = (width - gridSize) / 2f
        val startY = (height - gridSize) / 2f
        val step = gridSize / 2f
        return (startX + col * step) to (startY + row * step)
    }

    private fun findHitNode(x: Float, y: Float): Int {
        val hitRadius = 30f * d
        for (i in 0..8) {
            val (cx, cy) = nodeCenter(i)
            if (hypot(x - cx, y - cy) <= hitRadius) return i
        }
        return -1
    }

    private fun addNodeWithIntermediate(node: Int) {
        if (node < 0 || node in selectedNodes) return
        val last = selectedNodes.lastOrNull()
        if (last != null) {
            val r1 = last / 3; val c1 = last % 3
            val r2 = node / 3; val c2 = node % 3
            if ((r1 + r2) % 2 == 0 && (c1 + c2) % 2 == 0) {
                val mid = ((r1 + r2) / 2) * 3 + ((c1 + c2) / 2)
                if (mid !in selectedNodes) selectedNodes.add(mid)
            }
        }
        selectedNodes.add(node)
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val accent = TextoAppearance.accent(context)
        val ink = TextoAppearance.readableText(context)
        linePaint.color = ColorUtils.setAlphaComponent(accent, 190)
        haloPaint.color = ColorUtils.setAlphaComponent(accent, 48)

        if (selectedNodes.isNotEmpty()) {
            for (i in 0 until selectedNodes.size - 1) {
                val (x1, y1) = nodeCenter(selectedNodes[i])
                val (x2, y2) = nodeCenter(selectedNodes[i + 1])
                canvas.drawLine(x1, y1, x2, y2, linePaint)
            }
            if (dragging) {
                val (lx, ly) = nodeCenter(selectedNodes.last())
                canvas.drawLine(lx, ly, currentX, currentY, linePaint)
            }
        }

        for (i in 0..8) {
            val (cx, cy) = nodeCenter(i)
            val active = i in selectedNodes
            if (active) {
                canvas.drawCircle(cx, cy, 22f * d, haloPaint)
                dotPaint.color = accent
                canvas.drawCircle(cx, cy, 8f * d, dotPaint)
            } else {
                dotPaint.color = ColorUtils.setAlphaComponent(ink, 110)
                canvas.drawCircle(cx, cy, 6f * d, dotPaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                selectedNodes.clear()
                dragging = true
                currentX = event.x
                currentY = event.y
                addNodeWithIntermediate(findHitNode(event.x, event.y))
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                currentX = event.x
                currentY = event.y
                addNodeWithIntermediate(findHitNode(event.x, event.y))
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                dragging = false
                if (selectedNodes.isNotEmpty()) {
                    val pattern = selectedNodes.joinToString("-")
                    onPatternDetected?.invoke(pattern)
                }
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun clearPattern() {
        selectedNodes.clear()
        dragging = false
        invalidate()
    }
}
