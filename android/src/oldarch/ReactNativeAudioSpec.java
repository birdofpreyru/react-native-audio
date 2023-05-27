package com.drpogodin.reactnativeaudio;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.Promise;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.Map;

abstract class ReactNativeAudioSpec extends ReactContextBaseJavaModule {
  ReactNativeAudioSpec(ReactApplicationContext context) {
    super(context);
  }

  public abstract String getName();
  public abstract void configAudioSystem(Promise promise);
  public abstract Map<String,Object> getTypedExportedConstants();

  public Map<String,Object> getConstants() {
    return this.getTypedExportedConstants();
  }

  public abstract void getInputAvailable(Promise promise);

  public abstract void listen(
    double audioSource,
    double sampleRate,
    double channelConfig,
    double audioFormat,
    double samplingSize,
    Promise promise
  );

  public abstract void muteInputStream(double streamId, boolean muted);
  public abstract void unlisten(double streamId);
  public abstract void addListener(String eventName);
  public abstract void removeListeners(double count);
}
