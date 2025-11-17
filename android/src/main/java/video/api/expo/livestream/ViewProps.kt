package video.api.expo.livestream

import com.facebook.react.common.MapBuilder

object ViewProps {

  // React props
  const val AUDIO_CONFIG = "audio"
  const val VIDEO_CONFIG = "video"
  const val IS_MUTED = "isMuted"
  const val CAMERA = "camera"
  const val ZOOM_RATIO = "zoomRatio"
  const val ZOOM_ENABLED = "enablePinchedZoom"

  // Audio & Video configuration fields
  const val BITRATE = "bitrate"
  const val RESOLUTION = "resolution"
  const val WIDTH = "width"
  const val HEIGHT = "height"
  const val FPS = "fps"
  const val GOP_DURATION = "gopDuration"
  const val SAMPLE_RATE = "sampleRate"
  const val IS_STEREO = "isStereo"

  /**
   * Events exposed to JS
   */
  enum class Events(val eventName: String) {
    CONNECTION_SUCCESS("onConnectionSuccess"),
    CONNECTION_FAILED("onConnectionFailed"),
    DISCONNECTED("onDisconnect"),

    PERMISSIONS_DENIED("onPermissionsDenied"),
    PERMISSIONS_RATIONALE("onPermissionsRationale"),

    START_STREAMING("onStartStreaming");

    companion object {
      @JvmStatic
      fun toEventsMap(): Map<String, *> {
        val builder = MapBuilder.builder<String, Map<String, String>>()

        values().forEach {
          builder.put(
            it.eventName,
            MapBuilder.of("registrationName", it.eventName)
          )
        }

        return builder.build()
      }
    }
  }

  /**
   * Commands exposed to JS
   * startStreaming, stopStreaming, setZoomRatioCommand
   */
  enum class Commands(val action: String) {
    START_STREAMING("startStreaming"),
    STOP_STREAMING("stopStreaming"),
    ZOOM_RATIO("setZoomRatioCommand");

    companion object {
      @JvmStatic
      fun toCommandsMap(): Map<String, Int> {
        return values().associate { it.action to it.ordinal }
      }
    }
  }
}
