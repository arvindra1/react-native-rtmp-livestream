// NativeApiVideoLiveStreamView.ts
// Full Expo-compatible version (No Codegen Required)

import {
  requireNativeComponent,
  UIManager,
  findNodeHandle,
  ViewProps,
} from "react-native";

// -----------------------------
// Types
// -----------------------------

export type Camera = "front" | "back";

export type Resolution = {
  width: number;
  height: number;
};

export type OnConnectionFailedEvent = {
  code: string;
};

export type OnPermissionsDeniedEvent = {
  permissions: string[];
};

export type OnStartStreamingEvent = {
  requestId: number;
  result: boolean;
  error: string;
};

export interface NativeLiveStreamProps extends ViewProps {
  camera?: Camera;

  video: {
    bitrate: number;
    fps: number;
    resolution?: Resolution;
    gopDuration: number;
  };

  isMuted: boolean;

  audio: {
    bitrate: number;
    sampleRate?: 5500 | 11025 | 22050 | 44100;
    isStereo: boolean;
  };

  zoomRatio: number;
  enablePinchedZoom: boolean;

  onConnectionSuccess?: () => void;
  onConnectionFailed?: (event: OnConnectionFailedEvent) => void;
  onDisconnect?: () => void;

  onPermissionsDenied?: (event: OnPermissionsDeniedEvent) => void;

  // resolves internal promise
  onStartStreaming?: (event: OnStartStreamingEvent) => void;
}

// --------------------------------------------------
// Native Component Name Must Match iOS/Android Class
// --------------------------------------------------

const COMPONENT_NAME = "ApiVideoLiveStreamView";

// This loads the native view (iOS/Android)
export const NativeApiVideoLiveStreamView =
  requireNativeComponent<NativeLiveStreamProps>(COMPONENT_NAME);

// --------------------------------------------------
// Commands (expo-compatible)
// --------------------------------------------------

function getCommandId(commandName: string) {
  const manager = UIManager.getViewManagerConfig
    ? UIManager.getViewManagerConfig(COMPONENT_NAME)
    : (UIManager as any)[COMPONENT_NAME];

  return manager?.Commands?.[commandName];
}

export const Commands = {
  startStreaming(
    ref: any,
    requestId: number,
    streamKey: string,
    url: string = ""
  ) {
    const handle = findNodeHandle(ref);
    if (!handle) return;

    UIManager.dispatchViewManagerCommand(handle, getCommandId("startStreaming"), [
      requestId,
      streamKey,
      url,
    ]);
  },

  stopStreaming(ref: any) {
    const handle = findNodeHandle(ref);
    if (!handle) return;

    UIManager.dispatchViewManagerCommand(handle, getCommandId("stopStreaming"), []);
  },

  setZoomRatioCommand(ref: any, zoomRatio: number) {
    const handle = findNodeHandle(ref);
    if (!handle) return;

    UIManager.dispatchViewManagerCommand(
      handle,
      getCommandId("setZoomRatioCommand"),
      [zoomRatio]
    );
  },
};

// For typing
export type NativeLiveStreamViewType = typeof NativeApiVideoLiveStreamView;

// Default export
export default NativeApiVideoLiveStreamView;
