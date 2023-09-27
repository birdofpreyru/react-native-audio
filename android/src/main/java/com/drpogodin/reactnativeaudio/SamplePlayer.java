package com.drpogodin.reactnativeaudio;

import android.media.SoundPool;

import com.facebook.react.bridge.Promise;

import com.drpogodin.reactnativeaudio.Errors;

import java.util.HashMap;

public class SamplePlayer implements SoundPool.OnLoadCompleteListener {
  private static SoundPool.Builder builder;
  private HashMap<Integer,Promise> loadPromises = new HashMap<>();
  private SoundPool pool;
  private HashMap<String,Integer> soundIds = new HashMap<>();
  private HashMap<String,Integer> streamIds = new HashMap<>();

  static {
    SamplePlayer.builder = new SoundPool.Builder();
    SamplePlayer.builder.setMaxStreams(4);
  }

  SamplePlayer() {
    this.pool = SamplePlayer.builder.build();
    this.pool.setOnLoadCompleteListener(this);
  }

  void destroy() {
    pool.release();
  }

  void load(String name, String path, Promise promise) {
    Integer soundId = soundIds.get(name);
    if (soundId != null) pool.unload(soundId);

    soundId = this.pool.load(path, 1);
    loadPromises.put(soundId, promise);
    soundIds.put(name, soundId);
  }

  public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
    if (soundPool != this.pool) return;
    Promise promise = loadPromises.remove(sampleId);
    if (promise != null) {
      if (status == 0) promise.resolve(null);
      else Errors.OPERATION_FAILED.reject(promise, "Status: " + status);
    }
  }

  void play(String sampleName, boolean loop, Promise promise) {
    Integer soundId = soundIds.get(sampleName);
    if (soundId == null) {
      Errors.UNKNOWN_SAMPLE_NAME.reject(promise, sampleName);
      return;
    }

    Integer streamId = streamIds.remove(sampleName);
    if (streamId != null) pool.stop(streamId);

    streamId = pool.play(soundId, 1, 1, 1, loop ? 1 : 0, 1);
    if (streamId == 0) Errors.OPERATION_FAILED.reject(promise);
    else streamIds.put(sampleName, streamId);

    promise.resolve(null);
  }

  void stop(String sampleName, Promise promise) {
    Integer streamId = streamIds.remove(sampleName);
    if (streamId != null) pool.stop(streamId);
    promise.resolve(null);
  }

  void unload(String name, Promise promise) {
    Integer soundId = soundIds.remove(name);
    if (soundId == null) {
      Errors.UNKNOWN_SAMPLE_NAME.reject(promise, name);
      return;
    }

    boolean res = pool.unload(soundId);
    if (res) promise.resolve(null);
    else Errors.OPERATION_FAILED.reject(promise, "Sample is not loaded");
  }
}
