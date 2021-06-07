import {Buffer} from 'buffer';

import {NativeEventEmitter, NativeModules, Platform} from 'react-native';

import {
  check as checkPermission,
  request as requestPermission,
  PERMISSIONS,
  RESULTS as PERMISSION_STATUS,
} from 'react-native-permissions';

import type {AUDIO_FORMATS, AUDIO_SOURCES, CHANNEL_CONFIGS} from './constants';

const {ReactNativeAudio} = NativeModules;

type ChunkListener = (chunk: Buffer, chunkId: number) => void;
type ErrorListener = (error: Error) => void;

const eventEmitter = new NativeEventEmitter(ReactNativeAudio);

let chunkListeners: {
  streamId: number;
  listener: ChunkListener;
}[] = [];
let errorListeners: {
  streamId: number;
  listener: ErrorListener;
}[] = [];

eventEmitter.addListener('RNA_AudioChunk', ({streamId, chunkId, data}) => {
  const chunk = Buffer.from(data, 'base64');
  chunkListeners.forEach(item => {
    if (item.streamId === streamId) {
      item.listener(chunk, chunkId);
    }
  });
});

eventEmitter.addListener('RNA_InputAudioStreamError', ({streamId, error}) => {
  const e = Error(error);
  errorListeners.forEach(item => {
    if (item.streamId === streamId) {
      item.listener(e);
    }
  });
});

export class InputAudioStream {
  readonly audioSource: AUDIO_SOURCES;
  readonly sampleRate: number;
  readonly channelConfig: CHANNEL_CONFIGS;
  readonly audioFormat: AUDIO_FORMATS;
  readonly samplingSize: number;

  private streamId?: number;
  private chunkListeners: ChunkListener[] = [];
  private errorListeners: ErrorListener[] = [];

  /**
   * Creates a new InputAudioStream.
   * @param audioSource Stream source.
   * @param sampleRate Sample rate [Hz]. 44100 Hz is recommended as it is
   *  currently the only rate that is guaranteed to work on all Android devices.
   * @param channelConfig MONO or STEREO.
   * @param audioFormat Audio format.
   * @param samplingSize Sampling (data chunk) size, expressed as a number of
   *  samples per channel in the chunk.
   */
  constructor(
    audioSource: AUDIO_SOURCES,
    sampleRate: number,
    channelConfig: CHANNEL_CONFIGS,
    audioFormat: AUDIO_FORMATS,
    samplingSize: number,
  ) {
    this.audioSource = audioSource;
    this.sampleRate = sampleRate;
    this.channelConfig = channelConfig;
    this.audioFormat = audioFormat;
    this.samplingSize = samplingSize;
  }

  /**
   * Adds a new chunk listener.
   * @param listener
   */
  addChunkListener(listener: ChunkListener) {
    if (!this.chunkListeners.includes(listener)) {
      this.chunkListeners.push(listener);
      if (this.streamId !== undefined) {
        chunkListeners.push({streamId: this.streamId, listener});
      }
    }
  }

  addErrorListener(listener: ErrorListener) {
    if (!this.errorListeners.includes(listener)) {
      this.errorListeners.push(listener);
      if (this.streamId !== undefined) {
        errorListeners.push({streamId: this.streamId, listener});
      }
    }
  }

  async destroy() {
    if (this.streamId !== undefined) {
      chunkListeners = chunkListeners.filter(
        ({streamId}) => streamId !== this.streamId,
      );
      errorListeners = errorListeners.filter(
        ({streamId}) => streamId !== this.streamId,
      );
      ReactNativeAudio.unlisten(this.streamId);
    }
  }

  mute() {
    ReactNativeAudio.muteInputStream(this.streamId, true);
  }

  removeChunkListener(listener: ChunkListener) {
    if (this.streamId !== undefined) {
      chunkListeners = chunkListeners.filter(
        item => item.streamId !== this.streamId || item.listener !== listener,
      );
    }
  }

  removeErrorListener(listener: ErrorListener) {
    if (this.streamId !== undefined) {
      errorListeners = errorListeners.filter(
        item => item.streamId !== this.streamId || item.listener !== listener,
      );
    }
  }

  async start() {
    // Requests permission to record audio, if not granted, and requestable.
    // If no permission (at least limited) is granted in result, it bails out.
    // TODO: Clean-up the code. Can be twice more compact.
    let status;
    if (Platform.OS === 'ios') {
      status = await checkPermission(PERMISSIONS.IOS.MICROPHONE);
      if (status === PERMISSION_STATUS.DENIED) {
        status = await requestPermission(PERMISSIONS.IOS.MICROPHONE);
      }
    } else if (Platform.OS === 'android') {
      status = await checkPermission(PERMISSIONS.ANDROID.RECORD_AUDIO);
      if (status === PERMISSION_STATUS.DENIED) {
        status = await requestPermission(PERMISSIONS.ANDROID.RECORD_AUDIO);
      }
    }
    if (
      status !== PERMISSION_STATUS.GRANTED &&
      status !== PERMISSION_STATUS.LIMITED
    ) {
      return;
    }

    this.streamId = await ReactNativeAudio.listen(
      this.audioSource,
      this.sampleRate,
      this.channelConfig,
      this.audioFormat,
      this.samplingSize,
    );
    this.chunkListeners.forEach(item =>
      chunkListeners.push({
        streamId: this.streamId!,
        listener: item,
      }),
    );
    this.errorListeners.forEach(item =>
      errorListeners.push({
        streamId: this.streamId!,
        listener: item,
      }),
    );
  }

  unmute() {
    ReactNativeAudio.muteInputStream(this.streamId, false);
  }
}
