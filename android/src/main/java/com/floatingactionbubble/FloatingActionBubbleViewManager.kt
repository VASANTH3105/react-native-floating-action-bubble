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
  override fun setColor(view: FloatingActionBubbleView?, color: Int?) {
    view?.setBackgroundColor(color ?: Color.TRANSPARENT)
  }

  companion object {
    const val NAME = "FloatingActionBubbleView"
  }
}
