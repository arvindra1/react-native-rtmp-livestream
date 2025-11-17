package video.api.expo.livestream.utils

import android.util.Size
import com.facebook.react.bridge.ReadableMap
import video.api.expo.livestream.ViewProps
import video.api.livestream.enums.CameraFacingDirection
import video.api.livestream.models.AudioConfig
import video.api.livestream.models.VideoConfig
import java.security.InvalidParameterException

/**
 * Convert JS string to SDK camera facing enum.
 */
fun String.getCameraFacing(): CameraFacingDirection {
    return when (this.lowercase()) {
        "front" -> CameraFacingDirection.FRONT
        "back"  -> CameraFacingDirection.BACK
        else    -> throw InvalidParameterException("Unknown camera facing direction: $this")
    }
}

/**
 * Convert JS props → AudioConfig.
 */
fun ReadableMap.toAudioConfig(): AudioConfig {
    return AudioConfig(
        bitrate = this.getInt(ViewProps.BITRATE),
        sampleRate = this.getInt(ViewProps.SAMPLE_RATE),
        stereo = this.getBoolean(ViewProps.IS_STEREO),
        echoCanceler = true,
        noiseSuppressor = true
    )
}

/**
 * Convert JS props → VideoConfig.
 */
fun ReadableMap.toVideoConfig(): VideoConfig {
    val resolution = this.getMap(ViewProps.RESOLUTION)
        ?: throw InvalidParameterException("Missing resolution")

    return VideoConfig(
        bitrate = this.getInt(ViewProps.BITRATE),
        resolution = Size(
            resolution.getInt(ViewProps.WIDTH),
            resolution.getInt(ViewProps.HEIGHT)
        ),
        fps = this.getInt(ViewProps.FPS),
        gopDuration = this.getDouble(ViewProps.GOP_DURATION).toFloat()
    )
}
