#import <React/RCTLog.h>

#import "ReactNativeAudio.h"
#import "RNAInputAudioStream.h"

NSString *EVENT_AUDIO_CHUNK = @"RNA_AudioChunk";
NSString *EVENT_INPUT_AUDIO_STREAM_ERROR = @"RNA_InputAudioStreamError";

@implementation ReactNativeAudio {
  int lastInputStreamId;
  NSMutableDictionary<NSNumber*,RNAInputAudioStream*> *inputStreams;
}

RCT_EXPORT_MODULE(ReactNativeAudio)

- (id) init
{
  self = [super init];
  inputStreams = [NSMutableDictionary new];
  return self;
}

- (NSDictionary *) constantsToExport
{
  return @{
    @"AUDIO_FORMAT_PCM_8BIT": [NSNumber numberWithInt:PCM_8BIT],
    @"AUDIO_FORMAT_PCM_16BIT": [NSNumber numberWithInt:PCM_16BIT],
    @"AUDIO_FORMAT_PCM_FLOAT": [NSNumber numberWithInt:PCM_FLOAT],
    @"AUDIO_SOURCE_UNPROCESSED": [NSNumber numberWithInt:UNPROCESSED],
    @"CHANNEL_IN_MONO": [NSNumber numberWithInt:MONO],
    @"CHANNEL_IN_STEREO": [NSNumber numberWithInt:STEREO]
  };
}

+ (BOOL) requiresMainQueueSetup
{
  return NO;
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

// NOTE: Can't use enum as the argument type here, as RN won't understand that.
RCT_EXPORT_METHOD(listen:(double)audioSource
                  withSampleRate:(double)sampleRate
                  withChannelConfig:(double)channelConfig
                  withAudioFormat:(double)audioFormat
                  withSamplingSize:(double)samplingSize
                  resolver:(RCTPromiseResolveBlock) resolve
                  rejecter:(RCTPromiseRejectBlock) reject)
{
  NSNumber *streamId = [NSNumber numberWithInt:++lastInputStreamId];

  OnChunk onChunk = ^void(int chunkId, unsigned char *chunk, int size) {
    RCTLogInfo(@"AUDIO DATA CHUNK RECEIVED");
    NSData* data = [NSData dataWithBytesNoCopy:chunk
                                        length:size
                                  freeWhenDone:NO];
    [self sendEventWithName:EVENT_AUDIO_CHUNK
                       body:@{@"streamId":streamId,
                              @"chunkId":@(chunkId),
                              @"data":[data base64EncodedStringWithOptions:0]}];
  };
  
  OnError onError = ^void(NSString* error) {
    [self sendEventWithName:EVENT_INPUT_AUDIO_STREAM_ERROR
                       body:@{@"streamId":streamId, @"error":error}];
  };
  
  RNAInputAudioStream *stream =
  [RNAInputAudioStream streamAudioSource:(AUDIO_SOURCES)audioSource
                              sampleRate:sampleRate
                           channelConfig:(CHANNEL_CONFIGS)channelConfig
                             audioFormat:(AUDIO_FORMATS)audioFormat
                            samplingSize:samplingSize
                                 onChunk:onChunk
                                 onError:onError];
  
  inputStreams[streamId] = stream;
  
  resolve(streamId);
}

RCT_EXPORT_METHOD(unlisten:(double)streamId)
{
  NSNumber *id = [NSNumber numberWithDouble:streamId];
  [inputStreams[id] stop];
  [inputStreams removeObjectForKey:id];
}

RCT_EXPORT_METHOD(muteInputStream:(double)streamId mute:(BOOL)mute)
{
  inputStreams[[NSNumber numberWithDouble:streamId]].muted = mute;
}

#ifdef RCT_NEW_ARCH_ENABLED
- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
  {
    return std::make_shared<facebook::react::NativeAudioSpecJSI>(params); 
  }
#endif

@end
