package com.floatingactionbubble

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.content.pm.ServiceInfo
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlin.math.roundToInt

class FloatingActionBubbleOverlayService : Service() {
  private var windowManager: WindowManager? = null
  private var bubbleView: FloatingActionBubbleView? = null
  private var layoutParams: WindowManager.LayoutParams? = null
  private var screenWidth = 0
  private var screenHeight = 0

  override fun onCreate() {
    super.onCreate()
    windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val metrics = resources.displayMetrics
    screenWidth = metrics.widthPixels
    screenHeight = metrics.heightPixels
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    ensureForeground()
    ensureOverlay(intent)
    return START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()
    bubbleView?.let { view ->
      windowManager?.removeView(view)
    }
    bubbleView = null
    layoutParams = null
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun ensureOverlay(intent: Intent?) {
    if (bubbleView == null) {
      bubbleView = FloatingActionBubbleView(applicationContext)
      layoutParams = WindowManager.LayoutParams(
        dpToPx(DEFAULT_SIZE_DP),
        dpToPx(DEFAULT_SIZE_DP),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
          WindowManager.LayoutParams.TYPE_PHONE
        },
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
          WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT
      ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = DEFAULT_START_X
        y = DEFAULT_START_Y
      }

      attachDragHandler(bubbleView!!)
      windowManager?.addView(bubbleView, layoutParams)
    }

    applyIntentConfig(intent)
  }

  private fun applyIntentConfig(intent: Intent?) {
    val view = bubbleView ?: return
    val sizeDp = intent?.getFloatExtra("size", -1f) ?: -1f
    val color = intent?.getIntExtra("color", Int.MIN_VALUE)
    val borderColor = intent?.getIntExtra("borderColor", Int.MIN_VALUE)
    val borderWidth = intent?.getFloatExtra("borderWidth", -1f) ?: -1f
    val bubbleOpacity = intent?.getFloatExtra("bubbleOpacity", -1f) ?: -1f
    val borderOpacity = intent?.getFloatExtra("borderOpacity", -1f) ?: -1f
    val autoFade = intent?.getBooleanExtra("autoFade", false)
    val autoFadeOpacity = intent?.getFloatExtra("autoFadeOpacity", -1f) ?: -1f
    val autoFadeTimingMs = intent?.getIntExtra("autoFadeTimingMs", -1) ?: -1

    view.applyOverlayConfig(
      sizeDp.takeIf { it > 0f },
      color.takeIf { it != null && it != Int.MIN_VALUE },
      borderColor.takeIf { it != null && it != Int.MIN_VALUE },
      borderWidth.takeIf { it > 0f },
      bubbleOpacity.takeIf { it >= 0f },
      borderOpacity.takeIf { it >= 0f },
      autoFade,
      autoFadeOpacity.takeIf { it >= 0f },
      autoFadeTimingMs.takeIf { it >= 0 }
    )

    if (sizeDp > 0f) {
      layoutParams?.width = dpToPx(sizeDp)
      layoutParams?.height = dpToPx(sizeDp)
      windowManager?.updateViewLayout(view, layoutParams)
    }
  }

  private fun attachDragHandler(view: View) {
    var dragDX = 0f
    var dragDY = 0f
    view.setOnTouchListener { _, event ->
      val params = layoutParams ?: return@setOnTouchListener false
      when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
          dragDX = event.rawX - params.x
          dragDY = event.rawY - params.y
          true
        }
        MotionEvent.ACTION_MOVE -> {
          val viewWidth = view.width.takeIf { it > 0 } ?: dpToPx(DEFAULT_SIZE_DP)
          val viewHeight = view.height.takeIf { it > 0 } ?: dpToPx(DEFAULT_SIZE_DP)
          val maxX = (screenWidth - viewWidth).coerceAtLeast(0)
          val maxY = (screenHeight - viewHeight).coerceAtLeast(0)
          params.x = (event.rawX - dragDX).roundToInt().coerceIn(0, maxX)
          params.y = (event.rawY - dragDY).roundToInt().coerceIn(0, maxY)
          windowManager?.updateViewLayout(view, params)
          true
        }
        else -> false
      }
    }
  }

  private fun ensureForeground() {
    val notification = createNotification()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      ServiceCompat.startForeground(
        this,
        OVERLAY_NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
      )
    } else {
      startForeground(OVERLAY_NOTIFICATION_ID, notification)
    }
  }

  private fun createNotification(): Notification {
    val channelId = "floating_action_bubble"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      if (manager.getNotificationChannel(channelId) == null) {
        val channel = NotificationChannel(
          channelId,
          "Floating Action Bubble",
          NotificationManager.IMPORTANCE_MIN
        )
        manager.createNotificationChannel(channel)
      }
    }

    return NotificationCompat.Builder(this, channelId)
      .setContentTitle("Floating Action Bubble")
      .setContentText("Bubble overlay is active")
      .setSmallIcon(android.R.drawable.ic_dialog_info)
      .setOngoing(true)
      .build()
  }

  private fun dpToPx(dp: Float): Int {
    val density = resources.displayMetrics.density
    return (dp * density).roundToInt()
  }

  companion object {
    private const val DEFAULT_SIZE_DP = 48f
    private const val DEFAULT_START_X = 100
    private const val DEFAULT_START_Y = 200
    private const val OVERLAY_NOTIFICATION_ID = 19021
  }
}
