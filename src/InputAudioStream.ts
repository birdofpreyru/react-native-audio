import {Buffer} from 'buffer';
import {Alert, NativeEventEmitter, Platform} from 'react-native';

import {
  check as checkPermission,
  request as requestPermission,
  PERMISSIONS,
  RESULTS as PERMISSION_STATUS,
} from 'react-native-permissions';

import ReactNativeAudio from './ReactNativeAudio';

import type {AUDIO_FORMATS, AUDIO_SOURCES, CHANNEL_CONFIGS} from './constants';

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

/**
 * Configures audio system (input & output devices).
 *
 * Currently, it does nothing on Android; on iOS it re-configures audio session
 * in the best way for audio playback and recording.
 * @return {Promise<>}
 */
export async function configAudioSystem() {
  return ReactNativeAudio.configAudioSystem();
}

/**
 * Attempts to check and/or request (if necessary and possible) audio recording
 * permission.
 * @return {Promise<boolean>} Resolves "true" if at least limited audio
 * recording permissions are granted on the exit from this function.
 */
async function getAudioRecordingPermission() {
  let permission;
  switch (Platform.OS) {
    case 'android':
      permission = PERMISSIONS.ANDROID.RECORD_AUDIO;
      break;
    case 'ios':
      permission = PERMISSIONS.IOS.MICROPHONE;
      break;
    default:
      throw Error('Invalid OS');
  }

  let status = await checkPermission(permission);
  switch (status) {
    case PERMISSION_STATUS.UNAVAILABLE:
      Alert.alert(
        'Microphone unavailable',
        'Microphone is not accessible at your system',
      );
      break;
    case PERMISSION_STATUS.BLOCKED:
      Alert.alert(
        'Microphone access blocked',
        'Microphone access have been forbidden before. Allow it in the device settigns, or re-install the app to automatically ask for it again',
      );
      break;
    case PERMISSION_STATUS.DENIED:
      status = await requestPermission(permission);
      break;
    case PERMISSION_STATUS.GRANTED:
    case PERMISSION_STATUS.LIMITED:
      break;
    default:
      throw Error('Unexpected permission status');
  }

  const permitted =
    status === PERMISSION_STATUS.GRANTED ||
    status === PERMISSION_STATUS.LIMITED;

  if (!permitted) {
    Alert.alert(
      'Microphone access forbidden',
      'Microphone access is forbidden. Allow it in the device settigns, or re-install the app to automatically ask for it again',
    );
  }

  return permitted;
}

export class InputAudioStream {
  readonly audioSource: AUDIO_SOURCES;
  readonly sampleRate: number;
  readonly channelConfig: CHANNEL_CONFIGS;
  readonly audioFormat: AUDIO_FORMATS;
  readonly samplingSize: number;

  private streamId?: Promise<number | undefined>;
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
   * Gets the actual stream ID number (or undefined).
   */
  async getStreamId() {
    return this.streamId && (await this.streamId);
  }

  /**
   * Adds a new chunk listener.
   * @param listener
   */
  async addChunkListener(listener: ChunkListener) {
    if (!this.chunkListeners.includes(listener)) {
      this.chunkListeners.push(listener);
      const streamId = await this.getStreamId();
      if (streamId !== undefined) {
        chunkListeners.push({streamId, listener});
      }
    }
  }

  async addErrorListener(listener: ErrorListener) {
    if (!this.errorListeners.includes(listener)) {
      this.errorListeners.push(listener);
      const streamId = await this.getStreamId();
      if (streamId !== undefined) {
        errorListeners.push({streamId, listener});
      }
    }
  }

  async destroy() {
    const streamId = await this.getStreamId();
    if (streamId !== undefined) {
      chunkListeners = chunkListeners.filter(
        item => item.streamId !== streamId,
      );
      errorListeners = errorListeners.filter(
        item => item.streamId !== streamId,
      );
      ReactNativeAudio.unlisten(streamId);
    }
  }

  async mute() {
    const streamId = await this.getStreamId();
    if (streamId !== undefined) {
      ReactNativeAudio.muteInputStream(streamId, true);
    }
  }

  async removeChunkListener(listener: ChunkListener) {
    const streamId = await this.getStreamId();
    if (streamId !== undefined) {
      chunkListeners = chunkListeners.filter(
        item => item.streamId !== streamId || item.listener !== listener,
      );
    }
  }

  async removeErrorListener(listener: ErrorListener) {
    const streamId = await this.getStreamId();
    if (streamId !== undefined) {
      errorListeners = errorListeners.filter(
        item => item.streamId !== streamId || item.listener !== listener,
      );
    }
  }

  /**
   * Attempts to start the input audio stream. In case the stream is already
   * active, it just reports the active state. If the stream is pending to start
   * it waits and returns the result, without doing a new attempt in case of
   * failure.
   * @return Resolves "true" if the stream is active upon the call exit;
   * otherwise resolves "false".
   */
  async start(): Promise<boolean> {
    if (!this.streamId) {
      this.streamId = (async () => {
        try {
          if (await getAudioRecordingPermission()) {
            await configAudioSystem();
            const streamId = await ReactNativeAudio.listen(
              this.audioSource,
              this.sampleRate,
              this.channelConfig,
              this.audioFormat,
              this.samplingSize,
            );
            this.chunkListeners.forEach(item =>
              chunkListeners.push({
                listener: item,
                streamId,
              }),
            );
            this.errorListeners.forEach(item =>
              errorListeners.push({
                listener: item,
                streamId,
              }),
            );
            return streamId;
          }
        } catch (error) {
          // If an error happens we just fallthrough to reset this.streamId and
          // return "undefined". Which returns the object to its initial state
          // allowing to retry the start later, if needed.
        }
        this.streamId = undefined;
        return undefined;
      })();
    }
    return !!(await this.streamId);
  }

  async unmute() {
    const streamId = await this.getStreamId();
    if (streamId !== undefined) {
      ReactNativeAudio.muteInputStream(streamId, false);
    }
  }
}
