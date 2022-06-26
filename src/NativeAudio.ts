import type {TurboModule} from 'react-native';
import {TurboModuleRegistry} from 'react-native';

export interface Spec extends TurboModule {
  readonly getConstants: () => {
    // These are common for Android and iOS.
    AUDIO_FORMAT_PCM_8BIT: number;
    AUDIO_FORMAT_PCM_16BIT: number;
    AUDIO_FORMAT_PCM_FLOAT: number;

    CHANNEL_IN_MONO: number;
    CHANNEL_IN_STEREO: number;

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

  listen(
    audioSource: number, // Ignored on iOS.
    sampleRate: number,
    channelConfig: number,
    audioFormat: number,
    samplingSize: number,
  ): Promise<void>;

  muteInputStream(streamId: number, muted: boolean): void;
  unlisten(streamId: number): void;
}

export default TurboModuleRegistry.get<Spec>('Audio');
