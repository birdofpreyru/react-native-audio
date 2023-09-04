#import "RNAudioException.h"

#import <React/RCTLog.h>

static NSString* const ERROR_DOMAIN = @"RNAudio";

@implementation RNAudioException

/**
 * Inits a new RNAudioException with given `name` and `details` message.
 */
- (id) initWithName:(NSString *)name details:(NSString *)details
{
  self = [super initWithName:name reason:details userInfo:nil];
  return self;
}

- (id) initWithName:(NSString*)name
  details:(NSString*)details
  userInfo:(NSDictionary<NSErrorUserInfoKey, id>*)userInfo
{
  self = [super initWithName:name reason:details userInfo:userInfo];
  return self;
}

/**
 * Creates a new NSError object based on this RNAudioException.
 */
- (NSError*) error
{
  return [NSError
          errorWithDomain:ERROR_DOMAIN
          code:self.code
          userInfo:self.userInfo];
}

/**
 * Prints this exception details into RN logs, and returns itself for chaining.
 */
- (RNAudioException*) log
{
  RCTLog(@"%@: %@", self.name, self.reason);
  return self;
}

/**
 * Rejects a pending RN promise (or, better say, its reject block).
 */
- (RNAudioException*) reject:(RCTPromiseRejectBlock)reject
{
  reject(self.name, self.reason, [self error]);
  return self;
}

+ (RNAudioException*) fromError:(NSError*)error
{
  return [[RNAudioException alloc]
          initWithName:error.domain
          details:error.localizedDescription
          userInfo:error.userInfo];
}

/**
 * Creates a new RNAudioException instance based on the given NSException.
 */
+ (RNAudioException*) fromException:(NSException*)exception
{
  return [[RNAudioException alloc]
          initWithName:exception.name
          reason:exception.reason
          userInfo:exception.userInfo];
}

/**
 * Creates a new RNAudioException instance with given `name`, and no extra error details.
 */
+ (RNAudioException*) name:(NSString*)name
{
  return [[RNAudioException alloc] initWithName:name details:nil];
}

/**
 * Creates a new RNAudioException instance with given `name` and `details` message.
 */
+ (RNAudioException*) name:(NSString*)name details:(NSString*)details
{
  return [[RNAudioException alloc] initWithName:name details:details];
}

+ (RNAudioException*) INTERNAL_ERROR:(NSString*)details
{
  return [[RNAudioException alloc]
          initWithName:@"Internal error"
          details:details];
}

+ (RNAudioException*) OPERATION_FAILED:(NSString *)details
{
  return [[RNAudioException alloc]
          initWithName:@"Operation failed"
          details:details];
}

+ (RNAudioException*) UNKNOWN_PLAYER_ID:(RCTPromiseRejectBlock)reject
{
  return [[[RNAudioException alloc]
           initWithName:@"Unknown player ID"
           details:nil]
          reject:reject];
}

+ (RNAudioException*) UNKNOWN_SAMPLE_NAME:(RCTPromiseRejectBlock)reject
{
  return [[[RNAudioException alloc]
           initWithName:@"Unknown sample name"
           details:nil]
          reject:reject];
}

@end // RNAudioException
