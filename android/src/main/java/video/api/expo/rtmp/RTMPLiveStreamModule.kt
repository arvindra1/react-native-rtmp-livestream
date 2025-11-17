package video.api.expo.rtmp

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class RTMPLiveStreamModule : Module() {

  override fun definition() = ModuleDefinition {
    Name("RTMPLiveStreamModule")

    // Start RTMP streaming
    AsyncFunction("startStreaming") { options: Map<String, Any> ->
      RTMPLiveStreamManager.start(options)
      true
    }

    Function("stopStreaming") {
      RTMPLiveStreamManager.stop()
    }

    Function("switchCamera") {
      RTMPLiveStreamManager.switchCamera()
    }
  }
}
