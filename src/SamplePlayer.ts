import { Emitter } from '@dr.pogodin/js-utils';

import type { ErrorListener } from './constants';
import ReactNativeAudio, { eventEmitter } from './ReactNativeAudio';

let lastSamplePlayerId: number = 0;

const errorEmitters: { [playerId: number]: Emitter<[Error]> } = {};

eventEmitter.addListener('RNA_SamplePlayerError', ({ playerId, error }) => {
  const emitter = errorEmitters[playerId];
  if (emitter && emitter.hasListeners) emitter.emit(Error(error));
});

export class SamplePlayer {
  private id: number;

  constructor() {
    this.id = ++lastSamplePlayerId;
    errorEmitters[this.id] = new Emitter();
    ReactNativeAudio.initSamplePlayer(this.id);
  }

  addErrorListener(listener: ErrorListener) {
    errorEmitters[this.id]!.addListener(listener);
  }

  destroy() {
    delete errorEmitters[this.id];
    ReactNativeAudio.destroySamplePlayer(this.id);
  }

  load(sampleName: string, samplePath: string): Promise<void> {
    return ReactNativeAudio.loadSample(this.id, sampleName, samplePath);
  }

  play(sampleName: string, loop: boolean): Promise<void> {
    return ReactNativeAudio.playSample(this.id, sampleName, loop);
  }

  removeErrorListener(listener: ErrorListener) {
    errorEmitters[this.id]!.removeListener(listener);
  }

  stop(sampleName: string): Promise<void> {
    return ReactNativeAudio.stopSample(this.id, sampleName);
  }

  unload(sampleName: string): Promise<void> {
    return ReactNativeAudio.unloadSample(this.id, sampleName);
  }
}
