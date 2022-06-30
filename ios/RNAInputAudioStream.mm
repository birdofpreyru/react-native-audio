//
//  RNAInputAudioStream.m
//  ReactNativeAudio
//
//  Created by Sergey Pogodin on 4/6/21.
//  Copyright © 2021 Facebook. All rights reserved.
//

#import "RNAInputAudioStream.h"

#import <AVFoundation/AVFoundation.h>
#import <React/RCTLog.h>

const int NUM_BUFFERS = 3;

@implementation RNAInputAudioStream {
  BOOL destroyed;
  int chunkId;
  AudioQueueBufferRef buffers[NUM_BUFFERS];
  AudioQueueRef queue;
  OnChunk onChunk;
  OnError onError;
}

void HandleInputBuffer(void *inUserData,
                       AudioQueueRef inAQ,
                       AudioQueueBufferRef inBuffer,
                       const AudioTimeStamp *inStartTime,
                       UInt32 inNumPackets,
                       const AudioStreamPacketDescription *inPacketDesc)
{
  RNAInputAudioStream *stream = (__bridge RNAInputAudioStream*)inUserData;
  if (!stream.muted) {
    stream->onChunk(stream->chunkId,
                    inBuffer->mAudioData,
                    inBuffer->mAudioDataByteSize);
  }
  ++stream->chunkId;
  [stream handleStatus:AudioQueueEnqueueBuffer(inAQ, inBuffer, 0, NULL)];
}

/**
 *  Handles given error. If the error is not nil, it triggers onError callback with a message string composed
 *  for the error object; otherwise it does nothing.
 *  @param error  Error object.
 */
- (void)handleError:(NSError*)error
{
  if (error) {
    onError([NSString stringWithFormat:@"%@ [%d]: %@",
             error.domain, error.code, error.localizedDescription]);
  }
}

/**
 * Handles given status. If it is correspond to an error, it calls onError callback with a message string
 * composed for that status; otherwise it does nothing.
 * @param status Operaiton result status.
 */
- (void)handleStatus:(OSStatus)status
{
  if (status) {
    onError([NSString stringWithFormat:@"Error: %d", (int)status]);
  }
}

- (id)initWithAudioSource:(enum AUDIO_SOURCES)audioSource
           withSampleRate:(int)sampleRate
        withChannelConfig:(enum CHANNEL_CONFIGS)channelConfig
          withAudioFormat:(enum AUDIO_FORMATS)audioFormat
         withSamplingSize:(int)samplingSize
                  onChunk:(OnChunk)onChunk
                  onError:(OnError)onError
{
  self = [super init];
  self->onChunk = onChunk;
  self->onError = onError;
  
  NSError *error;
  
  // Configuration and activation of audio session.
  /*
  AVAudioSession *audioSession = [AVAudioSession sharedInstance];
  [audioSession setCategory:AVAudioSessionCategoryRecord error:&error];
  [self handleError:error];
  [audioSession setMode:AVAudioSessionModeMeasurement error:&error];
  [self handleError:error];
  [audioSession setActive:YES error:&error];
  [self handleError:error];
   */
  
  // Creates stream configuration.
  AudioStreamBasicDescription config = {0};
  config.mFormatID = kAudioFormatLinearPCM;
  config.mFormatFlags = 0;
  switch (audioFormat) {
    case PCM_8BIT: config.mBitsPerChannel = 8; break;
    case PCM_16BIT:
      config.mBitsPerChannel = 16;
      config.mFormatFlags |= kAudioFormatFlagIsSignedInteger;
      break;
    case PCM_FLOAT:
      config.mBitsPerChannel = 32;
      config.mFormatFlags |= kAudioFormatFlagIsFloat;
      break;
    default: onError(@"Invalid audio format");
  }
  switch (channelConfig) {
    case MONO: config.mChannelsPerFrame = 1; break;
    case STEREO: config.mChannelsPerFrame = 2; break;
    default: onError(@"Invalid channel config");
  }
  config.mBytesPerFrame = config.mBitsPerChannel * config.mChannelsPerFrame / 8;
  config.mBytesPerPacket = config.mBytesPerFrame;
  config.mFramesPerPacket = 1;
  config.mReserved = 0;
  config.mSampleRate = sampleRate;
  
  int bufferSize = samplingSize * config.mBytesPerFrame;
  
  [self handleStatus:AudioQueueNewInput(&config, HandleInputBuffer,
                                        (__bridge void*)self,
                                        NULL, NULL, 0, &queue)];
  for (int i = 0; i < NUM_BUFFERS; ++i) {
    [self handleStatus:AudioQueueAllocateBuffer(queue, bufferSize, &buffers[i])];
    [self handleStatus:AudioQueueEnqueueBuffer(queue, buffers[i], 0, NULL)];
  }
  [self handleStatus:AudioQueueStart(queue, NULL)];
  
  return self;
}

/**
 * Stops and destroys the stream.
 */
- (void)stop {
  if (!destroyed) {
    destroyed = true;
    [self handleStatus:AudioQueueDispose(queue, false)];
  }
}

- (void)dealloc {
  [self stop];
}

+ (RNAInputAudioStream*) streamAudioSource:(enum AUDIO_SOURCES)audioSource
                                sampleRate:(int)sampleRate
                             channelConfig:(enum CHANNEL_CONFIGS)channelConfig
                               audioFormat:(enum AUDIO_FORMATS)audioFormat
                              samplingSize:(int)samplingSize
                                   onChunk:(OnChunk)onChunk
                                   onError:(OnError)onError
{
  return [[RNAInputAudioStream alloc]
          initWithAudioSource:audioSource
          withSampleRate:sampleRate
          withChannelConfig:channelConfig
          withAudioFormat:audioFormat
          withSamplingSize:samplingSize
          onChunk:onChunk
          onError:onError];
}

@end
