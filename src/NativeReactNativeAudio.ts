import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  configAudioSystem(): Promise<void>;

  readonly getConstants: () => {
    // These are common for Android and iOS.
    AUDIO_FORMAT_PCM_8BIT: number;
    AUDIO_FORMAT_PCM_16BIT: number;
    AUDIO_FORMAT_PCM_FLOAT: number;

    CHANNEL_IN_MONO: number;
    CHANNEL_IN_STEREO: number;

    IS_MAC_CATALYST: boolean;

    // These are currently Android-only.
    AUDIO_SOURCE_CAMCODER?: number;
    AUDIO_SOURCE_DEFAULT?: number;
    AUDIO_SOURCE_MIC?: number;
    AUDIO_SOURCE_REMOTE_SUBMIX?: number;
    AUDIO_SOURCE_UNPROCESSED?: number;
    AUDIO_SOURCE_VOICE_CALL?: number;
    AUDIO_SOURCE_VOICE_COMMUNICATION?: number;
    AUDIO_SOURCE_VOICE_DOWNLINK?: number;
    AUDIO_SOURCE_VOICE_PERFORMANCE?: number;
    AUDIO_SOURCE_VOICE_RECOGNITION?: number;
    AUDIO_SOURCE_VOICE_UPLINK?: number;
  };

  getInputAvailable(): Promise<boolean>;

  listen(
    streamId: number,
    audioSource: number, // Ignored on iOS.
    sampleRate: number,
    channelConfig: number,
    audioFormat: number,
    samplingSize: number,
  ): Promise<void>;

  muteInputStream(streamId: number, muted: boolean): void;
  unlisten(streamId: number): Promise<void>;

  addListener(eventName: string): void;
  removeListeners(count: number): void;

  // Methods below are related to the SamplePlayer implementation.
  initSamplePlayer(playerId: number): Promise<void>;
  destroySamplePlayer(playerId: number): Promise<void>;

  loadSample(
    playerId: number,
    sampleName: string,
    samplePath: string,
  ): Promise<void>;

  playSample(
    playerId: number,
    sampleName: string,
    loop: boolean,
  ): Promise<void>;

  stopSample(playerId: number, sampleName: string): Promise<void>;
  unloadSample(playerId: number, sampleName: string): Promise<void>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('ReactNativeAudio');
