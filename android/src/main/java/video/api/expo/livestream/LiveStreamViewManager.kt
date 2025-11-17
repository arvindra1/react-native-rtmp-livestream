package video.api.expo.livestream

import android.util.Log
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.uimanager.ThemedReactContext
import com.facebook.react.uimanager.UIManagerHelper
import com.facebook.react.uimanager.annotations.ReactProp
import com.facebook.react.uimanager.annotations.ReactPropGroup
import video.api.expo.livestream.events.*
import video.api.expo.livestream.utils.getCameraFacing
import video.api.expo.livestream.utils.toAudioConfig
import video.api.expo.livestream.utils.toVideoConfig

class LiveStreamViewManager :
  LiveStreamViewManagerSpec<LiveStreamView>() {

  override fun getName() = NAME

  override fun createViewInstance(reactContext: ThemedReactContext): LiveStreamView {
    val view = LiveStreamView(reactContext)
    reactContext.addLifecycleEventListener(view)

    // Register all view → JS event forwarders
    view.onConnectionSuccess = {
      UIManagerHelper
        .getEventDispatcherForReactTag(reactContext, view.id)
        ?.dispatchEvent(OnConnectionSuccessEvent(view.id))
        ?: Log.e(NAME, "No event dispatcher for tag ${view.id}")
    }

    view.onConnectionFailed = { reason ->
      UIManagerHelper
        .getEventDispatcherForReactTag(reactContext, view.id)
        ?.dispatchEvent(OnConnectionFailedEvent(view.id, reason))
        ?: Log.e(NAME, "No event dispatcher for tag ${view.id}")
    }

    view.onDisconnected = {
      UIManagerHelper
        .getEventDispatcherForReactTag(reactContext, view.id)
        ?.dispatchEvent(OnDisconnectEvent(view.id))
        ?: Log.e(NAME, "No event dispatcher for tag ${view.id}")
    }

    view.onPermissionsDenied = { permissions ->
      UIManagerHelper
        .getEventDispatcherForReactTag(reactContext, view.id)
        ?.dispatchEvent(OnPermissionsDeniedEvent(view.id, permissions))
        ?: Log.e(NAME, "No event dispatcher for tag ${view.id}")
    }

    view.onStartStreaming = { requestId, result, error ->
      UIManagerHelper
        .getEventDispatcherForReactTag(reactContext, view.id)
        ?.dispatchEvent(OnStartStreamingEvent(view.id, requestId, result, error))
        ?: Log.e(NAME, "No event dispatcher for tag ${view.id}")
    }

    return view
  }

  // ---- Props ----

  @ReactProp(name = ViewProps.VIDEO_CONFIG)
  override fun setVideo(view: LiveStreamView, value: ReadableMap?) {
    requireNotNull(value) { "Video config cannot be null" }

    if (view.isStreaming) {
      view.videoBitrate = value.getInt(ViewProps.BITRATE)
    } else {
      view.videoConfig = value.toVideoConfig()
    }
  }

  @ReactProp(name = ViewProps.AUDIO_CONFIG)
  override fun setAudio(view: LiveStreamView, value: ReadableMap?) {
    requireNotNull(value) { "Audio config cannot be null" }
    view.audioConfig = value.toAudioConfig()
  }

  @ReactProp(name = ViewProps.CAMERA)
  override fun setCamera(view: LiveStreamView, value: String?) {
    value?.let { view.camera = it.getCameraFacing() }
  }

  @ReactProp(name = ViewProps.IS_MUTED)
  override fun setIsMuted(view: LiveStreamView, value: Boolean) {
    view.isMuted = value
  }

  @ReactProp(name = ViewProps.ZOOM_ENABLED)
  override fun setEnablePinchedZoom(view: LiveStreamView, value: Boolean) {
    view.enablePinchedZoom = value
  }

  @ReactProp(name = ViewProps.ZOOM_RATIO)
  override fun setZoomRatio(view: LiveStreamView, value: Float) {
    view.zoomRatio = value
  }

  // ---- COMMANDS (Fabric way) ----
  override fun receiveCommand(
    root: LiveStreamView,
    commandId: String,
    args: ReadableArray?
  ) {
    when (commandId) {

      ViewProps.Commands.START_STREAMING.action -> {
        val requestId = args?.getInt(0) ?: 0
        val streamKey = args?.getString(1) ?: ""
        val url = args?.getString(2)
        root.startStreaming(requestId, streamKey, url)
      }

      ViewProps.Commands.STOP_STREAMING.action -> {
        root.stopStreaming()
      }

      ViewProps.Commands.ZOOM_RATIO.action -> {
        val zoomRatio = args?.getDouble(0)?.toFloat() ?: 1f
        root.zoomRatio = zoomRatio
      }
    }
  }

  // ---- Events ----
  override fun getExportedCustomDirectEventTypeConstants(): Map<String, *> {
    return ViewProps.Events.toEventsMap()
  }

  override fun onDropViewInstance(view: LiveStreamView) {
    super.onDropViewInstance(view)
    view.close()
  }

  companion object {
    const val NAME = "ApiVideoLiveStreamView"
  }
}
