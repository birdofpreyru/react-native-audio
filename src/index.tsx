import { NativeModules } from 'react-native';

type ReactNativeAudioType = {
  multiply(a: number, b: number): Promise<number>;
};

const { ReactNativeAudio } = NativeModules;

export default ReactNativeAudio as ReactNativeAudioType;
