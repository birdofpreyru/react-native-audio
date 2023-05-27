# React Native Audio

[![Latest NPM Release](https://img.shields.io/npm/v/@dr.pogodin/react-native-audio.svg)](https://www.npmjs.com/package/@dr.pogodin/react-native-audio)
[![NPM Downloads](https://img.shields.io/npm/dm/@dr.pogodin/react-native-audio.svg)](https://www.npmjs.com/package/@dr.pogodin/react-native-audio)
[![CircleCI](https://dl.circleci.com/status-badge/img/gh/birdofpreyru/react-native-audio/tree/master.svg?style=shield)](https://app.circleci.com/pipelines/github/birdofpreyru/react-native-audio)
[![GitHub Repo stars](https://img.shields.io/github/stars/birdofpreyru/react-native-audio?style=social)](https://github.com/birdofpreyru/react-native-audio)
[![Dr. Pogodin Studio](https://raw.githubusercontent.com/birdofpreyru/react-native-audio/master/.README/logo-dr-pogodin-studio.svg)](https://dr.pogodin.studio/docs/react-native-audio)

React Native audio stream listening / recording, and utility functions for
audio system management.

[![Sponsor](https://raw.githubusercontent.com/birdofpreyru/react-native-audio/master/.README/sponsor.svg)](https://github.com/sponsors/birdofpreyru)

## Installation

```sh
npm install @dr.pogodin/react-native-audio
```

## Usage

Detailed documentation to be written, however the overall idea is this:

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

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT
