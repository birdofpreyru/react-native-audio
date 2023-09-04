#import <React/RCTLog.h>

#import "ReactNativeAudio.h"
#import "RNAudioException.h"
#import "RNAInputAudioStream.h"
#import "RNASamplePlayer.h"

NSString *EVENT_AUDIO_CHUNK = @"RNA_AudioChunk";
NSString *EVENT_INPUT_AUDIO_STREAM_ERROR = @"RNA_InputAudioStreamError";

@implementation ReactNativeAudio {
  NSMutableDictionary<NSNumber*,RNAInputAudioStream*> *inputStreams;
  NSMutableDictionary<NSNumber*,RNASamplePlayer*> *samplePlayers;
}

RCT_EXPORT_MODULE()

- (instancetype) init {
  inputStreams = [NSMutableDictionary new];
  samplePlayers = [NSMutableDictionary new];
  return [super init];
}

- (NSDictionary *) constantsToExport {
  return @{
    @"AUDIO_FORMAT_PCM_8BIT": [NSNumber numberWithInt:PCM_8BIT],
    @"AUDIO_FORMAT_PCM_16BIT": [NSNumber numberWithInt:PCM_16BIT],
    @"AUDIO_FORMAT_PCM_FLOAT": [NSNumber numberWithInt:PCM_FLOAT],
    @"AUDIO_SOURCE_DEFAULT": [NSNumber numberWithInt:DEFAULT],
    @"AUDIO_SOURCE_MIC": [NSNumber numberWithInt:MIC],
    @"AUDIO_SOURCE_UNPROCESSED": [NSNumber numberWithInt:UNPROCESSED],
    @"CHANNEL_IN_MONO": [NSNumber numberWithInt:MONO],
    @"CHANNEL_IN_STEREO": [NSNumber numberWithInt:STEREO]
  };
}

- (NSDictionary*) getConstants {
  return [self constantsToExport];
}

RCT_REMAP_METHOD(getInputAvailable,
  getInputAvailable:(RCTPromiseResolveBlock)resolve
  reject:(RCTPromiseRejectBlock)reject
) {
  resolve([NSNumber numberWithBool: AVAudioSession.sharedInstance.inputAvailable]);
}

/**
 *  Creates a dedicated queue for module operations.
 */
- (dispatch_queue_t)methodQueue
{
  return dispatch_queue_create("studio.pogodin.react_native_audio", DISPATCH_QUEUE_SERIAL);
}

- (NSArray<NSString*>*)supportedEvents
{
  return @[EVENT_AUDIO_CHUNK, EVENT_INPUT_AUDIO_STREAM_ERROR];
}

// TODO: Should we somehow plug-in this audio system configuration into
// AudioStream initialization, and base it on the "audioSource" parameter,
// which is now ignored on iOS?
RCT_REMAP_METHOD(configAudioSystem,
  configAudioSystem:(RCTPromiseResolveBlock)resolve
  reject:(RCTPromiseRejectBlock)reject
) {
  RCTLogInfo(@"Audio session configuration...");

  AVAudioSession *audioSession = AVAudioSession.sharedInstance;
  NSArray<AVAudioSessionCategory> *cats = audioSession.availableCategories;

  AVAudioSessionCategory category;
  if ([cats containsObject:AVAudioSessionCategoryPlayAndRecord]) {
    category = AVAudioSessionCategoryPlayAndRecord;
  } else if ([cats containsObject:AVAudioSessionCategoryPlayback]) {
    category = AVAudioSessionCategoryPlayback;
  } else {
    reject(@"incompatible_audio_session",
           @"neither play-and-record, nor playback category is supported",
           nil);
    return;
  }

  NSError *error = nil;
  BOOL res = [audioSession setCategory:category
                           withOptions:(AVAudioSessionCategoryOptionAllowBluetooth |
                                        AVAudioSessionCategoryOptionAllowBluetoothA2DP |
                                        AVAudioSessionCategoryOptionDefaultToSpeaker)
                                 error:&error];

  // TODO: Currently here, and in the next rejection, although we include
  // error object, the details of error we get to sentry are minimal, should
  // be investigated, and checked how do we pass all available details to
  // Sentry?

  // TODO: Probably, need to provide additional details here. At least
  // add some error ID, to determine where exactly do errors are thrown.
  if (res != YES || error != nil) {
    reject(error.domain, error.localizedDescription, error);
    return;
  }

  res = [audioSession setActive:YES error:&error];
  if (res != YES || error != nil) {
    reject(error.domain, error.localizedDescription, error);
    return;
  }

  resolve(nil);
}

