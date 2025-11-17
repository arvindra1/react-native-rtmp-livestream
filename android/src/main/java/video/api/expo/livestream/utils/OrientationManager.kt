package video.api.expo.livestream.utils

import android.content.Context
import android.view.OrientationEventListener
import java.io.Closeable

/**
 * Tracks orientation changes and exposes stable 0/90/180/270 degrees.
 */
class OrientationManager(private val context: Context) : Closeable {

    private var _currentDegrees: Int = OrientationEventListener.ORIENTATION_UNKNOWN

    private val listener = object : OrientationEventListener(context) {
        override fun onOrientationChanged(orientation: Int) {
            if (orientation == ORIENTATION_UNKNOWN) return

            val snapped = when {
                orientation in 315..360 || orientation in 0..45   -> 0
                orientation in 46..135   -> 90
                orientation in 136..225  -> 180
                orientation in 226..314  -> 270
                else -> return
            }

            if (snapped != _currentDegrees) {
                if (_currentDegrees == ORIENTATION_UNKNOWN) {
                    // Ignore first callback
                    _currentDegrees = snapped
                    return
                }
                _currentDegrees = snapped
                _orientationHasChanged = true
            }
        }
    }

    private var _orientationHasChanged = false

    /**
     * Returns true once per real orientation change.
     */
    val orientationHasChanged: Boolean
        get() {
            if (!listener.canDetectOrientation()) return true
            val changed = _orientationHasChanged
            _orientationHasChanged = false
            return changed
        }

    /**
     * Returns the current snapped orientation in degrees (0/90/180/270).
     */
    val currentOrientationDegrees: Int
        get() = _currentDegrees

    init {
        listener.enable()
    }

    override fun close() {
        listener.disable()
    }

    companion object {
        private const val ORIENTATION_UNKNOWN = OrientationEventListener.ORIENTATION_UNKNOWN
    }
}
