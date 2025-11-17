import UIKit
import ExpoModulesCore

class RTMPLiveStreamView: ExpoView {
  override init(frame: CGRect) {
    super.init(frame: frame)
    RTMPLiveStreamManager.shared.attachPreview(view: self)
  }
}