// NOTE: Can't use enum as the argument type here, as RN won't understand that.
RCT_REMAP_METHOD(listen,
  listen:(double)streamId
  audioSource:(double)audioSource
  sampleRate:(double)sampleRate
  channelConfig:(double)channelConfig
  audioFormat:(double)audioFormat
  samplingSize:(double)samplingSize
  resolve:(RCTPromiseResolveBlock) resolve
  reject:(RCTPromiseRejectBlock) reject
) {
  NSNumber *sid = [NSNumber numberWithDouble:streamId];

  OnChunk onChunk = ^void(int chunkId, unsigned char *chunk, int size) {
    RCTLogInfo(@"[Stream %@] Audio data chunk %d received", sid, chunkId);
    NSData* data = [NSData dataWithBytesNoCopy:chunk
                                        length:size
                                  freeWhenDone:NO];
    [self sendEventWithName:EVENT_AUDIO_CHUNK
                       body:@{@"streamId":sid,
                              @"chunkId":@(chunkId),
                              @"data":[data base64EncodedStringWithOptions:0]}];
  };
  
  OnError onError = ^void(NSString* error) {
    [self sendEventWithName:EVENT_INPUT_AUDIO_STREAM_ERROR
                       body:@{@"streamId":sid, @"error":error}];
  };
  
  RNAInputAudioStream *stream =
  [RNAInputAudioStream streamAudioSource:(AUDIO_SOURCES)audioSource
                              sampleRate:sampleRate
                           channelConfig:(CHANNEL_CONFIGS)channelConfig
                             audioFormat:(AUDIO_FORMATS)audioFormat
                            samplingSize:samplingSize
                                 onChunk:onChunk
                                 onError:onError];
  
  inputStreams[sid] = stream;
  
  resolve(nil);
}

RCT_REMAP_METHOD(unlisten,
  unlisten:(double)streamId
  resolve:(RCTPromiseResolveBlock) resolve
  reject:(RCTPromiseRejectBlock) reject
) {
  NSNumber *id = [NSNumber numberWithDouble:streamId];
  [inputStreams[id] stop];
  [inputStreams removeObjectForKey:id];
  RCTLogInfo(@"[Stream %@] Is unlistened", id);
  resolve(nil);
}

RCT_REMAP_METHOD(muteInputStream,
  muteInputStream:(double)streamId muted:(BOOL)muted
) {
  inputStreams[[NSNumber numberWithDouble:streamId]].muted = muted;
}

RCT_EXPORT_METHOD(destroySamplePlayer:(double)playerId
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  NSNumber *id = [NSNumber numberWithDouble:playerId];
  if (samplePlayers[id] == nil) {
    [RNAudioException UNKNOWN_PLAYER_ID:reject];
    return;
  }

  [samplePlayers removeObjectForKey:id];
  resolve(nil);
}

RCT_EXPORT_METHOD(initSamplePlayer:(double)playerId
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject)
{
  NSNumber *id = [NSNumber numberWithDouble:playerId];
  if (samplePlayers[id] != nil) {
    [[RNAudioException INTERNAL_ERROR:@"Sample player ID is occupied"]
     reject:reject];
    return;
  }

  samplePlayers[id] = [RNASamplePlayer new];
  resolve(nil);
}

- (void)loadSample:(double)playerId sampleName:(NSString *)sampleName samplePath:(NSString *)samplePath resolve:(RCTPromiseResolveBlock)resolve reject:(RCTPromiseRejectBlock)reject {
  NSNumber *id = [NSNumber numberWithDouble:playerId];
  RNASamplePlayer *player = samplePlayers[id];
  if (player == nil) {
    [RNAudioException UNKNOWN_PLAYER_ID:reject];
    return;
  }
  [player load:sampleName fromPath:samplePath resolve:resolve reject:reject];
}

- (void)playSample:(double)playerId
        sampleName:(NSString *)sampleName
              loop:(BOOL)loop
           resolve:(RCTPromiseResolveBlock)resolve
            reject:(RCTPromiseRejectBlock)reject
{
  NSNumber *id = [NSNumber numberWithDouble:playerId];
  RNASamplePlayer *player = samplePlayers[id];
  if (player == nil) {
    [RNAudioException UNKNOWN_PLAYER_ID:reject];
    return;
  }
  [player play:sampleName loop:loop resolve:resolve reject:reject];
}

- (void) stopSample:(double)playerId
         sampleName:(NSString *)sampleName
            resolve:(RCTPromiseResolveBlock)resolve
             reject:(RCTPromiseRejectBlock)reject
{
  NSNumber *id = [NSNumber numberWithDouble:playerId];
  RNASamplePlayer *player = samplePlayers[id];
  if (player == nil) {
    [RNAudioException UNKNOWN_PLAYER_ID:reject];
    return;
  }
  [player stop:sampleName resolve:resolve reject:reject];
}

- (void) unloadSample:(double)playerId
           sampleName:(NSString *)sampleName
              resolve:(RCTPromiseResolveBlock)resolve
               reject:(RCTPromiseRejectBlock)reject
{
  NSNumber *id = [NSNumber numberWithDouble:playerId];
  RNASamplePlayer *player = samplePlayers[id];
  if (player == nil) {
    [RNAudioException UNKNOWN_PLAYER_ID:reject];
    return;
  }
  [player unload:sampleName resolve:resolve reject:reject];
}

+ (BOOL) requiresMainQueueSetup {
    return NO;
}

// Don't compile this code when we build for the old architecture.
#ifdef RCT_NEW_ARCH_ENABLED
- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativeReactNativeAudioSpecJSI>(params);
}
#endif

@end
