// ApiVideoLiveStreamView.tsx
import { forwardRef, useImperativeHandle, useRef } from 'react';
import { ViewStyle } from 'react-native';
import NativeApiVideoLiveStreamView, {
  Commands as NativeLiveStreamCommands,
  NativeLiveStreamProps,
  Resolution,
  OnConnectionFailedEvent,
  OnPermissionsDeniedEvent,
  OnStartStreamingEvent,
} from './NativeApiVideoLiveStreamView';

export type PredefinedResolution = '240p' | '360p' | '480p' | '720p' | '1080p';

export type ApiVideoLiveStreamProps = {
  style?: ViewStyle;
  camera?: 'front' | 'back';

  video?: {
    bitrate?: number;
    fps?: number;
    resolution?: Resolution | PredefinedResolution;
    gopDuration?: number;
  };

  isMuted?: boolean;

  audio?: {
    bitrate?: number;
    sampleRate?: 5500 | 11025 | 22050 | 44100;
    isStereo?: boolean;
  };

  zoomRatio?: number;
  enablePinchedZoom?: boolean;

  onConnectionSuccess?: () => void;
  onConnectionFailed?: (code: string) => void;
  onDisconnect?: () => void;
  onPermissionsDenied?: (permissions: string[]) => void;
  onStartStreaming?: (event: OnStartStreamingEvent) => void;
};

const DEFAULT_PROPS: NativeLiveStreamProps = {
  style: {},
  camera: 'back',
  video: {
    bitrate: 2000000,
    fps: 30,
    resolution: { width: 1280, height: 720 },
    gopDuration: 1,
  },
  isMuted: false,
  audio: {
    bitrate: 128000,
    sampleRate: 44100,
    isStereo: true,
  },
  zoomRatio: 1,
  enablePinchedZoom: true,
};

// Bitrate helper
const getDefaultBitrate = (res: Resolution) => {
  const pixels = res.width * res.height;
  if (pixels <= 102240) return 800000;
  if (pixels <= 230400) return 1000000;
  if (pixels <= 409920) return 1300000;
  if (pixels <= 921600) return 2000000;
  return 3500000;
};

// Resolution resolver
function resolveResolution(input: Resolution | PredefinedResolution): Resolution {
  const preset: Record<PredefinedResolution, Resolution> = {
    '1080p': { width: 1920, height: 1080 },
    '720p': { width: 1280, height: 720 },
    '480p': { width: 854, height: 480 },
    '360p': { width: 640, height: 360 },
    '240p': { width: 352, height: 240 },
  };

  if (typeof input === 'string') return preset[input];

  return {
    width: Math.max(input.width, input.height),
    height: Math.min(input.width, input.height),
  };
}

export type ApiVideoLiveStreamMethods = {
  startStreaming: (streamKey: string, url?: string) => Promise<boolean>;
  stopStreaming: () => void;
  setZoomRatio: (zoom: number) => void;
};

const ApiVideoLiveStreamView = forwardRef<ApiVideoLiveStreamMethods, ApiVideoLiveStreamProps>(
  (props, forwardedRef) => {
    const resolution = resolveResolution(props.video?.resolution || '720p');

    const nativeProps: NativeLiveStreamProps = {
      ...DEFAULT_PROPS,
      ...props,
      video: {
        ...DEFAULT_PROPS.video,
        ...props.video,
        bitrate: getDefaultBitrate(resolution),
        resolution,
      },
      audio: {
        ...DEFAULT_PROPS.audio,
        ...props.audio,
      },
      onConnectionFailed: props.onConnectionFailed
        ? (event: OnConnectionFailedEvent) => props.onConnectionFailed?.(event.code)
        : undefined,
      onPermissionsDenied: props.onPermissionsDenied
        ? (event: OnPermissionsDeniedEvent) => props.onPermissionsDenied?.(event.permissions)
        : undefined,
      onStartStreaming: props.onStartStreaming
        ? (event: OnStartStreamingEvent) => props.onStartStreaming?.(event)
        : undefined,
    };

    const nativeRef = useRef<any>(null);
    const nextRequestId = useRef(1);
    const requestMap = useRef<Map<number, { resolve: (v: boolean) => void; reject: (e?: string) => void }>>(new Map());

    useImperativeHandle(forwardedRef, () => ({
      startStreaming: (streamKey: string, url?: string) => {
        return new Promise<boolean>((resolve, reject) => {
          if (!nativeRef.current) return reject('Native component not ready');

          const id = nextRequestId.current++;
          requestMap.current.set(id, { resolve, reject });

          NativeLiveStreamCommands.startStreaming(nativeRef.current, id, streamKey, url);
        });
      },
      stopStreaming: () => {
        if (nativeRef.current) NativeLiveStreamCommands.stopStreaming(nativeRef.current);
      },
      setZoomRatio: (zoom: number) => {
        if (nativeRef.current) NativeLiveStreamCommands.setZoomRatioCommand(nativeRef.current, zoom);
      },
    }));

    return <NativeApiVideoLiveStreamView {...nativeProps} ref={nativeRef} />;
  }
);

export { ApiVideoLiveStreamView };
export type { Resolution } from './NativeApiVideoLiveStreamView';
