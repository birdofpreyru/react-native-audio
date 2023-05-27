import ReactNativeAudio from './ReactNativeAudio';

export * from './constants';
export * from './InputAudioStream';

/**
 * Resolves to **true** if any input device is available on the machine;
 * otherwise resolves to **false**.
 */
export function getInputAvailable(): Promise<boolean> {
  return ReactNativeAudio.getInputAvailable();
}
