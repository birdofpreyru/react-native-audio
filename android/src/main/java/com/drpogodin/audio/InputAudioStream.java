package com.drpogodin.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;

/**
 * Represents an input audio stream.
 */
public class InputAudioStream {
  /**
   * Interface definition for an audio chunk listener.
   */
  public interface Listener {
    /**
     * Called when a new audio data chunk is ready.
     * @param chunkId Consequent chunk number (the count includes chunks which were not delivered
     *                because of the stream being muted).
     * @param chunk
     */
    void onChunk(int chunkId, byte[] chunk);

    /**
     * Called when any error happens during the stream initialization or lifetime.
     * @param e
     */
    void onError(Exception e);
  }

  /**
   * This flag governs whether current audio chunks are sent to the listener (if false),
   * or discarded silently (if true).
   */
  public boolean muted;

  /**
   * The execution thread.
   */
  private Thread thread;

  /**
   * Creates and starts a new AudioStream.
   * @param audioSource Audio source. Valid values are:
   *                    https://developer.android.com/reference/android/media/MediaRecorder.AudioSource#summary
   * @param sampleRate Sample rate [Hz]. 44100 Hz is currently the only rate that is guaranteed
   *                   to work on all devices. Zero value means the default rate, which is usually
   *                   the audio source sample rate.
   * @param channelConfig Valid values are:
   *                      - AudioFormat.CHANNEL_IN_MONO
   *                      - AudioFormat.CHANNEL_IN_STEREO
   * @param audioFormat Valid values are:
   *                    - AudioFormat.ENCODING_PCM_8BIT
   *                    - AudioFormat.ENCODING_PCM_16BIT
   *                    - AudioFormat.ENCODING_PCM_FLOAT
   * @param samplingSize Number of samples in chunk (per channel).
   * @param listener Chunk listener.
   */
  InputAudioStream(int audioSource, int sampleRate, int channelConfig, int audioFormat,
                   int samplingSize, Listener listener) {
    this.thread = new Thread() {
      @Override
      public void run() {
        AudioRecord record = null;

        try {
          // Initialization.
          int frameSize = getNumChannels(channelConfig) * getSampleSize(audioFormat); // bytes
          int chunkSize = frameSize * samplingSize; // bytes
          byte[] chunk = new byte[chunkSize];

          int minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
          int bufferSize = Math.max(3 * chunkSize, minBufferSize); // bytes

          record = new AudioRecord(audioSource, sampleRate, channelConfig, audioFormat,
            bufferSize);

          if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            throw new Exception("Failed to start audio recording");
          }

          record.startRecording();

          // Main lifetime.
          int chunkId = 0;
          while (!isInterrupted()) {
            record.read(chunk, 0, chunkSize);
            if (!muted) listener.onChunk(chunkId, chunk);
            ++chunkId;
          }

          // De-initialization.
          record.stop();
        } catch (SecurityException e) {
          listener.onError(e);
        } catch (Exception e) {
          listener.onError(e);
        }

        if (record != null) record.release();
      }
    };
    this.thread.start();
  }

  /**
   * Stops the stream, and releases all related resources.
   */
  public void stop() {
    this.thread.interrupt();
  }

  /**
   * Returns the number of channels corresponding the given config.
   * @param channelConfig
   * @return Number of channels.
   * @throws Exception
   */
  private static int getNumChannels(int channelConfig) throws Exception {
    switch (channelConfig) {
      case AudioFormat.CHANNEL_IN_MONO: return 1;
      case AudioFormat.CHANNEL_IN_STEREO: return 2;
      default: throw new Exception("Invalid channel config");
    }
  }

  /**
   * Returns sample size [bytes] for the given audio format.
   * @param audioFormat
   * @return Sample size [bytes].
   * @throws Exception
   */
  private static int getSampleSize(int audioFormat) throws Exception {
    switch (audioFormat) {
      case AudioFormat.ENCODING_PCM_8BIT: return 1;
      case AudioFormat.ENCODING_PCM_16BIT: return 2;
      case AudioFormat.ENCODING_PCM_FLOAT: return 4;
      default: throw new Exception("Invalid audio format");
    }
  }
}
