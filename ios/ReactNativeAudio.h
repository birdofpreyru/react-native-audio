#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

#ifdef RCT_NEW_ARCH_ENABLED
  #import <NativeAudioSpec/NativeAudioSpec.h>
  @interface ReactNativeAudio () <NativeAudioSpec>
  @end
#else
  @interface ReactNativeAudio: RCTEventEmitter <RCTBridgeModule>
  @end
#endif
