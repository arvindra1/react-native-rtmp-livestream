import { requireNativeModule, requireNativeViewManager } from 'expo-modules-core';
import { RTMPLiveStreamOptions } from './RTMPLiveStream.types';

const RTMPLiveStreamModule =
  requireNativeModule('RTMPLiveStreamModule');

export const RTMPLiveStreamView =
  requireNativeViewManager('RTMPLiveStreamView');

export function startStreaming(options: RTMPLiveStreamOptions) {
  return RTMPLiveStreamModule.startStreaming(options);
}

export function stopStreaming() {
  return RTMPLiveStreamModule.stopStreaming();
}

export function switchCamera() {
  return RTMPLiveStreamModule.switchCamera();
}
