package video.api.expo.rtmp

import android.util.Log
import android.view.SurfaceView
import net.butterflytv.rtmp_client.RTMPMuxer

object RTMPLiveStreamManager {

  private var muxer: RTMPMuxer? = null
  private var previewView: SurfaceView? = null

  fun attachPreviewView(view: SurfaceView) {
    previewView = view
  }

  fun start(options: Map<String, Any>) {
    val url = options["url"] as String
    muxer = RTMPMuxer()

    muxer?.open(url, false)
    muxer?.setVideoResolution(1280, 720)

    Log.d("RTMP", "Streaming started: $url")
  }

  fun stop() {
    muxer?.close()
    muxer = null
  }

  fun switchCamera() {
    Log.d("RTMP", "Switch camera")
  }
}
