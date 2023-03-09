#import <AVFAudio/AVFAudio.h>

enum AUDIO_FORMATS {
  PCM_8BIT = 1,
  PCM_16BIT = 2,
  PCM_FLOAT = 3
};

// NOTE: These are dummy values, as "audio source" parameter is ignored in iOS
// implementation of the library (at least as of now). Thus, the values below
// are selected to just match Android implementation.
enum AUDIO_SOURCES {
  DEFAULT = 0,
  MIC = 1,
  UNPROCESSED = 9
} ;

enum CHANNEL_CONFIGS {
  MONO = 1,
  STEREO = 2
};

/**
 * Audio data chunk handler.
 * @param chunkId Consequtive chunk number (the count includes chunks dropped because of mute).
 * @param chunk Raw chunk data.
 * @param size Size of chunk data in bytes.
 */
typedef void (^OnChunk)(int chunkId, unsigned char *chunk, int size);

/**
 * Error handler. Assuming its only purpose is to forward brief error descriptions to JS layer, it takes an error
 * description as a string (and not a specialized error handling object).
 * @param error Brief error message.
 */
typedef void (^OnError)(NSString *error);

@interface RNAInputAudioStream : NSObject

@property BOOL muted;

- (void)stop;

+ (RNAInputAudioStream*) streamAudioSource:(enum AUDIO_SOURCES)audioSource
                                sampleRate:(int)sampleRate
                             channelConfig:(enum CHANNEL_CONFIGS)channelConfig
                               audioFormat:(enum AUDIO_FORMATS)audioFormat
                              samplingSize:(int)samplingSize
                                   onChunk:(OnChunk)onChunk
                                   onError:(OnError)onError;
@end
