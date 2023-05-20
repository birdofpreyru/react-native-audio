import {Buffer} from 'buffer';
import {
  type AppStateStatus,
  type NativeEventSubscription,
  Alert,
  AppState,
  NativeEventEmitter,
  Platform,
} from 'react-native';

import {
  check as checkPermission,
  request as requestPermission,
  PERMISSIONS,
  RESULTS as PERMISSION_STATUS,
} from 'react-native-permissions';

import {Emitter, Semaphore} from '@dr.pogodin/js-utils';

import ReactNativeAudio from './ReactNativeAudio';

import type {AUDIO_FORMATS, AUDIO_SOURCES, CHANNEL_CONFIGS} from './constants';

type ChunkListener = (chunk: Buffer, chunkId: number) => void;
type ErrorListener = (error: Error) => void;

const eventEmitter = new NativeEventEmitter(ReactNativeAudio);

// TODO: UUID string would be better, but making "uuid" library to work for RN
// is a bit cumbersome, at least in my current understanding.
let lastInputStreamId: number = 0;

const chunkEmitters: {[streamId: number]: Emitter<[Buffer, number]>} = {};

const errorEmitters: {[streamId: number]: Emitter<[Error]>} = {};

eventEmitter.addListener('RNA_AudioChunk', ({streamId, chunkId, data}) => {
  const emitter = chunkEmitters[streamId];
  if (emitter && emitter.hasListeners) {
    const chunk = Buffer.from(data, 'base64');
    emitter.emit(chunk, chunkId);
  }
});

eventEmitter.addListener('RNA_InputAudioStreamError', ({streamId, error}) => {
  const emitter = errorEmitters[streamId];
  if (emitter && emitter.hasListeners) emitter.emit(Error(error));
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
  readonly stopInBackground: boolean;

  private _appStateSub?: NativeEventSubscription;
  private _active = false;
  private _muted = false;
  private sem = new Semaphore(true);
  private streamId: number;

  /**
   * `true` when this stream is active; `false` otherwise. Note, a muted stream
   * is still "active" - it holds native resources, and keeps listening the mic,
   * it just does not send captured data to JS layer, silently discarding them.
   * At the same time, a started stream, when it is automatically stopped in
   * background by "stopinBackground" option, is considered non-active, as it
   * releases native resources and stops listening the mic, and re-inits on
   * native side when the app returns to the foreground.
   */
  get active() {
    return this._active;
  }

  get muted() {
    return this._muted;
  }

  /**
   * Creates a new InputAudioStream.
   * @param audioSource Stream source.
   * @param sampleRate Sample rate [Hz]. 44100 Hz is recommended as it is
   *  currently the only rate that is guaranteed to work on all Android devices.
   * @param channelConfig MONO or STEREO.
   * @param audioFormat Audio format.
   * @param samplingSize Sampling (data chunk) size, expressed as a number of
   *  samples per channel in the chunk.
   * @param {boolean} [stopInBackground=true] If set `true` (default) the stream
   *  will automatically stop when the app leaves foreground, and resume when
   *  the app returns to the foreground.
   */
  constructor(
    audioSource: AUDIO_SOURCES,
    sampleRate: number,
    channelConfig: CHANNEL_CONFIGS,
    audioFormat: AUDIO_FORMATS,
    samplingSize: number,
    stopInBackground: boolean = true,
  ) {
    this.audioSource = audioSource;
    this.sampleRate = sampleRate;
    this.channelConfig = channelConfig;
    this.audioFormat = audioFormat;
    this.samplingSize = samplingSize;
    this.stopInBackground = stopInBackground;
    this.streamId = ++lastInputStreamId;
    chunkEmitters[this.streamId] = new Emitter();
    errorEmitters[this.streamId] = new Emitter();
  }

  /**
   * Adds a new chunk listener.
   * @param listener
   */
  async addChunkListener(listener: ChunkListener) {
    chunkEmitters[this.streamId]!.addListener(listener);
  }

  async addErrorListener(listener: ErrorListener) {
    errorEmitters[this.streamId]!.addListener(listener);
  }

  private _handleAppStateChange(appState: AppStateStatus) {
    if (appState === 'active') this.start();
    else this._stop();
  }

  private _configAppStateHandling() {
    if (this.stopInBackground) {
      if (!this._appStateSub) {
        this._appStateSub = AppState.addEventListener(
          'change',
          this._handleAppStateChange.bind(this),
        );
      }
    } else if (this._appStateSub) {
      this._appStateSub.remove();
      delete this._appStateSub;
    }
  }

  /**
   * Internal. Stops the audio input, if active, but keeps its listeners.
   */
  private async _stop() {
    try {
      await this.sem.seize();
      if (this._active) {
        await ReactNativeAudio.unlisten(this.streamId);
        this._active = false;
        this._muted = false;
      }
    } finally {
      this.sem.setReady(true);
    }
  }

  async stop() {
    if (this._appStateSub) {
      this._appStateSub.remove();
      delete this._appStateSub;
    }
    await this._stop();
  }

  async destroy() {
    await this._stop();
    delete chunkEmitters[this.streamId];
    delete errorEmitters[this.streamId];
  }

  async mute() {
    try {
      await this.sem.seize();
      if (this.active && !this._muted) {
        ReactNativeAudio.muteInputStream(this.streamId, true);
        this._muted = true;
      }
    } finally {
      this.sem.setReady(true);
    }
  }

  async removeChunkListener(listener: ChunkListener) {
    chunkEmitters[this.streamId]!.removeListener(listener);
  }

  async removeErrorListener(listener: ErrorListener) {
    errorEmitters[this.streamId]!.removeListener(listener);
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
    try {
      await this.sem.seize();
      if (!this._active && (await getAudioRecordingPermission())) {
        await configAudioSystem();
        this._configAppStateHandling();
        await ReactNativeAudio.listen(
          this.streamId,
          this.audioSource,
          this.sampleRate,
          this.channelConfig,
          this.audioFormat,
          this.samplingSize,
        );
        this._active = true;
      }
    } finally {
      this.sem.setReady(true);
    }
    return this._active;
  }

  async unmute() {
    try {
      await this.sem.seize();
      if (this.active && this._muted) {
        ReactNativeAudio.muteInputStream(this.streamId, false);
        this._muted = false;
      }
    } finally {
      this.sem.setReady(true);
    }
  }
}
