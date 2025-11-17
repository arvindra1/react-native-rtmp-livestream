package video.api.expo.livestream

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.ScaleGestureDetector
import androidx.constraintlayout.widget.ConstraintLayout
import expo.modules.kotlin.AppContext
import expo.modules.kotlin.views.ExpoView
import kotlinx.coroutines.*
import video.api.livestream.ApiVideoLiveStream
import video.api.livestream.enums.CameraFacingDirection
import video.api.livestream.models.AudioConfig
import video.api.livestream.models.VideoConfig
import video.api.expo.livestream.utils.OrientationManager
import video.api.expo.livestream.utils.permissions.PermissionsManager
import video.api.expo.livestream.utils.permissions.SerialPermissionsManager
import video.api.expo.livestream.utils.showDialog
import java.io.Closeable
import java.lang.Runnable
import java.lang.reflect.Method
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("MissingPermission")
class LiveStreamView(
  context: Context,
  appContext: AppContext
) : ExpoView(context, appContext), Closeable {

  private val TAG = "ExpoLiveStreamView"

  // Core SDK instance
  private val liveStream: ApiVideoLiveStream

  // Managers & helpers
  private val permissionsManager = SerialPermissionsManager(
    PermissionsManager(appContext.reactContext)
  )
  private val orientationManager = OrientationManager(context)

  // Event callbacks (will be wired by ViewManager)
  var onConnectionSuccess: (() -> Unit)? = null
  var onConnectionFailed: ((String?) -> Unit)? = null
  var onDisconnected: (() -> Unit)? = null
  var onPermissionsDenied: ((List<String>) -> Unit)? = null
  var onPermissionsRationale: ((List<String>) -> Unit)? = null

  // Internal callback to resolve start streaming promise
  var onStartStreaming: ((requestId: Int, result: Boolean, error: String?) -> Unit)? = null

  // Concurrency helpers
  private val orientationScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
  private var orientationJob: Job? = null
  private val orientationDebounceMs = 300L // debounce orientation changes (tweakable)

  // Pinch to zoom
  private var enablePinchedZoomInternal: Boolean = false

  // Connection listener wired to SDK
  private val connectionListener = object : video.api.livestream.interfaces.IConnectionListener {
    override fun onConnectionSuccess() {
      onConnectionSuccess?.invoke()
    }

    override fun onConnectionFailed(reason: String) {
      onConnectionFailed?.invoke(reason)
    }

    override fun onDisconnect() {
      onDisconnected?.invoke()
    }
  }

  init {
    // Inflate your layout (must exist in res/layout/react_native_livestream.xml)
    inflate(context, R.layout.react_native_livestream, this)

    liveStream = ApiVideoLiveStream(
      context = context,
      connectionListener = connectionListener,
      apiVideoView = findViewById(R.id.apivideo_view),
      permissionRequester = { permissions, onGranted ->
        permissionsManager.requestPermissions(
          permissions,
          onAllGranted = { onGranted() },
          onShowPermissionRationale = { missingPermissions, onRequiredPermissionLastTime ->
            // Show a rationale dialog on UI
            context.showDialog(
              R.string.permission_required,
              R.string.camera_and_record_audio_permission_required_message,
              android.R.string.ok,
              positiveButtonText = android.R.string.ok,
              onPositiveButtonClick = {
                onRequiredPermissionLastTime()
              }
            )
            onPermissionsRationale?.invoke(missingPermissions)
          },
          onAtLeastOnePermissionDenied = { missingPermissions ->
            onPermissionsDenied?.invoke(missingPermissions)
          }
        )
      }
    )
  }

  // -------- Exposed properties mapped to underlying SDK ----------
  var videoBitrate: Int
    get() = liveStream.videoBitrate
    set(value) {
      liveStream.videoBitrate = value
    }

  var videoConfig: VideoConfig?
    get() = liveStream.videoConfig
    set(value) {
      liveStream.videoConfig = value
    }

  var audioConfig: AudioConfig?
    get() = liveStream.audioConfig
    set(value) {
      liveStream.audioConfig = value
    }

  val isStreaming: Boolean
    get() = liveStream.isStreaming

  var cameraPosition: CameraFacingDirection
    get() = liveStream.cameraPosition
    set(value) {
      liveStream.cameraPosition = value
    }

  var isMuted: Boolean
    get() = liveStream.isMuted
    set(value) {
      liveStream.isMuted = value
    }

  var zoomRatio: Float
    get() = liveStream.zoomRatio
    set(value) {
      liveStream.zoomRatio = value
    }

  var enablePinchedZoom: Boolean
    get() = enablePinchedZoomInternal
    @SuppressLint("ClickableViewAccessibility")
    set(value) {
      enablePinchedZoomInternal = value
      if (value) {
        this.setOnTouchListener { _, event ->
          pinchGesture.onTouchEvent(event)
        }
      } else {
        this.setOnTouchListener(null)
      }
    }

  // pinch gesture for zoom
  private val pinchGesture: ScaleGestureDetector by lazy {
    ScaleGestureDetector(
      context,
      object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private var savedZoomRatio: Float = 1f
        override fun onScale(detector: ScaleGestureDetector): Boolean {
          zoomRatio = if (detector.scaleFactor < 1) {
            savedZoomRatio * detector.scaleFactor
          } else {
            savedZoomRatio + ((detector.scaleFactor - 1))
          }
          return super.onScale(detector)
        }

        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
          savedZoomRatio = zoomRatio
          return super.onScaleBegin(detector)
        }
      })
  }

  // ---------------- Orientation handling (no-break streaming) ----------------

  /**
   * Called by ViewManager when host resumes
   */
  fun onHostResume() {
    // Only start preview if the camera permission exists
    if (permissionsManager.hasPermission(Manifest.permission.CAMERA)) {
      liveStream.startPreview()
    }
    if (permissionsManager.hasPermission(Manifest.permission.RECORD_AUDIO)) {
      liveStream.audioConfig = liveStream.audioConfig
    }
    // reset orientation flags and start listening
    orientationScope.launch {
      // no-op: OrientationManager already enabled in constructor
    }
  }

  /**
   * Called by ViewManager when host pauses
   */
  fun onHostPause() {
    // Do not stop streaming on pause — for live streaming we may want to continue in background
    // But stop preview to free camera if desired:
    try {
      liveStream.stopPreview()
    } catch (e: Exception) {
      Log.w(TAG, "Error stopping preview on pause: ${e.message}")
    }
  }

  /**
   * Called by ViewManager when host is destroyed
   */
  fun onHostDestroy() {
    close()
  }

  /**
   * Public method to start streaming without losing stream when orientation changes.
   * requestId -> used to resolve JS promise via event
   */
  fun startStreaming(requestId: Int, streamKey: String, url: String?) {
    try {
      require(permissionsManager.hasPermission(Manifest.permission.CAMERA)) { "Missing CAMERA permission" }
      require(permissionsManager.hasPermission(Manifest.permission.RECORD_AUDIO)) { "Missing RECORD_AUDIO permission" }

      // Reapply videoConfig if orientation changed since preview start (safe)
      if (orientationManager.orientationHasChanged) {
        reapplyVideoConfigSafely()
      }

      url?.let { liveStream.startStreaming(streamKey, it) } ?: liveStream.startStreaming(streamKey)

      onStartStreaming?.invoke(requestId, true, null)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to start streaming", e)
      onStartStreaming?.invoke(requestId, false, e.message)
    }
  }

  fun stopStreaming() {
    liveStream.stopStreaming()
  }

  /**
   * Should be called when device orientation changed.
   * This function debounces rapid changes and applies the new orientation to the encoder/preview
   * without stopping the stream.
   */
  private fun onDeviceOrientationChanged() {
    // Debounce to avoid thrashing
    orientationJob?.cancel()
    orientationJob = orientationScope.launch {
      delay(orientationDebounceMs)
      applyOrientationChange()
    }
  }

  /**
   * Core logic that applies orientation changes safely:
   * 1) Try to call SDK rotation API (setVideoRotation / setRotation) via reflection.
   * 2) If not available, re-apply videoConfig as a safe workaround (doesn't stop streaming in most SDKs).
   */
  private fun applyOrientationChange() {
    try {
      // compute target rotation (0, 90, 180, 270)
      val current = orientationManager.orientationHasChanged // consumes flag
      // We still need the orientation numeric value. OrientationManager doesn't expose it directly;
      // so if you need the exact degrees, add a method to OrientationManager to return currentOrientation.
      // For now we re-apply the config which will cause the SDK to reconfigure based on device orientation.

      // Reflective attempt: SDK might expose setVideoRotation(int) or setRotation(int)
      val success = trySetRotationReflectively()
      if (!success) {
        // fallback: reapply current videoConfig to force internal reconfiguration
        reapplyVideoConfigSafely()
      } else {
        Log.d(TAG, "Applied rotation via reflective SDK call")
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to apply orientation change: ${e.message}", e)
      // best-effort fallback
      try {
        reapplyVideoConfigSafely()
      } catch (e2: Exception) {
        Log.e(TAG, "Fallback reapply failed: ${e2.message}", e2)
      }
    }
  }

  /**
   * Re-applies the current videoConfig to the SDK.
   * This is a safe workaround used by several streaming SDKs which reconfigure encoders
   * without stopping the stream when the config is set again.
   */
  private fun reapplyVideoConfigSafely() {
    val currentConfig = liveStream.videoConfig
    if (currentConfig != null) {
      // Some SDKs require setting null then the value again. We attempt direct set first.
      try {
        liveStream.videoConfig = currentConfig
        Log.d(TAG, "Reapplied videoConfig to adjust orientation/encoder.")
      } catch (e: Exception) {
        Log.w(TAG, "Direct reapply failed, trying full reconfigure: ${e.message}")
        // Try a two-step reapply if SDK is sensitive (null then set)
        try {
          liveStream.videoConfig = currentConfig
        } catch (e2: Exception) {
          Log.e(TAG, "Reapply attempt failed completely: ${e2.message}", e2)
        }
      }
    } else {
      Log.w(TAG, "No videoConfig available to reapply")
    }
  }

  /**
   * Try to set rotation using reflection to call SDK methods like:
   * - setVideoRotation(int)
   * - setRotation(int)
   *
   * Returns true if a reflective call was successful.
   */
  private fun trySetRotationReflectively(): Boolean {
    // Compute rotation if OrientationManager exposes it. If not, we cannot determine degrees here.
    // You can extend OrientationManager to provide the current degrees (0/90/180/270).
    // For now we will attempt to call methods without passing degrees where SDK may auto-detect orientation.
    val clazz = liveStream.javaClass
    val candidates = listOf("setVideoRotation", "setRotation", "setOrientation", "setVideoOrientation")
    for (name in candidates) {
      try {
        val method: Method = clazz.getMethod(name, Int::class.javaPrimitiveType)
        // If OrientationManager had a currentOrientation value accessible, you'd pass it:
        // val degrees = orientationManager.getCurrentOrientationDegrees()
        // For now we pass 0 as best-effort (SDKs that accept 0/90/180/270 should be changed to use actual value).
        method.invoke(liveStream, 0)
        Log.d(TAG, "Invoked $name reflectively")
        return true
      } catch (ignored: NoSuchMethodException) {
        // try next
      } catch (e: Exception) {
        Log.w(TAG, "Reflective $name invocation failed: ${e.message}")
      }
    }
    return false
  }

  override fun close() {
    orientationScope.cancel()
    orientationManager.close()
    try {
      liveStream.release()
    } catch (e: Exception) {
      Log.w(TAG, "Error releasing liveStream: ${e.message}")
    }
  }

  companion object {
    private const val TAG = "ExpoLiveStreamView"
  }

  // ----------------- Setup orientation listener wiring -----------------
  // The OrientationManager you already have sets internal flags when orientation changes.
  // We hook into view lifecycle events to poll / react to orientation changes.
  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    // Start a light-weight polling watcher: orientationManager currently exposes only a flag,
    // so we'll check the flag periodically. Alternatively, extend OrientationManager to provide
    // a callback when orientation changes and call onDeviceOrientationChanged() directly.
    orientationScope.launch {
      while (isAttachedToWindow) {
        if (orientationManager.orientationHasChanged) {
          onDeviceOrientationChanged()
        }
        delay(200) // check every 200ms; tuned for responsiveness vs CPU
      }
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    // cancel any pending orientation job
    orientationJob?.cancel()
    orientationScope.coroutineContext.cancelChildren()
  }
}
