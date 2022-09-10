package com.drpogodin.audio;

import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.util.Base64;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.HashMap;
import java.util.Map;

public class ReactNativeAudioModuleImpl {
  public static final String NAME = "Audio";

  private int lastInputStreamId;
  private HashMap<Integer, InputAudioStream> inputStreams = new HashMap<>();

  public void configAudioSystem(Promise promise) {
    // As of now, no special configuration needed on Android.
    promise.resolve(null);
  }

  public static Map<String, Object> getConstants() {
    final Map<String, Object> constants = new HashMap<>();

    // Valid audio formats, see:
    // https://developer.android.com/reference/android/media/AudioFormat#encoding
    constants.put("AUDIO_FORMAT_PCM_8BIT", AudioFormat.ENCODING_PCM_8BIT);
    constants.put("AUDIO_FORMAT_PCM_16BIT", AudioFormat.ENCODING_PCM_16BIT);
    constants.put("AUDIO_FORMAT_PCM_FLOAT", AudioFormat.ENCODING_PCM_FLOAT);

    // All valid audio sources, as per:
    // https://developer.android.com/reference/android/media/MediaRecorder.AudioSource
    constants.put("AUDIO_SOURCE_CAMCODER", MediaRecorder.AudioSource.CAMCORDER);
    constants.put("AUDIO_SOURCE_DEFAULT", MediaRecorder.AudioSource.DEFAULT);
    constants.put("AUDIO_SOURCE_MIC", MediaRecorder.AudioSource.MIC);
    constants.put("AUDIO_SOURCE_REMOTE_SUBMIX", MediaRecorder.AudioSource.REMOTE_SUBMIX);
    constants.put("AUDIO_SOURCE_UNPROCESSED", MediaRecorder.AudioSource.UNPROCESSED);
    constants.put("AUDIO_SOURCE_VOICE_CALL", MediaRecorder.AudioSource.VOICE_CALL);
    constants.put("AUDIO_SOURCE_VOICE_COMMUNICATION", MediaRecorder.AudioSource.VOICE_COMMUNICATION);
    constants.put("AUDIO_SOURCE_VOICE_DOWNLINK", MediaRecorder.AudioSource.VOICE_DOWNLINK);
    constants.put("AUDIO_SOURCE_VOICE_PERFORMANCE", MediaRecorder.AudioSource.VOICE_PERFORMANCE);
    constants.put("AUDIO_SOURCE_VOICE_RECOGNITION", MediaRecorder.AudioSource.VOICE_RECOGNITION);
    constants.put("AUDIO_SOURCE_VOICE_UPLINK", MediaRecorder.AudioSource.VOICE_UPLINK);

    // Valid channel configuration flags, see:
    // https://developer.android.com/reference/android/media/AudioFormat#channelMask
    constants.put("CHANNEL_IN_MONO", AudioFormat.CHANNEL_IN_MONO);
    constants.put("CHANNEL_IN_STEREO", AudioFormat.CHANNEL_IN_STEREO);

    return constants;
  }

  /**
   * Sets up and runs an input audio stream.
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
   * @param samplingSize Number of samples in data chunk (per channel).
   * @param promise RN promise to resolve / reject.
   */
  public void listen(
    Double audioSource,
    Double sampleRate,
    Double channelConfig,
    Double audioFormat,
    Double samplingSize,
    DeviceEventManagerModule.RCTDeviceEventEmitter emitter,
    Promise promise
  ) {
    int streamId = ++lastInputStreamId;
    InputAudioStream stream = new InputAudioStream(
      audioSource.intValue(),
      sampleRate.intValue(),
      channelConfig.intValue(),
      audioFormat.intValue(),
      samplingSize.intValue(),
      new InputAudioStream.Listener() {
        @Override
        public void onChunk(int chunkId, byte[] chunk) {
          WritableMap event = Arguments.createMap();
          event.putDouble("streamId", streamId);
          event.putDouble("chunkId", chunkId);
          event.putString("data", Base64.encodeToString(chunk, Base64.NO_WRAP));
          emitter.emit("RNA_AudioChunk", event);
        }

        @Override
        public void onError(Exception e) {
          WritableMap event = Arguments.createMap();
          event.putDouble("streamId", streamId);
          event.putString("error", e.toString());
          emitter.emit("RNA_InputAudioStreamError", event);
        }
      });
    inputStreams.put(streamId, stream);
    promise.resolve(streamId);
  }

  /**
   * Sets the input stream's `muted` flag to the given value.
   * @param streamId
   * @param muted
   */
  public void muteInputStream(Double streamId, boolean muted) {
    inputStreams.get(streamId.intValue()).muted = muted;
  }

  /**
   * Stops, and releases an input audio stream.
   * @param streamId
   */
  public void unlisten(Double streamId) {
    inputStreams.remove(streamId.intValue()).stop();
  }
}
