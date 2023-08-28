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

/**
 * This is a temporary function for testing playback,
 * we'll probably add better playback interface later.
 */
export function playTest() {
  ReactNativeAudio.playTest();
}
