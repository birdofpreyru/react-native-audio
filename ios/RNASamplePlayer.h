#ifndef RNASamplePlayer_h
#define RNASamplePlayer_h

#import <React/RCTBridgeModule.h>

@interface RNASamplePlayer: NSObject
- (id) init;

- (void) load:(NSString*)name
     fromPath:(NSString*)path
      resolve:(RCTPromiseResolveBlock)resolve
       reject:(RCTPromiseRejectBlock)reject;

- (void) play:(NSString*)sampleName
         loop:(BOOL)loop
      resolve:(RCTPromiseResolveBlock)resolve
       reject:(RCTPromiseRejectBlock)reject;

- (void) stop:(NSString*)sampleName
      resolve:(RCTPromiseResolveBlock)resolve
       reject:(RCTPromiseRejectBlock)reject;

- (void) unload:(NSString*)sampleName
        resolve:(RCTPromiseResolveBlock)resolve
         reject:(RCTPromiseRejectBlock)reject;

+ (RNASamplePlayer*) new;
@end // RNASamplePlayer

#endif /* RNASamplePlayer_h */
