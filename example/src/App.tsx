// This example app uses react-native-audio library to listen
// the microphone, do FFT transform of the signal, and depict
// resulting sound spectra as a simple text representation.

import FFT from 'fft.js';
import React, { useEffect, useState } from 'react';

import { Alert, Button, Platform, StyleSheet, View, Text } from 'react-native';

import {
  AUDIO_FORMATS,
  AUDIO_SOURCES,
  CHANNEL_CONFIGS,
  getInputAvailable,
  InputAudioStream,
  playTest,
  configAudioSystem,
} from '@dr.pogodin/react-native-audio';

const FFT_SIZE = 4096;
const SAMPLE_RATE_BASE = 44100; // [Hz]

const fft = new FFT(FFT_SIZE);
const fftOutput = fft.createComplexArray();
const spectra = new Array(16);
const DF = (SAMPLE_RATE_BASE / FFT_SIZE) * 128;

export default function App() {
  const [inputAvailable, setInputAvailable] = useState('N/A');

  const [currentChunkId, setCurrentChunkId] = useState(0);

  const [textSpectra, setTextSpectra] = useState(() => {
    const res = Array(16);
    res.fill('');
    return res;
  });

  useEffect(() => {
    let stream: InputAudioStream | undefined;
    (async () => {
      const ia = await getInputAvailable();
      setInputAvailable(ia ? 'YES' : 'NO');
      if (ia) {
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
      stream?.destroy();
      stream = undefined;
    };
  }, []);

  // const [result, setResult] = React.useState<number | undefined>();

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
      <Button
        title="Playback test"
        onPress={async () => {
          await configAudioSystem();
          playTest();
        }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
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
