package com.drpogodin.reactnativeaudio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.util.Base64;
import androidx.annotation.NonNull;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import com.drpogodin.reactnativeaudio.Errors;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ReactNativeAudioModule extends ReactNativeAudioSpec {
  public static final String NAME = "ReactNativeAudio";

  private HashMap<Double,InputAudioStream> inputStreams = new HashMap<>();

  ReactNativeAudioModule(ReactApplicationContext context) {
    super(context);
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

  @ReactMethod
  public void configAudioSystem(Promise promise) {
    // As of now, no special configuration needed on Android.
    promise.resolve(null);
  }

  public Map<String,Object> getTypedExportedConstants() {
    final Map<String,Object> constants = new HashMap<>();

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

  @ReactMethod
  public void getInputAvailable(Promise promise) {
    Context ctxt = getReactApplicationContext().getApplicationContext();
    AudioManager manager = (AudioManager)ctxt.getSystemService(
      Context.AUDIO_SERVICE);
    try {
      promise.resolve(manager.getMicrophones().size() > 0);
    } catch (IOException e) {
      String msg = "Failed to get microphone list";
      promise.reject(
        "ReactNativeAudio:getInputAvailable",
        msg, new Error(msg)
      );
    }
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
  @ReactMethod
  public void listen(
    double streamId,
    double audioSource,
    double sampleRate,
    double channelConfig,
    double audioFormat,
    double samplingSize,
    Promise promise
  ) {
    DeviceEventManagerModule.RCTDeviceEventEmitter emitter =
      getReactApplicationContext()
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class);

    InputAudioStream stream = new InputAudioStream(
      (int)audioSource,
      (int)sampleRate,
      (int)channelConfig,
      (int)audioFormat,
      (int)samplingSize,
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
    promise.resolve(null);
  }

  /**
   * Sets the input stream's `muted` flag to the given value.
   * @param streamId
   * @param muted
   */
  @ReactMethod
  public void muteInputStream(double streamId, boolean muted) {
    inputStreams.get(streamId).muted = muted;
  }

  /**
   * This method is currently used only for a sound playback test in
   * the Example App, and this test for now is intended mostly for testing
   * the audio session configuration on iOS, thus not yet implemented for
   * Android.
   */
  @ReactMethod
  public void playTest() {
    Errors.NOT_IMPLEMENTED.log();
  }

  /**
   * Stops, and releases an input audio stream.
   * @param streamId
   */
  @ReactMethod
  public void unlisten(double streamId, Promise promise) {
    inputStreams.remove(streamId).stop();
    promise.resolve(null);
  }

  @ReactMethod
  public void addListener(String eventName) {
    // NOOP
  }

  @ReactMethod
  public void removeListeners(double count) {
    // NOOP
  }
}
