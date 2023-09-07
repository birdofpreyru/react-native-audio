#import "RNASamplePlayer.h"

#import <AVFoundation/AVFoundation.h>

#import "RNAudioException.h"

@interface RNAPlayerController: NSObject<AVAudioPlayerDelegate>
@property AVAudioPlayer *player;
- (id) init:(AVAudioPlayer*)player;
- (void) audioPlayerDidFinishPlaying:(AVAudioPlayer *)player
                        successfully:(BOOL)flag;

- (void) play:(BOOL)loop
      resolve:(RCTPromiseResolveBlock)resolve
       reject:(RCTPromiseRejectBlock)reject;

- (void) stop:(RCTPromiseResolveBlock)resolve
       reject:(RCTPromiseRejectBlock)reject;

+ (RNAPlayerController*) for:(AVAudioPlayer*)player;
@end // RNAPlayerController

@implementation RNAPlayerController {
  dispatch_time_t nextStopTime;
  uint64_t playbackId;
}

/**
 * Inits controller instance, and connects it with the given AVAudioPlayer.
 */
- (id) init:(AVAudioPlayer*)player {
  self->_player = player;
  self->nextStopTime = 0;
  self->playbackId = 0;
  player.delegate = self;
  return self;
}

/**
 * Each time the underlying AVAudioPlayer stops to play, we immediately prepare it to play again,
 * to thus guarantee a low latency of the future playback.
 */
- (void) audioPlayerDidFinishPlaying:(AVAudioPlayer *)player
                        successfully:(BOOL)flag
{
  [_player prepareToPlay];
}

/**
 * Plays the sample from the beginning.
 */
- (void) play:(BOOL)loop
      resolve:(RCTPromiseResolveBlock)resolve
       reject:(RCTPromiseRejectBlock)reject
{
  // If this player is currently playing and has not been ordered to stop yet,
  // we order it to - otherwise an audible "click" is likely when we rewind it
  // to the sample beginning.
  if (_player.playing == YES && self->nextStopTime == 0) {
    [self stop:nil reject:nil];
  }

  void (^start)() = ^{
    ++self->playbackId;
    self->_player.currentTime = 0;
    self->_player.numberOfLoops = loop ? -1 : 0;
    [self->_player setVolume:1];
    if ([self->_player play] == NO) {
      [[RNAudioException OPERATION_FAILED:nil] reject:reject];
      return;
    }
    resolve(nil);
  };

  dispatch_time_t now = dispatch_time(DISPATCH_TIME_NOW, 0);
  if (self->nextStopTime > now) {
    // Note: If a previous playback is still being stopped, we wait till it is
    // stopped prior to starting the new one - otherwise there most probably
    // will be an audible "click" due to the sample position jumping back
    // to the beginning.
    dispatch_after(self->nextStopTime, dispatch_get_main_queue(), start);
  } else start();
}

/**
 * Gracefully stops the player. The point is: if we just call [_player stop] right away, it will produce
 * an audible click in most cases, to prevent which we need to fade the player volume down to 0 over
 * a brief time, and only then stop the player. Further we need to take special consideration to ensure
 * that this volume fading does not block the thread, and does not interrupt a new playback of this
 * player, if it has been started in the meantime.
 */
- (void) stop:(RCTPromiseResolveBlock)resolve
       reject:(RCTPromiseRejectBlock)reject
{
  uint64_t id = playbackId;
  if (_player.playing == NO) {
    if (resolve != nil) resolve(nil);
    return;
  }

  [_player setVolume:0 fadeDuration:0.1];
  self->nextStopTime = dispatch_time(DISPATCH_TIME_NOW, NSEC_PER_SEC * 0.1);
  dispatch_after(self->nextStopTime, dispatch_get_main_queue(), ^{
    if (id == self->playbackId) {
      self->nextStopTime = 0;
      [self.player stop];
    }
    if (resolve != nil) resolve(nil);
  });
}

+ (RNAPlayerController*) for:(AVAudioPlayer*)player
{
  return [[RNAPlayerController alloc] init:player];
}

@end // RNAPlayerDelegate

@implementation RNASamplePlayer {
  NSMutableDictionary<NSString*,RNAPlayerController*> *pool;
}

/**
 * Inits RNASamplePlayer instance.
 */
- (id) init
{
  pool = [NSMutableDictionary new];
  return self;
}

- (void) load:(NSString*)name
     fromPath:(NSString*)path
      resolve:(RCTPromiseResolveBlock)resolve
       reject:(RCTPromiseRejectBlock)reject
{
  RNAPlayerController *controller = pool[name];
  if (controller != nil) [controller stop:nil reject:nil];

  NSURL *url = [NSURL fileURLWithPath:path];
  if ([url checkResourceIsReachableAndReturnError:nil] == NO) {
    [[RNAudioException OPERATION_FAILED:@"Invalid sample path"] reject:reject];
    return;
  }

  NSError *error;
  AVAudioPlayer *player = [[AVAudioPlayer alloc]
                           initWithContentsOfURL:url
                           error:&error];
  if (error != nil) {
    [[RNAudioException OPERATION_FAILED:error.localizedDescription]
     reject:reject];
    return;
  }

  if ([player prepareToPlay] == NO) {
    [[RNAudioException OPERATION_FAILED:@"Playback preparation failure"]
     reject:reject];
    return;
  }

  pool[name] = [RNAPlayerController for:player];
  resolve(nil);
}

- (void) play:(NSString *)sampleName
         loop:(BOOL)loop
      resolve:(RCTPromiseResolveBlock)resolve
       reject:(RCTPromiseRejectBlock)reject
{
  RNAPlayerController *controller = pool[sampleName];
  if (controller == nil) {
    [RNAudioException UNKNOWN_SAMPLE_NAME:reject];
    return;
  }

  [controller play:loop resolve:resolve reject:reject];
}

- (void) stop:(NSString*)sampleName
      resolve:(RCTPromiseResolveBlock)resolve
       reject:(RCTPromiseRejectBlock)reject
{
  RNAPlayerController *controller = pool[sampleName];
  if (controller == nil) {
    [RNAudioException UNKNOWN_SAMPLE_NAME:reject];
    return;
  }

  [controller stop:resolve reject:reject];
}

- (void) unload:(NSString *)sampleName
        resolve:(RCTPromiseResolveBlock)resolve
         reject:(RCTPromiseRejectBlock)reject
{
  RNAPlayerController *controller = pool[sampleName];
  if (controller == nil) {
    [RNAudioException UNKNOWN_SAMPLE_NAME:reject];
    return;
  }

  [pool removeObjectForKey:sampleName];
  [controller stop:resolve reject:reject];
}

/**
 * Creates a new RNASamplePlayer instance.
 */
+ (RNASamplePlayer*) new
{
  return [[RNASamplePlayer alloc] init];
}

@end // RNASamplePlayer
