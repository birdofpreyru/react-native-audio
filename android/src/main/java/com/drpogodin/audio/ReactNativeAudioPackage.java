package com.drpogodin.audio;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.module.model.ReactModuleInfo;
import com.facebook.react.module.model.ReactModuleInfoProvider;
import com.facebook.react.TurboReactPackage;

import java.util.HashMap;
import java.util.Map;

public class ReactNativeAudioPackage extends TurboReactPackage {
  @Nullable
  @Override
  public NativeModule getModule(String name, ReactApplicationContext context) {
    if (name.equals(ReactNativeAudioModuleImpl.NAME)) {
      return new ReactNativeAudioModule(context);
    } else {
      return null;
    }
  }

  @Override
  public ReactModuleInfoProvider getReactModuleInfoProvider() {
    return () -> {
      final Map<String, ReactModuleInfo> moduleInfos = new HashMap<>();
      boolean isTurboModule = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED;
      moduleInfos.put(
        ReactNativeAudioModuleImpl.NAME,
        new ReactModuleInfo(
          ReactNativeAudioModuleImpl.NAME,
          ReactNativeAudioModuleImpl.NAME,
          false, // canOverrideExistingModule
          false, // needsEagerInit
          true, // hasConstants
          false, // isCxxModule
          isTurboModule // isTurboModule
        )
      );
      return moduleInfos;
    };
  }
}
