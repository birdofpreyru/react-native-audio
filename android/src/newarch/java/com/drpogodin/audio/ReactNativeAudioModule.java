package com.drpogodin.audio;

import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.HashMap;
import java.util.Map;

public class ReactNativeAudioModule extends NativeAudioSpec {
  private ReactNativeAudioModuleImpl impl;

  public ReactNativeAudioModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.impl = new ReactNativeAudioModuleImpl();
  }

  @Override
  @NonNull
  public String getName() { return ReactNativeAudioModuleImpl.NAME; }

  @Override
  public void configAudioSystem(Promise promise) {
    this.impl.configAudioSystem(promise);
  }

  @Override
  public Map<String, Object> getTypedExportedConstants() {
    return ReactNativeAudioModuleImpl.getConstants();
  }

  public void listen(
    double audioSource,
    double sampleRate,
    double channelConfig,
    double audioFormat,
    double samplingSize,
    Promise promise
  ) {
    DeviceEventManagerModule.RCTDeviceEventEmitter eventEmitter =
      getReactApplicationContext()
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);
    this.impl.listen(audioSource, sampleRate, channelConfig, audioFormat,
      samplingSize, eventEmitter, promise);
  }

  public void muteInputStream(double streamId, boolean muted) {
    this.impl.muteInputStream(streamId, muted);
  }

  public void unlisten(double streamId) {
    this.impl.unlisten(streamId);
  }

  @Override
  public void addListener(String eventName) {
    // Keep: Required for RN built in Event Emitter Calls.
  }

  @Override
  public void removeListeners(double count) {
    // Keep: Required for RN built in Event Emitter Calls.
  }
}
