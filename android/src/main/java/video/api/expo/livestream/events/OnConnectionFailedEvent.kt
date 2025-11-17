package video.api.expo.livestream.events

import com.facebook.react.bridge.Arguments
import com.facebook.react.uimanager.events.Event
import com.facebook.react.uimanager.events.RCTEventEmitter
import video.api.expo.livestream.ViewProps

class OnConnectionFailedEvent(private val viewTag: Int, private val reason: String?) :
  Event<OnConnectionFailedEvent>(viewTag) {
  private val params = Arguments.createMap().apply {
    putString("code", reason)
  }

  override fun getEventName() = ViewProps.Events.CONNECTION_FAILED.eventName

  override fun dispatch(rctEventEmitter: RCTEventEmitter) {
    rctEventEmitter.receiveEvent(viewTag, eventName, params)
  }
}
