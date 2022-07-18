package com.drpogodin.audio;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.Map;

@ReactModule(name = ReactNativeAudioModuleImpl.NAME)
public class ReactNativeAudioModule extends ReactContextBaseJavaModule {
  private ReactNativeAudioModuleImpl impl;

  public ReactNativeAudioModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.impl = new ReactNativeAudioModuleImpl();
  }

  @Override
  @NonNull
  public String getName() { return ReactNativeAudioModuleImpl.NAME; }

  @ReactMethod
  public void configAudioSystem(Promise promise) {
    this.impl.configAudioSystem(promise);
  }

  @Override
  public Map<String, Object> getConstants() {
    return ReactNativeAudioModuleImpl.getConstants();
  }

  @ReactMethod
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

  @ReactMethod
  public void muteInputStream(double streamId, boolean muted) {
    this.impl.muteInputStream(streamId, muted);
  }

  @ReactMethod
  public void unlisten(double streamId) {
    this.impl.unlisten(streamId);
  }

  @ReactMethod
  public void addListener(String eventName) {
    // Keep: Required for RN built in Event Emitter Calls.
  }

  @ReactMethod
  public void removeListeners(Integer count) {
    // Keep: Required for RN built in Event Emitter Calls.
  }
}
