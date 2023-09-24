#import "RNASamplePlayer.h"

#import <AVFoundation/AVFoundation.h>

@interface RNAOutputStream: NSObject

- (id) init:(AVAudioEngine*)engine
     sample:(AVAudioPCMBuffer*)sample
       loop:(BOOL)loop;

- (void) play:(RCTPromiseResolveBlock)resolve
       reject:(RCTPromiseRejectBlock)reject;

- (void) stop;

@end // RNAOutputStream

@implementation RNAOutputStream {
  AVAudioPlayerNode *player;
  AVAudioMixerNode *mixer;
  NSTimer *timer;
}

- (id) init:(AVAudioEngine*)engine
     sample:(AVAudioPCMBuffer *)sample
       loop:(BOOL)loop
{
  player = [[AVAudioPlayerNode alloc] init];
  mixer = [[AVAudioMixerNode alloc] init];
  [engine attachNode:player];
  [engine attachNode:mixer];
  [engine connect:player to:mixer format:sample.format];
  [engine connect:mixer to:engine.mainMixerNode format:nil];
  [player scheduleBuffer:sample
                  atTime:nil
                 options:loop ? AVAudioPlayerNodeBufferLoops : 0
  completionCallbackType:AVAudioPlayerNodeCompletionDataPlayedBack
       completionHandler:^(AVAudioPlayerNodeCompletionCallbackType) {
    // NOTE: Node detachment should be done async, otherwise it just hangs,
    // presumably because the engine waits till completion handler exists
    // before it assumes the node can be detached.
    dispatch_async(dispatch_get_main_queue(), ^(void) {
      [engine detachNode:self->mixer];
      [engine detachNode:self->player];
    });
  }];
  return self;
}

- (void) fadeOut
{
  if (mixer.outputVolume <= 0.1) {
    [player stop];
    return;
  }

  mixer.outputVolume -= 0.1;
  dispatch_time_t when = dispatch_time(DISPATCH_TIME_NOW, NSEC_PER_SEC * 0.01);
  dispatch_after(when, dispatch_get_main_queue(), ^(void) {
    [self fadeOut];
  });
}

- (void) play:(RCTPromiseResolveBlock)resolve
       reject:(RCTPromiseRejectBlock)reject
{
  NSError *error;
  AVAudioEngine *engine = player.engine;
  if (engine.running != YES && [engine startAndReturnError:&error] != YES) {
    [[RNAudioException INTERNAL_ERROR:error.localizedDescription] reject:reject];
    return;
  }
  [player play];
  resolve(nil);
}

- (void) stop
{
  if (mixer.outputVolume == 1.0) [self fadeOut];
}

@end // RNAOutputStream

@implementation RNASamplePlayer {
  OnError onError;
  AVAudioEngine *engine;
  NSMutableDictionary<NSString*,AVAudioPCMBuffer*> *samples;
  RNAOutputStream *activeStream;
}

/**
 * Inits RNASamplePlayer instance.
 */
- (id) init:(OnError)onError
{
  self->onError = onError;
  engine = [[AVAudioEngine alloc] init];
  samples = [NSMutableDictionary new];
  return self;
}

- (void) load:(NSString*)name
     fromPath:(NSString*)path
      resolve:(RCTPromiseResolveBlock)resolve
       reject:(RCTPromiseRejectBlock)reject
{
  NSURL *url = [NSURL fileURLWithPath:path];
  if ([url checkResourceIsReachableAndReturnError:nil] == NO) {
    [[RNAudioException OPERATION_FAILED:@"Invalid sample path"] reject:reject];
    return;
  }

  NSError *error;
  AVAudioFile *file = [[AVAudioFile alloc] initForReading:url error:&error];
  if (error != nil) {
    [[RNAudioException OPERATION_FAILED:error.localizedDescription]
     reject:reject];
    return;
  }

  AVAudioPCMBuffer *sample = [[AVAudioPCMBuffer alloc]
                              initWithPCMFormat:file.processingFormat
                              frameCapacity:file.length];

  if (![file readIntoBuffer:sample error:&error]) {
    [[RNAudioException OPERATION_FAILED:error.localizedDescription]
     reject:reject];
    return;
  }

  samples[name] = sample;
  resolve(nil);
}

- (void) play:(NSString *)sampleName
         loop:(BOOL)loop
      resolve:(RCTPromiseResolveBlock)resolve
       reject:(RCTPromiseRejectBlock)reject
{
  [self stop:@"" resolve:nil reject:nil];

  AVAudioPCMBuffer *sample = samples[sampleName];
  if (sample == nil) {
    [RNAudioException UNKNOWN_SAMPLE_NAME:reject];
    return;
  }

  activeStream = [[RNAOutputStream alloc]
                  init:engine
                  sample:sample
                  loop:loop];
  [activeStream play:resolve reject:reject];
}

- (void) stop:(NSString*)sampleName
      resolve:(RCTPromiseResolveBlock)resolve
       reject:(RCTPromiseRejectBlock)reject
{
  if (activeStream != nil) {
    [activeStream stop];
    activeStream = nil;
  }
  if (resolve != nil) resolve(nil);
}

- (void) unload:(NSString *)sampleName
        resolve:(RCTPromiseResolveBlock)resolve
         reject:(RCTPromiseRejectBlock)reject
{
  if (samples[sampleName] == nil) {
    [RNAudioException UNKNOWN_SAMPLE_NAME:reject];
    return;
  }

  [samples removeObjectForKey:sampleName];
  resolve(nil);
}

/**
 * Creates a new RNASamplePlayer instance.
 */
+ (RNASamplePlayer*) new:(OnError)onError
{
  return [[RNASamplePlayer alloc] init:onError];
}

@end // RNASamplePlayer
