// This example app uses react-native-audio library to listen
// the microphone, do FFT transform of the signal, and depict
// resulting sound spectra as a simple text representation.

import FFT from 'fft.js';
import React, { useEffect, useRef, useState } from 'react';

import {
  Alert,
  Platform,
  Pressable,
  StyleSheet,
  View,
  Text,
} from 'react-native';

import {
  AUDIO_FORMATS,
  AUDIO_SOURCES,
  CHANNEL_CONFIGS,
  IS_MAC_CATALYST,
  getInputAvailable,
  InputAudioStream,
  configAudioSystem,
  SamplePlayer,
} from '@dr.pogodin/react-native-audio';

import {
  DocumentDirectoryPath,
  MainBundlePath,
  copyFileAssets,
} from '@dr.pogodin/react-native-fs';

const FFT_SIZE = 4096;
const SAMPLE_RATE_BASE = 44100; // [Hz]

const fft = new FFT(FFT_SIZE);
const fftOutput = fft.createComplexArray();
const spectra = new Array(16);
const DF = (SAMPLE_RATE_BASE / FFT_SIZE) * 128;

type HeapT = {
  player?: SamplePlayer;
};

export default function App() {
  const { current: heap } = useRef<HeapT>({});

  const [inputAvailable, setInputAvailable] = useState('N/A');

  const [currentChunkId, setCurrentChunkId] = useState(0);

  const [textSpectra, setTextSpectra] = useState(() => {
    const res = Array(16);
    res.fill('');
    return res;
  });

  useEffect(() => {
    // TODO: This way we miss synchornization between when the player is
    // ready to play, and when it is actually used / disposed. Fine for now,
    // not good for a production use.
    heap.player = new SamplePlayer();
    heap.player.addErrorListener((error) => {
      console.error(error);
    });

    let stream: InputAudioStream | undefined;
    (async () => {
      // Player initialization.
      // TODO: We should give an option for Android to load a sample directly
      // from assets, but for now, let's just copy it into a regular file first,
      // and load that.
      let samplePath: string;
      if (Platform.OS === 'android') {
        samplePath = `${DocumentDirectoryPath}/sample.mp3`;
        await copyFileAssets('Sine_wave_440.mp3', samplePath);
      } else if (IS_MAC_CATALYST) {
        samplePath = `${MainBundlePath}/Contents/Resources/assets/Sine_wave_440.mp3`;
      } else {
        samplePath = `${MainBundlePath}/assets/Sine_wave_440.mp3`;
      }
      await heap.player?.load('test', samplePath);

      // Setup of the input audio stream.
      const ia = await getInputAvailable();
      setInputAvailable(ia ? 'YES' : 'NO');
      if (ia) {
        await configAudioSystem();
        stream = new InputAudioStream(
          AUDIO_SOURCES.RAW,
          SAMPLE_RATE_BASE,
          CHANNEL_CONFIGS.MONO,
          AUDIO_FORMATS.PCM_16BIT,
          FFT_SIZE,
        );
        stream.addErrorListener((error) => {
          Alert.alert('React Natitve Audio Error', error.message);
        });
        stream.addChunkListener((chunk, chunkId) => {
          stream?.mute();
          setCurrentChunkId(chunkId);
          const data = new Int16Array(chunk.buffer);
          fft.realTransform(fftOutput, data);
          spectra.fill(0);
          for (let i = 0; i < FFT_SIZE / 2; ++i) {
            const bin = Math.floor(i / 128);
            spectra[bin] = fftOutput[2 * i] ** 2 + fftOutput[2 * i + 1] ** 2;
          }
          let max = 0;
          for (let i = 0; i < 16; ++i) {
            if (max < spectra[i]) max = spectra[i];
          }
          const newTextSpectra = Array(16);
          for (let i = 0; i < 16; ++i) {
            newTextSpectra[i] = ''.padEnd(
              Math.floor((16 * spectra[i]) / max),
              '*',
            );
          }
          setTextSpectra(newTextSpectra);
          stream?.unmute();
        });
        stream.start();
      }
    })();
    return () => {
      heap.player?.destroy();
      heap.player = undefined;
      stream?.destroy();
      stream = undefined;
    };
  }, [heap]);

  return (
    <View style={styles.container}>
      <Text>Input device available: {inputAvailable}</Text>
      <Text>Current chunk ID: {currentChunkId}</Text>
      <Text>Microphone signal spectra:</Text>
      {textSpectra.map((value, i) => {
        // BEWARE: Actually, this frequency calculation might be wrong.
        // Good enough for illustration purposes of library work, but should
        // be double-checked for a real calculation purposes.
        const freq = ((i + 0.5) * DF).toFixed().toString().padStart(4);
        return (
          <Text key={freq} style={styles.mono}>
            {freq} Hz: {value.padEnd(16)}
          </Text>
        );
      })}
      <Pressable
        onPressIn={async () => {
          await configAudioSystem();
          heap.player?.play('test', true);
        }}
        onPressOut={async () => {
          heap.player?.stop('test');
        }}
      >
        <Text style={styles.button}>Sample Player Test</Text>
      </Pressable>
      <Pressable
        onPressIn={async () => {
          // Note: This test repeats 3 times a 1s playback of sample,
          // stopping and immediately restarting it in-between. The purpose is
          // to check that no undesireable audible "clicks" happen at the stop /
          // start points.
          await configAudioSystem();
          heap.player?.play('test', true);
          setTimeout(() => {
            heap.player?.stop('test');
            heap.player?.play('test', true);
          }, 1000);
          setTimeout(() => {
            heap.player?.play('test', true);
          }, 2000);
          setTimeout(() => {
            heap.player?.stop('test');
          }, 3000);
        }}
      >
        <Text style={styles.button}>Sample Player Test II</Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  button: {
    backgroundColor: 'blue',
    color: 'white',
    margin: 6,
    paddingHorizontal: 12,
    paddingVertical: 6,
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
  mono: {
    // On iOS "monospace" value is not allowed by iOS, not without extra
    // setup. Just leaving it undefined is fine for us.
    fontFamily: Platform.OS !== 'ios' ? 'monospace' : undefined,
  },
});
