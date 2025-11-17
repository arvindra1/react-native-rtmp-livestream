import React from 'react';
import { ViewProps } from 'react-native';
import { RTMPLiveStreamView as NativeView } from './index';

export default function RTMPLiveStreamView(props: ViewProps) {
  return <NativeView {...props} />;
}
