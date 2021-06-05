import { Buffer } from 'buffer';
import {
  EmitterSubscription,
  NativeEventEmitter,
  NativeModules,
} from 'react-native';

import type {
  AUDIO_FORMATS,
  AUDIO_SOURCES,
  CHANNEL_CONFIGS,
} from './constants';

const { ReactNativeAudio } = NativeModules;

type ChunkListener = (chunk: Buffer, chunkId: number) => void;
type ErrorListener = (error: Error) => void;

const eventEmitter = new NativeEventEmitter(ReactNativeAudio);

export class InputAudioStream {
  readonly audioSource: AUDIO_SOURCES;
  readonly sampleRate: number;
  readonly channelConfig: CHANNEL_CONFIGS;
  readonly audioFormat: AUDIO_FORMATS;
  readonly samplingSize: number;

  private streamId?: number;
  private chunkListeners: ChunkListener[] = [];
  private errorListeners: ErrorListener[] = [];

  private chunkSubscription?: EmitterSubscription;
  private errorSubscription?: EmitterSubscription;

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
    samplingSize: number
  ) {
    this.audioSource = audioSource;
    this.sampleRate = sampleRate;
    this.channelConfig = channelConfig;
    this.audioFormat = audioFormat;
    this.samplingSize = samplingSize;
  }

  addChunkListener(listener: ChunkListener) {
    this.chunkListeners.push(listener);
  }

  addErrorListener(listener: ErrorListener) {
    this.errorListeners.push(listener);
  }

  async destroy() {
    this.chunkListeners = [];
    this.chunkSubscription!.remove();
    this.errorSubscription!.remove();
    ReactNativeAudio.unlisten(this.streamId);
  }

  mute() {
    ReactNativeAudio.muteInputStream(this.streamId, true);
  }

  removeChunkListener(listener: ChunkListener) {
    const idx = this.chunkListeners.indexOf(listener);
    if (idx >= 0) this.chunkListeners.splice(idx, 1);
  }

  removeErrorListener(listener: ErrorListener) {
    const idx = this.errorListeners.indexOf(listener);
    if (idx >= 0) this.errorListeners.splice(idx, 1);
  }

  async start() {
    this.streamId = await ReactNativeAudio.listen(
      this.audioSource,
      this.sampleRate,
      this.channelConfig,
      this.audioFormat,
      this.samplingSize
    );
    this.chunkSubscription = eventEmitter.addListener(
      `RNA_AudioChunk`,
      ({ chunkId, data }: { chunkId: number; data: string }) => {
        if (this.chunkListeners.length) {
          const chunk = Buffer.from(data, 'base64');
          this.chunkListeners.forEach((listener) => listener(chunk, chunkId));
        }
      }
    );
    this.errorSubscription = eventEmitter.addListener(
      `RNA_InputAudioStreamError`,
      (error: string) => {
        if (this.errorListeners.length) {
          const err = new Error(error);
          this.errorListeners.forEach((listener) => listener(err));
        }
      }
    );
  }

  unmute() {
    ReactNativeAudio.muteInputStream(this.streamId, false);
  }
}
