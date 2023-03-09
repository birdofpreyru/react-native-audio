#import <React/RCTEventEmitter.h>

#ifdef RCT_NEW_ARCH_ENABLED
#import "RNReactNativeAudioSpec.h"

@interface ReactNativeAudio : RCTEventEmitter <NativeReactNativeAudioSpec>
#else
#import <React/RCTBridgeModule.h>

@interface ReactNativeAudio : RCTEventEmitter <RCTBridgeModule>
#endif

@end
