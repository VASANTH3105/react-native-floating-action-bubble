package com.floatingactionbubble

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.ReadableType
import com.facebook.react.module.annotations.ReactModule
import android.graphics.Color

@ReactModule(name = FloatingActionBubbleModule.NAME)
class FloatingActionBubbleModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  override fun getName(): String = NAME

  @ReactMethod
  fun isOverlayPermissionGranted(promise: Promise) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      promise.resolve(true)
      return
    }
    promise.resolve(Settings.canDrawOverlays(reactApplicationContext))
  }

  @ReactMethod
  fun requestOverlayPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
    val activity = reactApplicationContext.currentActivity ?: return
    val intent = Intent(
      Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
      Uri.parse("package:${activity.packageName}")
    )
    activity.startActivity(intent)
  }

  @ReactMethod
  fun showOverlay(options: ReadableMap?) {
    val intent = Intent(reactApplicationContext, FloatingActionBubbleOverlayService::class.java)
    if (options != null) {
      if (options.hasKey("size") && !options.isNull("size")) {
        intent.putExtra("size", options.getDouble("size").toFloat())
      }
      if (options.hasKey("color") && !options.isNull("color")) {
        getColorFromDynamic(options, "color")?.let { intent.putExtra("color", it) }
      }
      if (options.hasKey("borderColor") && !options.isNull("borderColor")) {
        getColorFromDynamic(options, "borderColor")?.let { intent.putExtra("borderColor", it) }
      }
      if (options.hasKey("borderWidth") && !options.isNull("borderWidth")) {
        intent.putExtra("borderWidth", options.getDouble("borderWidth").toFloat())
      }
      if (options.hasKey("bubbleOpacity") && !options.isNull("bubbleOpacity")) {
        intent.putExtra("bubbleOpacity", options.getDouble("bubbleOpacity").toFloat())
      }
      if (options.hasKey("borderOpacity") && !options.isNull("borderOpacity")) {
        intent.putExtra("borderOpacity", options.getDouble("borderOpacity").toFloat())
      }
      if (options.hasKey("autoFade") && !options.isNull("autoFade")) {
        intent.putExtra("autoFade", options.getBoolean("autoFade"))
      }
      if (options.hasKey("autoFadeOpacity") && !options.isNull("autoFadeOpacity")) {
        intent.putExtra("autoFadeOpacity", options.getDouble("autoFadeOpacity").toFloat())
      }
      if (options.hasKey("autoFadeTimingMs") && !options.isNull("autoFadeTimingMs")) {
        intent.putExtra("autoFadeTimingMs", options.getInt("autoFadeTimingMs"))
      }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      ContextCompat.startForegroundService(reactApplicationContext, intent)
    } else {
      reactApplicationContext.startService(intent)
    }
  }

  @ReactMethod
  fun hideOverlay() {
    val intent = Intent(reactApplicationContext, FloatingActionBubbleOverlayService::class.java)
    reactApplicationContext.stopService(intent)
  }

  companion object {
    const val NAME = "FloatingActionBubbleModule"
  }

  private fun getColorFromDynamic(options: ReadableMap?, key: String): Int? {
    if (options == null || !options.hasKey(key) || options.isNull(key)) return null
    val dynamic = options.getDynamic(key)
    return when (dynamic.type) {
      ReadableType.Number -> dynamic.asDouble().toInt()
      ReadableType.String -> runCatching { Color.parseColor(dynamic.asString()) }.getOrNull()
      ReadableType.Map -> {
        // For RN color objects like { r, g, b, a }
        val map = dynamic.asMap() ?: return null
        runCatching {
          val r = map.getInt("r")
          val g = map.getInt("g")
          val b = map.getInt("b")
          val a = if (map.hasKey("a")) (map.getDouble("a") * 255).toInt() else 255
          Color.argb(a, r, g, b)
        }.getOrNull()
      }
      else -> null
    }
  }
}
