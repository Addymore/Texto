package dev.texto.privacy

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import dev.octoshrimpy.quik.R
import kotlin.math.abs
import androidx.core.view.NestedScrollingParent3
import androidx.core.view.NestedScrollingParentHelper
import androidx.core.view.ViewCompat

/** Drag up to collapse the viewing area; pull down at the top to reach/unlock private messages. */
class ReachableMessagesLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs), NestedScrollingParent3 {
    private val privateGesture = TwoFingerPull()
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> privateGesture.reset()
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2 && !findViewById<RecyclerView>(R.id.recyclerView).canScrollVertically(-1)
                    && event.getY(0) < findViewById<View>(R.id.textoNavigation).top
                    && event.getY(1) < findViewById<View>(R.id.textoNavigation).top) privateGesture.begin(
                    event.getPointerId(0), event.getY(0), event.getPointerId(1), event.getY(1))
                else privateGesture.reset()
            }
            MotionEvent.ACTION_MOVE -> if (event.pointerCount == 2) {
                privateGesture.move(event.getPointerId(0), event.getY(0), event.getPointerId(1), event.getY(1), dp(72).toFloat())
            }
            MotionEvent.ACTION_CANCEL -> privateGesture.reset()
        }
        val result = super.dispatchTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            val list = findViewById<RecyclerView>(R.id.recyclerView)
            val unlock = privatePullEnabled && header.visibility == View.VISIBLE &&
                !list.canScrollVertically(-1) && privateGesture.armed
            privateGesture.reset()
            if (unlock) {
                settleTo(restingHeight)
                performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                onPrivatePull?.invoke()
            }
        }
        return result
    }
    private val nestedHelper = NestedScrollingParentHelper(this)
    var onPrivatePull: (() -> Unit)? = null
    var privatePullEnabled = true
    var restingSubtitle = "Messages, comfortably within reach"
    private var downX = 0f; private var downY = 0f; private var initialHeight = 0
    private var dragging = false
    private var desiredHeight = -1
    private var frameQueued = false
    private var settle: android.animation.ValueAnimator? = null
    private fun scheduleResize(value: Int) {
        desiredHeight = value
        if (!frameQueued) { frameQueued = true; postOnAnimation { frameQueued = false; if (desiredHeight >= 0) resize(desiredHeight) } }
    }
    private fun settleTo(value: Int) {
        desiredHeight = -1
        settle?.cancel()
        if (TextoAppearance.prefs(context).getBoolean("reduce_motion",false)) { resize(value); return }
        settle = android.animation.ValueAnimator.ofInt(header.height,value).apply {
            duration = 220; interpolator = android.view.animation.DecelerateInterpolator(1.5f)
            addUpdateListener { resize(it.animatedValue as Int) }; start()
        }
    }
    override fun onDetachedFromWindow() { settle?.cancel(); super.onDetachedFromWindow() }
    private var initialLayout = true
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private val restingHeight get() = (height * 0.30f).toInt().coerceIn(dp(160), dp(280))
    private val header get() = findViewById<View>(R.id.textoHeader)
    override fun onFinishInflate() {
        super.onFinishInflate()
        post { if (header.visibility == View.VISIBLE) resize(restingHeight) }
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (initialLayout && h > 0) { initialLayout = false; post { if (header.visibility == View.VISIBLE) resize(restingHeight) } }
    }
    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (header.visibility != View.VISIBLE) return super.onInterceptTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { settle?.cancel(); downX = event.x; downY = event.y; initialHeight = header.height; dragging = false }
            MotionEvent.ACTION_MOVE -> {
                val dy = event.y - downY
                val list = findViewById<RecyclerView>(R.id.recyclerView)
                val nav = findViewById<View>(R.id.textoNavigation)
                if (downY >= nav.top || downY >= list.top) return false // RecyclerView uses nested scrolling; never steal its fling.
                if (abs(dy) > slop && abs(dy) > abs(event.x - downX) * 1.4f &&
                    ((dy < 0 && initialHeight > dp(72)) || (dy > 0 && !list.canScrollVertically(-1)))) {
                    dragging = true; parent?.requestDisallowInterceptTouchEvent(true); return true
                }
            }
        }
        return super.onInterceptTouchEvent(event)
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!dragging) return super.onTouchEvent(event)
        when (event.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                val dy = event.y - downY
                val next = (initialHeight + dy * 0.65f).toInt().coerceIn(dp(72), restingHeight + dp(110))
                scheduleResize(next)
                findViewById<TextView>(R.id.textoSubtitle).text = if (privatePullEnabled && privateGesture.armed && next >= restingHeight + dp(72)) "Private messages" else restingSubtitle
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val target = if (header.height > restingHeight) restingHeight else header.height
                settleTo(target); dragging = false
                findViewById<TextView>(R.id.textoSubtitle).text = restingSubtitle
            }
        }
        return true
    }
    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int) = header.visibility == View.VISIBLE && axes and ViewCompat.SCROLL_AXIS_VERTICAL != 0
    override fun onStartNestedScroll(child: View, target: View, axes: Int) = onStartNestedScroll(child,target,axes,ViewCompat.TYPE_TOUCH)
    override fun onNestedScrollAccepted(child: View, target: View, axes: Int, type: Int) { settle?.cancel(); nestedHelper.onNestedScrollAccepted(child,target,axes,type) }
    override fun onNestedScrollAccepted(child: View, target: View, axes: Int) = onNestedScrollAccepted(child,target,axes,ViewCompat.TYPE_TOUCH)
    override fun getNestedScrollAxes() = nestedHelper.nestedScrollAxes
    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        val current = if (desiredHeight >= 0) desiredHeight else header.height
        val amount = when {
            dy > 0 && current > dp(72) -> dy.coerceAtMost(current-dp(72))
            dy < 0 && !target.canScrollVertically(-1) && current < restingHeight -> -((-dy).coerceAtMost(restingHeight-current))
            else -> 0
        }
        if (amount != 0) { scheduleResize(current-amount); consumed[1] += amount }
    }
    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) = onNestedPreScroll(target,dx,dy,consumed,ViewCompat.TYPE_TOUCH)
    override fun onNestedScroll(target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, type: Int, consumed: IntArray) {
        if (dyUnconsumed < 0 && type == ViewCompat.TYPE_TOUCH) {
            val current = if (desiredHeight >= 0) desiredHeight else header.height
            val next = (current-dyUnconsumed*0.55f).toInt().coerceAtMost(restingHeight+dp(110))
            scheduleResize(next); consumed[1] += dyUnconsumed
            findViewById<TextView>(R.id.textoSubtitle).text = if (privatePullEnabled && privateGesture.armed && next >= restingHeight+dp(72)) "Private messages" else restingSubtitle
        }
    }
    override fun onNestedScroll(target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int, type: Int) = onNestedScroll(target,dxConsumed,dyConsumed,dxUnconsumed,dyUnconsumed,type,IntArray(2))
    override fun onNestedScroll(target: View, dxConsumed: Int, dyConsumed: Int, dxUnconsumed: Int, dyUnconsumed: Int) = onNestedScroll(target,dxConsumed,dyConsumed,dxUnconsumed,dyUnconsumed,ViewCompat.TYPE_TOUCH)
    override fun onStopNestedScroll(target: View, type: Int) {
        nestedHelper.onStopNestedScroll(target,type)
        val current = if (desiredHeight >= 0) desiredHeight else header.height
        if (current > restingHeight) settleTo(restingHeight)
        findViewById<TextView>(R.id.textoSubtitle).text = restingSubtitle
    }
    override fun onStopNestedScroll(target: View) = onStopNestedScroll(target,ViewCompat.TYPE_TOUCH)
    override fun onNestedFling(target: View, velocityX: Float, velocityY: Float, consumed: Boolean) = false
    override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float) = false

    override fun performAccessibilityAction(action: Int, args: android.os.Bundle?): Boolean {
        if (action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD && privatePullEnabled) { onPrivatePull?.invoke(); return true }
        return super.performAccessibilityAction(action, args)
    }
    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        if (privatePullEnabled) info.addAction(AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD, "Private messages"))
    }
    private fun resize(value: Int) { if (header.layoutParams.height != value) header.layoutParams = header.layoutParams.apply { height = value } }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
