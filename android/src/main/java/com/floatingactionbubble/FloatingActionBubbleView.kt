package com.floatingactionbubble

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.content.Intent
import android.net.Uri
import android.animation.ValueAnimator
import kotlin.math.roundToInt
import kotlin.math.abs
import kotlin.math.min

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
  private val borderDrawable = GradientDrawable()
  private val fillDrawable = GradientDrawable()
  private var layerDrawable: android.graphics.drawable.LayerDrawable? = null
  private var sizePx: Int = dpToPx(DEFAULT_SIZE_DP)
  private var bubbleColor: Int = Color.WHITE
  private var borderColor: Int = Color.WHITE
  private var borderWidthPx: Int = 0
  private var bubbleOpacity: Float = 1f
  private var borderOpacity: Float = 1f
  private var autoFade: Boolean = false
  private var autoFadeOpacity: Float = DEFAULT_AUTO_FADE_OPACITY
  private var autoFadeTimingMs: Long = DEFAULT_AUTO_FADE_TIMING_MS
  private var onLongPressNavigate: String? = null
  private var positionSticky: Boolean = false
  private var stickyShapeAdaptive: Boolean = true
  private var stickyCornerRadiusPx: Float = dpToPx(DEFAULT_STICKY_RADIUS_DP).toFloat()
  private var stickyEdge: StickyEdge = StickyEdge.NONE
  private var currentCornerRadii: FloatArray? = null

  private var dragDX = 0f
  private var dragDY = 0f
  private var downRawX = 0f
  private var downRawY = 0f
  private var longPressTriggered = false

  private val longPressHandler = Handler(Looper.getMainLooper())
  private val longPressRunnable = Runnable {
    if (!longPressTriggered) {
      longPressTriggered = true
      openDeepLink(onLongPressNavigate)
    }
  }

  private val fadeRunnable = Runnable {
    if (autoFade) {
      animate().alpha(autoFadeOpacity.coerceIn(0f, 1f)).setDuration(200).start()
    }
  }

  init {
    backgroundDrawable.shape = GradientDrawable.RECTANGLE
    borderDrawable.shape = GradientDrawable.RECTANGLE
    fillDrawable.shape = GradientDrawable.RECTANGLE
    updateBackgroundLayers()
    isClickable = true
    applyVisuals()
    setOnTouchListener { v, event ->
      when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
          longPressTriggered = false
          dragDX = event.rawX - v.x
          dragDY = event.rawY - v.y
          downRawX = event.rawX
          downRawY = event.rawY
          animate().cancel()
          alpha = 1f
          animateToCircleShape()
          mainHandler.removeCallbacks(fadeRunnable)
          if (!onLongPressNavigate.isNullOrBlank()) {
            longPressHandler.postDelayed(
              longPressRunnable,
              ViewConfiguration.getLongPressTimeout().toLong()
            )
          }
          true
        }
        MotionEvent.ACTION_MOVE -> {
          val moveX = abs(event.rawX - downRawX)
          val moveY = abs(event.rawY - downRawY)
          if (moveX > 10 || moveY > 10) {
            longPressHandler.removeCallbacks(longPressRunnable)
          }
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
          longPressHandler.removeCallbacks(longPressRunnable)
          if (positionSticky) {
            snapToNearestEdge(v.parent as? View)
          } else {
            stickyEdge = StickyEdge.NONE
            animateToCircleShape()
          }
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
    updateBackgroundLayers()
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

  fun setOnLongPressNavigate(path: String?) {
    onLongPressNavigate = path
  }

  fun setPositionSticky(enabled: Boolean) {
    positionSticky = enabled
  }

  fun setStickyShapeAdaptive(enabled: Boolean) {
    stickyShapeAdaptive = enabled
  }

  fun setStickyCornerRadius(radiusDp: Float) {
    stickyCornerRadiusPx = dpToPx(radiusDp).toFloat()
  }

  fun triggerAutoFade() {
    scheduleAutoFade()
  }

  fun cancelAutoFade() {
    mainHandler.removeCallbacks(fadeRunnable)
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
    autoFadeTimingMs: Int?,
    onLongPressNavigate: String?,
    positionSticky: Boolean?,
    stickyShapeAdaptive: Boolean?,
    stickyCornerRadius: Float?
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
    if (!onLongPressNavigate.isNullOrBlank()) setOnLongPressNavigate(onLongPressNavigate)
    if (positionSticky != null) setPositionSticky(positionSticky)
    if (stickyShapeAdaptive != null) setStickyShapeAdaptive(stickyShapeAdaptive)
    if (stickyCornerRadius != null) setStickyCornerRadius(stickyCornerRadius)
  }

  private fun applyVisuals() {
    val fillAlpha = (bubbleOpacity.coerceIn(0f, 1f) * 255).roundToInt()
    val strokeAlpha = (borderOpacity.coerceIn(0f, 1f) * 255).roundToInt()
    val fillColor = Color.argb(fillAlpha, Color.red(bubbleColor), Color.green(bubbleColor), Color.blue(bubbleColor))
    val strokeColor = Color.argb(strokeAlpha, Color.red(borderColor), Color.green(borderColor), Color.blue(borderColor))
    backgroundDrawable.setColor(fillColor)
    borderDrawable.setColor(strokeColor)
    fillDrawable.setColor(fillColor)
  }

  private fun snapToNearestEdge(parentView: View?) {
    val host = parentView ?: return
    val maxX = (host.width - width).coerceAtLeast(0)
    val maxY = (host.height - height).coerceAtLeast(0)
    val left = x
    val top = y
    val right = maxX - x
    val bottom = maxY - y

    val min = listOf(left to StickyEdge.LEFT, right to StickyEdge.RIGHT, top to StickyEdge.TOP, bottom to StickyEdge.BOTTOM)
      .minByOrNull { it.first } ?: return

    stickyEdge = min.second
    when (stickyEdge) {
      StickyEdge.LEFT -> x = 0f
      StickyEdge.RIGHT -> x = maxX.toFloat()
      StickyEdge.TOP -> y = 0f
      StickyEdge.BOTTOM -> y = maxY.toFloat()
      StickyEdge.NONE -> {}
    }

    if (stickyShapeAdaptive) {
      animateToStickyEdge(stickyEdge)
    } else {
      animateToCircleShape()
    }
  }

  internal fun setCircleShape() {
    val r = getCircleRadius()
    applyCornerRadii(floatArrayOf(r, r, r, r, r, r, r, r))
  }

  private fun applyStickyShape() {
    val r = stickyCornerRadiusPx
    val radii = when (stickyEdge) {
      StickyEdge.LEFT -> floatArrayOf(0f, 0f, r, r, r, r, 0f, 0f)
      StickyEdge.RIGHT -> floatArrayOf(r, r, 0f, 0f, 0f, 0f, r, r)
      StickyEdge.TOP -> floatArrayOf(0f, 0f, 0f, 0f, r, r, r, r)
      StickyEdge.BOTTOM -> floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
      StickyEdge.NONE -> floatArrayOf(r, r, r, r, r, r, r, r)
    }
    applyCornerRadii(radii)
  }

  private fun applyStickyEdge(edge: StickyEdge) {
    stickyEdge = edge
    if (stickyShapeAdaptive) {
      applyStickyShape()
    } else {
      setCircleShape()
    }
  }

  internal fun applyStickyEdgeFromOverlay(edge: String) {
    val mapped = when (edge) {
      "LEFT" -> StickyEdge.LEFT
      "RIGHT" -> StickyEdge.RIGHT
      "TOP" -> StickyEdge.TOP
      "BOTTOM" -> StickyEdge.BOTTOM
      else -> StickyEdge.NONE
    }
    applyStickyEdge(mapped)
  }

  internal fun animateToStickyEdgeFromOverlay(edge: String) {
    val mapped = when (edge) {
      "LEFT" -> StickyEdge.LEFT
      "RIGHT" -> StickyEdge.RIGHT
      "TOP" -> StickyEdge.TOP
      "BOTTOM" -> StickyEdge.BOTTOM
      else -> StickyEdge.NONE
    }
    animateToStickyEdge(mapped)
  }

  internal fun animateToCircleShapeFromOverlay() {
    animateToCircleShape()
  }

  private fun animateToCircleShape() {
    val r = getCircleRadius()
    animateCornerRadii(floatArrayOf(r, r, r, r, r, r, r, r))
  }

  private fun animateToStickyEdge(edge: StickyEdge) {
    stickyEdge = edge
    if (!stickyShapeAdaptive) {
      animateToCircleShape()
      return
    }
    val r = stickyCornerRadiusPx
    val target = when (stickyEdge) {
      StickyEdge.LEFT -> floatArrayOf(0f, 0f, r, r, r, r, 0f, 0f)
      StickyEdge.RIGHT -> floatArrayOf(r, r, 0f, 0f, 0f, 0f, r, r)
      StickyEdge.TOP -> floatArrayOf(0f, 0f, 0f, 0f, r, r, r, r)
      StickyEdge.BOTTOM -> floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
      StickyEdge.NONE -> floatArrayOf(r, r, r, r, r, r, r, r)
    }
    animateCornerRadii(target)
  }

  private fun animateCornerRadii(target: FloatArray) {
    val start = currentCornerRadii ?: target.clone()
    if (start.contentEquals(target)) {
      applyCornerRadii(target)
      return
    }
    val animator = ValueAnimator.ofFloat(0f, 1f)
    animator.duration = SHAPE_ANIMATION_MS
    animator.addUpdateListener { valueAnimator ->
      val t = valueAnimator.animatedValue as Float
      val interpolated = FloatArray(8)
      for (i in 0 until 8) {
        interpolated[i] = start[i] + (target[i] - start[i]) * t
      }
      applyCornerRadii(interpolated)
    }
    animator.start()
  }

  private fun applyCornerRadii(radii: FloatArray) {
    currentCornerRadii = radii
    backgroundDrawable.cornerRadii = radii
    borderDrawable.cornerRadii = radii
    val inset = borderWidthPx.toFloat()
    val inner = FloatArray(8)
    for (i in 0 until 8) {
      inner[i] = (radii[i] - inset).coerceAtLeast(0f)
    }
    fillDrawable.cornerRadii = inner
  }

  private fun getCircleRadius(): Float {
    val size = if (width > 0 && height > 0) min(width, height) else sizePx
    return size / 2f
  }

  private fun updateBackgroundLayers() {
    if (borderWidthPx > 0) {
      val layers = android.graphics.drawable.LayerDrawable(arrayOf(borderDrawable, fillDrawable))
      layers.setLayerInset(1, borderWidthPx, borderWidthPx, borderWidthPx, borderWidthPx)
      layerDrawable = layers
      background = layers
    } else {
      layerDrawable = null
      background = fillDrawable
    }
    // Re-apply current radii so inner/outer match new inset
    currentCornerRadii?.let { applyCornerRadii(it) }
  }

  private fun scheduleAutoFade() {
    mainHandler.removeCallbacks(fadeRunnable)
    if (autoFade) {
      mainHandler.postDelayed(fadeRunnable, autoFadeTimingMs)
    }
  }

  private fun openDeepLink(path: String?) {
    if (path.isNullOrBlank()) return
    runCatching {
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(path))
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(intent)
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
    private const val DEFAULT_STICKY_RADIUS_DP = 12f
    private const val SHAPE_ANIMATION_MS = 180L
  }

  internal enum class StickyEdge { NONE, LEFT, RIGHT, TOP, BOTTOM }
}
