package com.floatingactionbubble

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.roundToInt

class FloatingActionBubbleView : View {
  constructor(context: Context?) : super(context)
  constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
    context,
    attrs,
    defStyleAttr
  )

  private val mainHandler = Handler(Looper.getMainLooper())
  private val backgroundDrawable = GradientDrawable()
  private var sizePx: Int = dpToPx(DEFAULT_SIZE_DP)
  private var bubbleColor: Int = Color.WHITE
  private var borderColor: Int = Color.WHITE
  private var borderWidthPx: Int = 0
  private var bubbleOpacity: Float = 1f
  private var borderOpacity: Float = 1f
  private var autoFade: Boolean = false
  private var autoFadeOpacity: Float = DEFAULT_AUTO_FADE_OPACITY
  private var autoFadeTimingMs: Long = DEFAULT_AUTO_FADE_TIMING_MS

  private var dragDX = 0f
  private var dragDY = 0f

  private val fadeRunnable = Runnable {
    if (autoFade) {
      animate().alpha(autoFadeOpacity.coerceIn(0f, 1f)).setDuration(200).start()
    }
  }

  init {
    backgroundDrawable.shape = GradientDrawable.OVAL
    background = backgroundDrawable
    isClickable = true
    applyVisuals()
    setOnTouchListener { v, event ->
      when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
          dragDX = event.rawX - v.x
          dragDY = event.rawY - v.y
          animate().cancel()
          alpha = bubbleOpacity.coerceIn(0f, 1f)
          mainHandler.removeCallbacks(fadeRunnable)
          true
        }
        MotionEvent.ACTION_MOVE -> {
          val parentView = v.parent as? View
          val maxWidth = (parentView?.width ?: v.rootView.width) - v.width
          val maxHeight = (parentView?.height ?: v.rootView.height) - v.height
          val targetX = (event.rawX - dragDX).coerceIn(0f, maxWidth.toFloat())
          val targetY = (event.rawY - dragDY).coerceIn(0f, maxHeight.toFloat())
          v.x = targetX
          v.y = targetY
          true
        }
        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
          scheduleAutoFade()
          true
        }
        else -> false
      }
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    setMeasuredDimension(sizePx, sizePx)
  }

  fun setSize(sizeDp: Float) {
    if (sizeDp <= 0f) return
    sizePx = dpToPx(sizeDp)
    requestLayout()
  }

  fun setBubbleColor(color: Int) {
    bubbleColor = color
    applyVisuals()
  }

  fun setBorderColor(color: Int) {
    borderColor = color
    applyVisuals()
  }

  fun setBorderWidth(borderWidthDp: Float) {
    borderWidthPx = if (borderWidthDp <= 0f) 0 else dpToPx(borderWidthDp)
    applyVisuals()
  }

  fun setBubbleOpacity(opacity: Float) {
    bubbleOpacity = opacity.coerceIn(0f, 1f)
    alpha = bubbleOpacity
  }

  fun setBorderOpacity(opacity: Float) {
    borderOpacity = opacity.coerceIn(0f, 1f)
    applyVisuals()
  }

  fun setAutoFade(enabled: Boolean) {
    autoFade = enabled
    scheduleAutoFade()
  }

  fun setAutoFadeOpacity(opacity: Float) {
    autoFadeOpacity = opacity.coerceIn(0f, 1f)
    scheduleAutoFade()
  }

  fun setAutoFadeTimingMs(timingMs: Int) {
    autoFadeTimingMs = timingMs.toLong().coerceAtLeast(0L)
    scheduleAutoFade()
  }

  fun applyOverlayConfig(
    sizeDp: Float?,
    color: Int?,
    borderColor: Int?,
    borderWidthDp: Float?,
    bubbleOpacity: Float?,
    borderOpacity: Float?,
    autoFade: Boolean?,
    autoFadeOpacity: Float?,
    autoFadeTimingMs: Int?
  ) {
    if (sizeDp != null) setSize(sizeDp)
    if (color != null) setBubbleColor(color)
    if (borderColor != null) setBorderColor(borderColor)
    if (borderWidthDp != null) setBorderWidth(borderWidthDp)
    if (bubbleOpacity != null) setBubbleOpacity(bubbleOpacity)
    if (borderOpacity != null) setBorderOpacity(borderOpacity)
    if (autoFade != null) setAutoFade(autoFade)
    if (autoFadeOpacity != null) setAutoFadeOpacity(autoFadeOpacity)
    if (autoFadeTimingMs != null) setAutoFadeTimingMs(autoFadeTimingMs)
  }

  private fun applyVisuals() {
    val fillAlpha = (bubbleOpacity.coerceIn(0f, 1f) * 255).roundToInt()
    val strokeAlpha = (borderOpacity.coerceIn(0f, 1f) * 255).roundToInt()
    val fillColor = Color.argb(fillAlpha, Color.red(bubbleColor), Color.green(bubbleColor), Color.blue(bubbleColor))
    val strokeColor = Color.argb(strokeAlpha, Color.red(borderColor), Color.green(borderColor), Color.blue(borderColor))
    backgroundDrawable.setColor(fillColor)
    backgroundDrawable.setStroke(borderWidthPx, strokeColor)
  }

  private fun scheduleAutoFade() {
    mainHandler.removeCallbacks(fadeRunnable)
    if (autoFade) {
      mainHandler.postDelayed(fadeRunnable, autoFadeTimingMs)
    }
  }

  private fun dpToPx(dp: Float): Int {
    val density = resources.displayMetrics.density
    return (dp * density).roundToInt()
  }

  companion object {
    private const val DEFAULT_SIZE_DP = 48f
    private const val DEFAULT_AUTO_FADE_OPACITY = 0.2f
    private const val DEFAULT_AUTO_FADE_TIMING_MS = 2000L
  }
}
