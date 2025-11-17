import ExpoModulesCore

public class RTMPLiveStreamModule: Module {
  public func definition() -> ModuleDefinition {
    
    Name("RTMPLiveStreamModule")

    AsyncFunction("startStreaming") { (options: [String: Any]) in
      RTMPLiveStreamManager.shared.start(options: options)
      return true
    }

    Function("stopStreaming") {
      RTMPLiveStreamManager.shared.stop()
    }

    Function("switchCamera") {
      RTMPLiveStreamManager.shared.switchCamera()
    }
  }
}
