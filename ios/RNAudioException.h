#ifndef RNAudioException_h
#define RNAudioException_h

#import <React/RCTBridgeModule.h>

/**
 * Error handler. Assuming its only purpose is to forward brief error descriptions to JS layer, it takes an error
 * description as a string (and not a specialized error handling object).
 * @param error Brief error message.
 */
typedef void (^OnError)(NSString *error);

@interface RNAudioException : NSException
- (id) initWithName:(NSString*)name details:(NSString*)details;

- (id) initWithName:(NSString*)name
            details:(NSString*)details
               code:(NSInteger)code
           userInfo:(NSDictionary<NSErrorUserInfoKey,id>*)userInfo;

- (NSError*) error;
- (RNAudioException*) log;
- (RNAudioException*) reject:(RCTPromiseRejectBlock)reject;
+ (RNAudioException*) fromError:(NSError*)error;
+ (RNAudioException*) fromException:(NSException*)exception;
+ (RNAudioException*) name:(NSString*)name;
+ (RNAudioException*) name:(NSString*)name details:(NSString*)details;

+ (RNAudioException*) INTERNAL_ERROR:(NSInteger)code
                             details:(NSString*)details;

+ (RNAudioException*) OPERATION_FAILED:(NSString*)details;
+ (RNAudioException*) UNKNOWN_PLAYER_ID:(RCTPromiseRejectBlock)reject;
+ (RNAudioException*) UNKNOWN_SAMPLE_NAME:(RCTPromiseRejectBlock)reject;

@property (readonly) NSInteger code;
@end // RNAudioException

#endif /* RNAudioException_h */
