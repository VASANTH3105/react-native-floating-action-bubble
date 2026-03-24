package com.floatingactionbubble

import android.graphics.Color
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.uimanager.SimpleViewManager
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.ViewManagerDelegate
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.viewmanagers.FloatingActionBubbleViewManagerInterface
import com.facebook.react.viewmanagers.FloatingActionBubbleViewManagerDelegate

@ReactModule(name = FloatingActionBubbleViewManager.NAME)
class FloatingActionBubbleViewManager : SimpleViewManager<FloatingActionBubbleView>(),
  FloatingActionBubbleViewManagerInterface<FloatingActionBubbleView> {
  private val mDelegate: ViewManagerDelegate<FloatingActionBubbleView>

  init {
    mDelegate = FloatingActionBubbleViewManagerDelegate(this)
  }

  override fun getDelegate(): ViewManagerDelegate<FloatingActionBubbleView>? {
    return mDelegate
  }

  override fun getName(): String {
    return NAME
  }

  public override fun createViewInstance(context: ThemedReactContext): FloatingActionBubbleView {
    return FloatingActionBubbleView(context)
  }

  @ReactProp(name = "color")
  override fun setColor(view: FloatingActionBubbleView, color: Int?) {
    view.setBubbleColor(color ?: Color.WHITE)
  }

  @ReactProp(name = "size")
  override fun setSize(view: FloatingActionBubbleView, size: Float) {
    view.setSize(size)
  }

  @ReactProp(name = "borderColor")
  override fun setBorderColor(view: FloatingActionBubbleView, color: Int?) {
    view.setBorderColor(color ?: Color.WHITE)
  }

  @ReactProp(name = "borderWidth")
  override fun setBorderWidth(view: FloatingActionBubbleView, borderWidth: Float) {
    view.setBorderWidth(borderWidth)
  }

  @ReactProp(name = "bubbleOpacity")
  override fun setBubbleOpacity(view: FloatingActionBubbleView, opacity: Float) {
    view.setBubbleOpacity(opacity)
  }

  @ReactProp(name = "borderOpacity")
  override fun setBorderOpacity(view: FloatingActionBubbleView, opacity: Float) {
    view.setBorderOpacity(opacity)
  }

  @ReactProp(name = "autoFade")
  override fun setAutoFade(view: FloatingActionBubbleView, enabled: Boolean) {
    view.setAutoFade(enabled)
  }

  @ReactProp(name = "autoFadeOpacity")
  override fun setAutoFadeOpacity(view: FloatingActionBubbleView, opacity: Float) {
    view.setAutoFadeOpacity(opacity)
  }

  @ReactProp(name = "autoFadeTimingMs")
  override fun setAutoFadeTimingMs(view: FloatingActionBubbleView, value: Int) {
    view.setAutoFadeTimingMs(value)
  }

  @ReactProp(name = "onLongPressNavigate")
  override fun setOnLongPressNavigate(view: FloatingActionBubbleView, value: String?) {
    view.setOnLongPressNavigate(value)
  }

  companion object {
    const val NAME = "FloatingActionBubbleView"
  }
}
