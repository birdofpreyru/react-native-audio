import ReactNativeAudio from './NativeReactNativeAudio';

export * from './constants';
export * from './InputAudioStream';
export * from './SamplePlayer';

/**
 * Resolves to **true** if any input device is available on the machine;
 * otherwise resolves to **false**.
 */
export function getInputAvailable(): Promise<boolean> {
  return ReactNativeAudio.getInputAvailable();
}
