package com.drpogodin.reactnativeaudio

import com.facebook.react.TurboReactPackage
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.NativeModule
import com.facebook.react.module.model.ReactModuleInfoProvider
import com.facebook.react.module.model.ReactModuleInfo
import java.util.HashMap

class ReactNativeAudioPackage : TurboReactPackage() {
  override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? {
    return if (name == ReactNativeAudioModule.NAME) {
      ReactNativeAudioModule(reactContext)
    } else {
      null
    }
  }

  override fun getReactModuleInfoProvider(): ReactModuleInfoProvider {
    return ReactModuleInfoProvider {
      val moduleInfos: MutableMap<String, ReactModuleInfo> = HashMap()
      val isTurboModule: Boolean = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED
      moduleInfos[ReactNativeAudioModule.NAME] = ReactModuleInfo(
        ReactNativeAudioModule.NAME,
        ReactNativeAudioModule.NAME,
        canOverrideExistingModule = false,  // canOverrideExistingModule
        needsEagerInit = false,  // needsEagerInit
        hasConstants = true,  // hasConstants
        isCxxModule = false,  // isCxxModule
        isTurboModule = isTurboModule // isTurboModule
      )
      moduleInfos
    }
  }
}
