import { NativeEventEmitter } from 'react-native';

import ReactNativeAudio from './NativeReactNativeAudio';

const eventEmitter = new NativeEventEmitter(ReactNativeAudio);

export default eventEmitter;
