# React Native Audio

[![Latest NPM Release](https://img.shields.io/npm/v/@dr.pogodin/react-native-audio.svg)](https://www.npmjs.com/package/@dr.pogodin/react-native-audio)
[![NPM Downloads](https://img.shields.io/npm/dm/@dr.pogodin/react-native-audio.svg)](https://www.npmjs.com/package/@dr.pogodin/react-native-audio)
[![CircleCI](https://dl.circleci.com/status-badge/img/gh/birdofpreyru/react-native-audio/tree/master.svg?style=shield)](https://app.circleci.com/pipelines/github/birdofpreyru/react-native-audio)
[![GitHub Repo stars](https://img.shields.io/github/stars/birdofpreyru/react-native-audio?style=social)](https://github.com/birdofpreyru/react-native-audio)
[![Dr. Pogodin Studio](https://raw.githubusercontent.com/birdofpreyru/react-native-audio/master/.README/logo-dr-pogodin-studio.svg)](https://dr.pogodin.studio/docs/react-native-audio)

React Native (RN) Audio library for Android and iOS platforms, with support of
[new][New RN Architecture] and [old][Old RN Architecture] RN architectures.
It covers:
- Input audio stream (microphone) listening / recording.
- Audio samples playback.
- Utility functions for audio system management.
- _More stuff to come_...

[![Sponsor](https://raw.githubusercontent.com/birdofpreyru/react-native-audio/master/.README/sponsor.svg)](https://github.com/sponsors/birdofpreyru)

## Content
- [Installation]
- [Getting Started]
- [API Reference]

## Installation
[Installation]: #installation

- Install the package and its peer dependencies
  ```sh
  npx install-peerdeps @dr.pogodin/react-native-audio
  ```

- Follow [react-native-permissions] documentation to setup your app for asking
  the user for the _RECORD\_AUDIO_ (Android) and/or _Microphone_ (iOS) permissions.
  **react-native-audio** library will automatically ask for these permissions,
  if needed, when a stream [.start()] method is called, provided the app has
  been correctly configured to ask for them.

## Getting Started
[Getting Started]: #getting-started

A better _Getting Started_ tutorial is to be written, however the main idea
is this:

```js
import {
  AUDIO_FORMATS,
  AUDIO_SOURCES,
  CHANNEL_CONFIGS,
  InputAudioStream,
} from "@dr.pogodin/react-native-audio";

function createAndStartAudioStream() {
  const stream = new InputAudioStream(
    AUDIO_SOURCES.RAW,
    44100, // Sample rate in Hz.
    CHANNEL_CONFIGS.MONO,
    AUDIO_FORMATS.PCM_16BIT,
    4096, // Sampling size.
  );

  stream.addErrorListener((error) => {
    // Do something with a stream error.
  });

  stream.addChunkListener((chunk, chunkId) => {
    // Pause the stream for the chunk processing. The point is: if your chunk
    // processing in this function is too slow, and chunks arrive faster than
    // this callback is able to handle them, it will rapidly crash the app,
    // with out of memory error. Muting the stream ignores any new chunks
    // until stream.unmute() is called, thus protecting from the crash.
    // And if your chunk processing is rapid enough, not chunks won't be
    // skipped. The "chunkId" argument is just sequential chunk numbers,
    // by which you may judge whether any chunks have been skipped between
    // this callback calls or not.
    stream.mute();

    // Do something with the chunk.

    // Resumes the stream.
    stream.unmute();
  });

  stream.start();

  // Call stream.destroy() to stop the stream and release any associated
  // resources. If you need to temporarily stop and then resume the stream,
  // use .mute() and .unmute() methods instead.
}
```
and on top of this the library will include other auxiliary methods related
to audio input and output.

## API Reference
[API Reference]: #api-reference

- [Classes]
  - [InputAudioStream] &mdash; Represents individual input audio streams.
    - [constructor()][InputAudioStream.constructor()] &mdash; Creates
      a new [InputAudioStream] instance.
    - [.addChunkListener()] &mdash; Adds a new audio data chunk listener to
      the stream.
    - [.addErrorListener()][InputAudioStream.addErrorListener()] &mdash; Adds a new error listener to the stream.
    - [.destroy()][InputAudioStream.destroy()] &mdash; Destroys the stream
      &mdash; stops recording, and releases all related resources.
    - [.mute()] &mdash; Mutes the stream.
    - [.removeChunkListener()] &mdash; Removes an audio data chunk listener
      from the stream.
    - [.removeErrorListener()][InputAudioStream.removeErrorListener()] &mdash;
      Removes an error listener from the stream.
    - [.start()] &mdash; Starts the audio stream recording.
    - [.stop()][InputAudioStream.stop()] &mdash; Stops the stream.
    - [.unmute()] &mdash; Unmutes a previously muted stream.
    - [.active] &mdash; _true_ when the stream is started and recoding.
    - [.audioFormat] &mdash; Holds the audio format value provided to
      the [constructor()][InputAudioStream.constructor()].
    - [.audioSource] &mdash; Holds the audio source value provided to
      the [constructor()][InputAudioStream.constructor()].
    - [.channelConfig] &mdash; Holds the channel mode value provided to
      the [constructor()][InputAudioStream.constructor()].
    - [.muted] &mdash; _true_ when the stream is muted.
    - [.sampleRate] &mdash; Holds the stream's sample rate in [[Hz]].
    - [.samplingSize] &mdash; Holds the stream's sampling (audio data chunk)
      size, per channel.
    - [.stopInBackground] &mdash; _true_ if the stream is configured to stop
      automatically when the app leaves foreground, and to start again when it
      returns to the foreground.
  - [SamplePlayer] &mdash; Represents an audio sample player.
    - [constructor()][SamplePlayer.constructor()] &mdash; Creates a new
      [SamplePlayer] instance.
    - [.addErrorListener()][SamplePlayer.addErrorListener()] &mdash; Adds a new
      error listener to the player.
    - [.destroy()][SamplePlayer.destroy()] &mdash; Destroys the player,
      releasing all related resources.
    - [.load()] &mdash; Loads an (additional) audio sample.
    - [.play()] &mdash; Plays an audio sample.
    - [.removeErrorListener()][SamplePlayer.removeErrorListener()] &mdash;
      Removes an error listener from the player.
    - [.stop()][SamplePlayer.stop()] &mdash; Stops an audio sample playback.
    - [.unload()] &mdash; Unloads an audio sample.
- [Constants]
  - [AUDIO_FORMATS] &mdash; Provides valid [.audioFormat] values.
  - [AUDIO_SOURCES] &mdash; Provides valid [.audioSource] values.
  - [CHANNEL_CONFIGS] &mdash; Provides valid [.channelConfig] values.
  - [IS_MAC_CATALYST] &mdash; _true_ if app is running on macOS (Catalyst).
- [Functions]
  - [configAudioSystem()] &mdash; Configures audio system (input & output devices)
    for iOS, does nothing on Android.
  - [getInputAvailable()] &mdash; Resolves _true_ if device has an available
    audio input source.
- [Types]
  - [ChunkListener] &mdash; The type of audio data chunk listeners that can be
    connected to a stream with [.addChunkListener()] method.
  - [ErrorListener] &mdash; The type of error listeners that can be connected to
    [InputAudioStream] and [SamplePlayer] instances using their corresponding
    `.addErrorListener()` methods (see [stream method][InputAudioStream.addErrorListener()],
    and [player method][SamplePlayer.addErrorListener()]).

## Classes
[Classes]: #classes

### InputAudioStream
[InputAudioStream]: #inputaudiostream
```tsx
class InputAudioStream;
```

The [InputAudioStream] class, as its name suggests, represents individual input
audio streams, capturing audio data in the configured format from the specified
audio source.

<a id="inputaudiostream-constructor"></a>
#### constructor()
[InputAudioStream.constructor()]: #inputaudiostream-constructor
```tsx
const stream = new InputAudioStream(
  audioSource: AUDIO_SOURCES,
  sampleRate: number,
  channelConfig: CHANNEL_CONFIGS,
  audioFormat: AUDIO_FORMATS,
  samplingSize: number,
  stopInBackground: boolean = true,
);
```
Creates a new [InputAudioStream] instance. The newly created stream does not
record audio, neither consumes resources at the native side until its [.start()]
method is called.

- `audioSource` &mdash; [AUDIO_SOURCES] &mdash; The audio source this stream
  will listen to. Currently, it is supported for Android only; on iOS this value
  is just ignored, and the stream captures audio data from the default input
  source of the device.
- `sampleRate` &mdash; **number** &mdash; Sample rate [[Hz]].
  44100 Hz is the recommended value, as it is the only rate that is
  guaranteed to work on all Android (and many other) devices.
- `channelConfig` &mdash; [CHANNEL_CONFIGS] &mdash; _Mono_ or _Stereo_ stream
  mode.
- `audioFormat` &mdash; [AUDIO_FORMATS] &mdash; Audio format.
- `samplingSize` &mdash; **number** &mdash; Sampling (data chunk) size,
  expressed as the number of samples per channel in the chunk.
- `stopInBackground` &mdash; **boolean** &mdash; Optional. It _true_ (default)
  the stream will automatically pause itself when the app leaves the foreground,
  and the stream will automatically resume itself when the app returns to
  the foreground.

#### .addChunkListener()
[.addChunkListener()]: #addchunklistener
```ts
stream.addChunkListener(listener: ChunkListener): void;
```
Adds a new audio data chunk listener to the stream. See [.removeChunkListener()]
to subsequently remove the listener from the stream.

**Note:** It is safe to call it repeatedly for the same listener & stream pair
&mdash; the listener still won't be added to the stream more than once.

- `listener` &mdash; [ChunkListener] &mdash; The callback to call with audio
  data chunks when they arrive.

<a id="inputaudiostream-adderrorlistener"></a>
#### .addErrorListener()
[InputAudioStream.addErrorListener()]: #inputaudiostream-adderrorlistener
```ts
stream.addErrorListener(listener: ErrorListener): void;
```
Adds a new error listener to the stream. See [.removeErrorListener()][InputAudioStream.removeErrorListener()]
to subsequently remove the listener from the stream.

**Note:** It is safe to call it repeatedly for the same listener & stream pair
&mdash; the listener still won't be added to the stream more than once.

- `listener` &mdash; [ErrorListener] &mdash; The callback to call with error
  details, if any error happens in the stream.

<a id="inputaudiostream-destroy"></a>
#### .destroy()
[InputAudioStream.destroy()]: #inputaudiostream-destroy
```ts
stream.destroy(): void;
```
Destroys the stream &mdash; stops the recording, and releases all related
resources, both at the native and JS sides. Once a stream is destroyed,
it cannot be re-used.

#### .mute()
[.mute()]: #mute
```ts
stream.mute(): void;
```
Mutes the stream. A muted stream still continues to capture audio data chunks
from the audio source, and thus keeps incrementing chunk IDs (see [ChunkListener]),
but it discards all data chunks immediately after the capture, without sending
them to the JavaScript layer, thus causing the minimal performance and memory
overhead possible without interrupting the recording.

Calling [.mute()] on a muted, or non-active (not recording) audio stream has
no effect. See also [.active], [.muted].

#### .removeChunkListener()
[.removeChunkListener()]: #removechunklistener
```ts
stream.removeChunkListener(listener: ChunkListener): void;
```
Removes the listener from the stream. No operation if given `listener` is not
connected to the stream. See [.addChunkListener()] to add the listener.
- `listener` &mdash; [ChunkListener] &mdash; The listener to disconnect.

#### .removeErrorListener()
[InputAudioStream.removeErrorListener()]: #removeerrorlistener
```ts
stream.removeErrorListener(listener: ErrorListener): void;
```
Removes the listener from the stream. No operation if given `listener` is not
connected to the stream. See [.addErrorListener()][InputAudioStream.addErrorListener()] to connect the listener.

- `listener` &mdash; [ErrorListener] &mdash; The listener to disconnect.

#### .start()
[.start()]: #start
```ts
stream.start(): Promise<boolean>;
```
Starts the audio stream recording. This method actually initializes the stream
on the native side, and starts the recording.

**Note:** If necessary, this method will ask app user for the audio recoding
permission, using the [react-native-permissions] library.

- Resolves to **boolean** value &mdash; _true_ if the stream has started
  successfully and is [.active], _false_ otherwise.

<a id="inputaudiostream-stop"></a>
#### .stop()
[InputAudioStream.stop()]: #inputaudiostream-stop
```ts
stream.stop(): Promise<void>;
```
Stops the stream. Unlike the [.mute()] method, [.stop()][InputAudioStream.stop()] actually stops
the audio stream and releases its resources on the native side; however,
unlike the [.destroy()][InputAudioStream.destroy()] method, it does not release its resource in the JS
layer (_i.e._ does not drop references to all connected listeners), thus
allowing to [.start()] this stream instance again (which will technically
will init a new stream on the native side, but it will be opaque to the end
user on the JS side).

- Resolves once the stream is stopped.

#### .unmute()
[.unmute()]: #unmute
```ts
stream.unmute(): void;
```
Unmutes a previously [.muted] stream. It has no effect if called on inactive
(non started), or already muted stream.

#### .active
[.active]: #active
```ts
stream.active: boolean;
```
Read-only. _true_ when the stream is [started][.start()] and recording, _false_
otherwise.

**Note:** [.active] will be _true_ for a started and [.muted] stream.

#### .audioFormat
[.audioFormat]: #audioformat
```ts
stream.audioFormat: AUDIO_FORMATS;
```
Read-only. Holds the audio format value provided to [InputAudioStream]'s
[constructor()][InputAudioStream.constructor()]. [AUDIO_FORMATS] enum provides valid format values.

#### .audioSource
[.audioSource]: #audiosource
```ts
stream.audioSource: AUDIO_SOURCES;
```
Read-only. Holds the audio source value provided to [InputAudioStream]'s
[constructor()][InputAudioStream.constructor()]. As of now it only has an affect on Android devices, and it is
ignored for iOS. [AUDIO_SOURCES] enum provides valid audio source values.

#### .channelConfig
[.channelConfig]: #channelconfig
```ts
stream.channelConfig: CHANNEL_CONFIGS;
```
Read-only. Holds the channel mode (_Mono_ or _Stereo_) value provided to
[InputAudioStream]'s [constructor()][InputAudioStream.constructor()]. [CHANNEL_CONFIGS] enum provides valid
channel mode values.

#### .muted
[.muted]: #muted
```ts
stream.muted: boolean;
```
Read-only. _true_ when the stream is muted by [.mute()], _false_ otherwise.

#### .sampleRate
[.sampleRate]: #samplerate
```ts
stream.sampleRate: number;
```
Read-only. Holds the stream's sample rate provided to the stream [constructor()][InputAudioStream.constructor()],
in [[Hz]].

#### .samplingSize
[.samplingSize]: #samplingsize
```ts
stream.samplingSize: number;
```
Read-only. Holds the stream's sampling (audio data chunk) size, provided to
the stream [constructor()][InputAudioStream.constructor()]. The value is the number of samples per channel,
thus for multi-channel streams the actual chunk size will be a multiple of
this number, and also the sample size in bytes may vary for different
[.audioFormat].

#### .stopInBackground
[.stopInBackground]: #stopinbackground
```ts
stream.stopInBackground: boolean;
```
Read-only. _true_ if the stream is set to automatically [.stop()][InputAudioStream.stop()] when the app
leaves foreground, and [.start()] again when it returns to the foreground.

### SamplePlayer
[SamplePlayer]: #sampleplayer
```ts
class SamplePlayer;
```
Represents an audio sample player. It is intended for loading into the memory
a set of short audio fragments, which then can be played at demand with a low
latency.

On Android we use [SoundPool](https://developer.android.com/reference/android/media/SoundPool)
for the underlying implementation, you may check its documentation for further
details. In particular note: _each decoded sound is internally limited to one
megabyte storage, whcih represents approximately 5.6 seconds at 44.1Hz stereo
(the duration is proportionally longer at lower sample rates or a channel mask
of mono)._

<a id="sampleplayer-constructor"></a>
#### constructor()
[SamplePlayer.constructor()]: #sampleplayer-constructor
```ts
const player = new SamplePlayer();
```
Creates a new [SamplePlayer] instance. Note that this creation of [SamplePlayer]
instance already allocates some resources at the native side, thus to release
those resources you MUST USE its [.destroy()][SamplePlayer.destroy()] method
once the instance is not needed anymore.

<a id="sampleplayer-adderrorlistener"></a>
#### .addErrorListener()
[SamplePlayer.addErrorListener()]: #sampleplayer-adderrorlistener
```ts
player.addErrorListener(listener: ErrorListener): void;
```
Adds an error listener to the player. Does nothing if given `listener` is
already added to this player.

- `listener` &mdash; [ErrorListener] &mdash; Error listener.

<a id="sampleplayer-destroy"></a>
#### .destroy()
[SamplePlayer.destroy()]: #sampleplayer-destroy
```ts
player.destroy(): Promise<void>;
```
Destroys player instance, releasing all related resources. Once destroyed
the player instance can't be reused.
- Resolves once completed.

#### .load()
[.load()]: #load
```ts
player.load(sampleName: string, samplePath: string): Promise<void>;
```
Loads an (additional) audio sample into the player.
- `sampleName` &mdash; **string** &mdash; Sample name, by which you'll refer
  to the loaded sample in other methods, like [.play()], [SamplePlayer.stop()],
  and [.unload()]. If it matches a name of a previously loaded sample, that
  sample will be replaced.
- `samplePath` &mdash; **string** &mdash; Path to the sample file on the device.
  For now, only loading samples from regular files is supported (_e.g._ not
  possible to load from Android asset, without first copying the asset into
  a regular file).
- Resolves once the sample is loaded and decoded, thus ready to be played.

#### .play()
[.play()]: #play
```ts
player.play(sampleName: string, loop: boolean): Promise<void>;
```
Plays an audio sample, previously loaded with [.load()] method.

**NOTE:** In the current implementation, starting a sample playback always stops
the ongoing playback of a sample previously played by the same player, if any.
There is no technical barrier to support playback of multiple samples at
the same time, it just needs some more development effort.

**NOTE:** Use [.addErrorListener()][SamplePlayer.addErrorListener()] method to
recieve details of any errors that happen during the playback. Although [.play()]
itself rejects if the playback fails to start, that rejection message does not
provide any details beyond the fact of the failure, and it also does not capture
any further errors (as the playback itself is asynchronous).

- `sampleName` &mdash; **string** &mdash; Sample name, assinged when loading it
  with the [.load()] method.
- `loop` &mdash; **boolean** &mdash; Set _true_ to infinitely loop the sample;
  or _false_ to play it once.
- Resolves once the playback is launched; rejects if the playback fails to start
  due to some error.

<a id="sampleplayer-removeerrorlistener"></a>
#### .removeErrorListener()
[SamplePlayer.removeErrorListener()]: #sampleplayer-removeerrorlistener
```ts
player.removeErrorListener(listener: ErrorListener): void;
```
Removes `listener` from this `player`, or does nothing if the listener is not
connected to the player.

- `listener` &mdash; [ErrorListener] &mdash; Error listener to disconnect.

<a id="sampleplayer-stop"></a>
#### .stop()
[SamplePlayer.stop()]: #sampleplayer-stop
```ts
player.stop(sampleName: string): Promise<void>;
```
Stops sample playback, does nothing if the sample is not being played by this
player.
- `sampleName` &mdash; **string** &mdash; Sample name.
- Resolves once completed.

#### .unload()
[.unload()]: #unload
```ts
player.unload(sampleName: string): Promise<void>;
```
Unloads an audio sample previouly loaded into this player.
- `sampleName` &mdash; **string** &mdash; Sample name.
- Resolves once completed.

## Constants
[Constants]: #constants

### AUDIO_FORMATS
[AUDIO_FORMATS]: #audio_formats
```ts
enum AUDIO_FORMATS {
  PCM_8BIT: number;
  PCM_16BIT: number;
  PCM_FLOAT: number;
};
```
Provides valid [.audioFormat] values. See
[Android documentation](https://developer.android.com/reference/android/media/AudioFormat#encoding)
for exact definitions of these three formats; they should be the same on iOS
devices.

**Note:** At least Android allows for other audio formats, which we may include
here in future.

### AUDIO_SOURCES
[AUDIO_SOURCES]: #audio_sources
```ts
enum AUDIO_SOURCES {
  CAMCODER: number;
  DEFAULT: number;
  MIC: number;
  REMOTE_SUBMIX: number;
  RAW: number;
  VOICE_CALL: number;
  VOICE_COMMUNICATION: number;
  VOICE_DOWNLINK: number;
  VOICE_PERFORMANCE: number;
  VOICE_RECOGNITION: number;
  VOICE_UPLINK: number;
};
```
Provides valid [.audioSource] values. As of now, they have effect for Android
devices only, and for them they represent corresponding values of
[MediaRecorder.AudioSource](https://developer.android.com/reference/android/media/MediaRecorder.AudioSource).

### CHANNEL_CONFIGS
[CHANNEL_CONFIGS]: #channel_configs
```ts
enum CHANNEL_CONFIGS {
  MONO: number;
  STEREO: number;
};
```
Provides valid [.channelConfig] values.

**Note:** As of now, it provides only two values, _MONO_ and _STEREO_, however,
at least Android seems to support additional channels, which might be added in
future, see
[Android's AudioFormat documentation](https://developer.android.com/reference/android/media/AudioFormat).

### IS_MAC_CATALYST
[IS_MAC_CATALYST]: #is_mac_catalyst
```ts
const IS_MAC_CATALYST: boolean;
```
Equals _true_ if the app is running on the macOS (Catalyst) platform;
_false_ otherwise.

## Functions
[Functions]: #functions

### configAudioSystem()
[configAudioSystem()]: #configaudiosystem
```ts
function configAudioSystem(): Promise<void>;
```
Configures audio system (input & output devices).

Currently it does nothing on Android; on iOS it (re-)configures the audio
session, setting the _Play & Record_ category and activating the session.

**Note:** On iOS, if _Play & Record_ category is not available on the device,
it sets the _Playback_ category instead; and if neither category is available,
the function rejects its result promise. The function also sets the following
options for the iOS audio session: _AllowBluetooth_, _AllowBluetoothA2DP_, and
_DefaultToSpeaker_.

See [iOS documentation](https://developer.apple.com/documentation/avfaudio/avaudiosession?language=objc)
for further details about iOS audio sessions and categories.

- Resolves once completed.

### getInputAvailable()
[getInputAvailable()]: #getinputavailable
```ts
function getInputAvailable(): Promise<boolean>;
```
- Resolves _true_ if device has an available audio input source,
  _false_ otherwise.

## Types
[Types]: #types

### ChunkListener
[ChunkListener]: #chunklistener
```ts
type ChunkListener = (chunk: Buffer, chunkId: number) => void;
```
The type of audio data chunk listeners that can be connected to an
[InputAudioStream] with [.addChunkListener()] method.

- `chunk` &mdash; [Buffer] &mdash; Audio data chunk in the format specified
  upon the audio stream [construction][InputAudioStream.constructor()]. [Buffer] implementation
  for RN is provided by [the `buffer` library](https://www.npmjs.com/package/buffer).
- `chunkId` &mdash; **number** &mdash; Consequtive chunk number. When a stream
  is [.muted] the chunk numbers are still incremented for discarted audio chunks,
  thus `chunkId` may be used to judge whether any chunks were missed while
  a stream was muted.

### ErrorListener
[ErrorListener]: #errorlistener
```ts
type ErrorListener = (error: Error) => void;
```
The type of error listeners that can be connected to an [InputAudioStream] with
[.addErrorListener()][InputAudioStream.addErrorListener()] method.

- `error` &mdash; [Error] &mdash; Stream error.

<!-- Global references. -->
[Buffer]: https://nodejs.org/api/buffer.html
[Error]: https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Error
[Hz]: https://en.wikipedia.org/wiki/Hertz
[New RN Architecture]: https://reactnative.dev/docs/the-new-architecture/pillars-turbomodules
[Old RN Architecture]: https://reactnative.dev/docs/native-modules-intro
[react-native-permissions]: https://github.com/zoontek/react-native-permissions
