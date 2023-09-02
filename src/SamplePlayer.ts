import ReactNativeAudio from './ReactNativeAudio';

let lastSamplePlayerId: number = 0;

export class SamplePlayer {
  private id: number;

  constructor() {
    this.id = ++lastSamplePlayerId;
    ReactNativeAudio.initSamplePlayer(this.id);
  }

  destroy() {
    ReactNativeAudio.destroySamplePlayer(this.id);
  }

  load(sampleName: string, samplePath: string): Promise<void> {
    return ReactNativeAudio.loadSample(this.id, sampleName, samplePath);
  }

  play(sampleName: string, loop: boolean): Promise<void> {
    return ReactNativeAudio.playSample(this.id, sampleName, loop);
  }

  stop(sampleName: string): Promise<void> {
    return ReactNativeAudio.stopSample(this.id, sampleName);
  }

  unload(sampleName: string): Promise<void> {
    return ReactNativeAudio.unloadSample(this.id, sampleName);
  }
}
