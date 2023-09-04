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
  uint64_t playbackId;
}

/**
 * Inits controller instance, and connects it with the given AVAudioPlayer.
 */
- (id) init:(AVAudioPlayer*)player {
  self.player = player;
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
  ++self->playbackId;
  _player.currentTime = 0;
  _player.numberOfLoops = loop ? -1 : 0;
  if ([_player play] == NO) {
    [[RNAudioException OPERATION_FAILED:nil] reject:reject];
    return;
  }
  if (_player.volume != 1) [_player setVolume:1 fadeDuration:0.1];
  resolve(nil);
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
  dispatch_time_t when = dispatch_time(DISPATCH_TIME_NOW, NSEC_PER_SEC * 0.1);
  dispatch_after(when, dispatch_get_main_queue(), ^{
    if (id == self->playbackId) [self.player stop];
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
