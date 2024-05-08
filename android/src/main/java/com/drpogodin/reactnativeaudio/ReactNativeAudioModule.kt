package com.drpogodin.reactnativeaudio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.modules.core.DeviceEventManagerModule
import java.io.IOException

class ReactNativeAudioModule internal constructor(context: ReactApplicationContext) :
  ReactNativeAudioSpec(context) {
    private val inputStreams = HashMap<Double, InputAudioStream>()
    private val samplePlayers = HashMap<Double, SamplePlayer>()
    @ReactMethod
    override fun configAudioSystem(promise: Promise) {
        // As of now, no special configuration needed on Android.
        promise.resolve(null)
    }

    override fun getTypedExportedConstants(): MutableMap<String, Any> {
        val constants: MutableMap<String, Any> = HashMap()

        // Valid audio formats, see:
        // https://developer.android.com/reference/android/media/AudioFormat#encoding
        constants["AUDIO_FORMAT_PCM_8BIT"] = AudioFormat.ENCODING_PCM_8BIT
        constants["AUDIO_FORMAT_PCM_16BIT"] = AudioFormat.ENCODING_PCM_16BIT
        constants["AUDIO_FORMAT_PCM_FLOAT"] = AudioFormat.ENCODING_PCM_FLOAT

        // All valid audio sources, as per:
        // https://developer.android.com/reference/android/media/MediaRecorder.AudioSource
        constants["AUDIO_SOURCE_CAMCODER"] = MediaRecorder.AudioSource.CAMCORDER
        constants["AUDIO_SOURCE_DEFAULT"] = MediaRecorder.AudioSource.DEFAULT
        constants["AUDIO_SOURCE_MIC"] = MediaRecorder.AudioSource.MIC
        constants["AUDIO_SOURCE_REMOTE_SUBMIX"] = MediaRecorder.AudioSource.REMOTE_SUBMIX

        constants["AUDIO_SOURCE_UNPROCESSED"] =
          if (Build.VERSION.SDK_INT >= 24) MediaRecorder.AudioSource.UNPROCESSED
          else MediaRecorder.AudioSource.DEFAULT

        constants["AUDIO_SOURCE_VOICE_CALL"] = MediaRecorder.AudioSource.VOICE_CALL
        constants["AUDIO_SOURCE_VOICE_COMMUNICATION"] = MediaRecorder.AudioSource.VOICE_COMMUNICATION
        constants["AUDIO_SOURCE_VOICE_DOWNLINK"] = MediaRecorder.AudioSource.VOICE_DOWNLINK

        constants["AUDIO_SOURCE_VOICE_PERFORMANCE"] =
          if (Build.VERSION.SDK_INT >= 29) MediaRecorder.AudioSource.VOICE_PERFORMANCE
          else MediaRecorder.AudioSource.DEFAULT

        constants["AUDIO_SOURCE_VOICE_RECOGNITION"] = MediaRecorder.AudioSource.VOICE_RECOGNITION
        constants["AUDIO_SOURCE_VOICE_UPLINK"] = MediaRecorder.AudioSource.VOICE_UPLINK

        // Valid channel configuration flags, see:
        // https://developer.android.com/reference/android/media/AudioFormat#channelMask
        constants["CHANNEL_IN_MONO"] = AudioFormat.CHANNEL_IN_MONO
        constants["CHANNEL_IN_STEREO"] = AudioFormat.CHANNEL_IN_STEREO
        constants["IS_MAC_CATALYST"] = false
        return constants
    }

    @ReactMethod
    override fun getInputAvailable(promise: Promise) {
        val ctxt: Context = reactApplicationContext.applicationContext
        val manager = ctxt.getSystemService(
                Context.AUDIO_SERVICE) as AudioManager
        try {
            if (Build.VERSION.SDK_INT >= 28) promise.resolve(manager.microphones.size > 0)
            else Errors.NOT_IMPLEMENTED.reject(promise, "Requires Android SDK 28 or above")
        } catch (e: IOException) {
            val msg = "Failed to get microphone list"
            promise.reject(
                    "ReactNativeAudio:getInputAvailable",
                    msg, Error(msg)
            )
        }
    }

    /**
     * Sets up and runs an input audio stream.
     * @param audioSource Audio source. Valid values are:
     * https://developer.android.com/reference/android/media/MediaRecorder.AudioSource#summary
     * @param sampleRate Sample rate (Hz). 44100 Hz is currently the only rate that is guaranteed
     * to work on all devices. Zero value means the default rate, which is usually
     * the audio source sample rate.
     * @param channelConfig Valid values are:
     * - AudioFormat.CHANNEL_IN_MONO
     * - AudioFormat.CHANNEL_IN_STEREO
     * @param audioFormat Valid values are:
     * - AudioFormat.ENCODING_PCM_8BIT
     * - AudioFormat.ENCODING_PCM_16BIT
     * - AudioFormat.ENCODING_PCM_FLOAT
     * @param samplingSize Number of samples in data chunk (per channel).
     * @param promise RN promise to resolve / reject.
     */
    @ReactMethod
    override fun listen(
            streamId: Double,
            audioSource: Double,
            sampleRate: Double,
            channelConfig: Double,
            audioFormat: Double,
            samplingSize: Double,
            promise: Promise
    ) {
        val emitter: DeviceEventManagerModule.RCTDeviceEventEmitter = reactApplicationContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
        val stream = InputAudioStream(audioSource.toInt(), sampleRate.toInt(), channelConfig.toInt(), audioFormat.toInt(), samplingSize.toInt(),
                object : InputAudioStream.Listener {
                    override fun onChunk(chunkId: Int, chunk: ByteArray?) {
                        val event = Arguments.createMap()
                        event.putDouble("streamId", streamId)
                        event.putDouble("chunkId", chunkId.toDouble())
                        event.putString("data", Base64.encodeToString(chunk, Base64.NO_WRAP))
                        emitter.emit("RNA_AudioChunk", event)
                    }

                    override fun onError(e: Exception?) {
                        val event = Arguments.createMap()
                        event.putDouble("streamId", streamId)
                        event.putString("error", e.toString())
                        emitter.emit("RNA_InputAudioStreamError", event)
                    }
                })
        inputStreams[streamId] = stream
        promise.resolve(null)
    }

    /**
     * Sets the input stream's `muted` flag to the given value.
     * @param streamId
     * @param muted
     */
    @ReactMethod
    override fun muteInputStream(streamId: Double, muted: Boolean) {
        inputStreams[streamId]!!.muted = muted
    }

    /**
     * Stops, and releases an input audio stream.
     * @param streamId
     */
    @ReactMethod
    override fun unlisten(streamId: Double, promise: Promise) {
        inputStreams.remove(streamId)!!.stop()
        promise.resolve(null)
    }

    @ReactMethod
    override fun addListener(eventName: String?) {
        // NOOP
    }

    @ReactMethod
    override fun removeListeners(count: Double) {
        // NOOP
    }

    // These methods are for SamplePlayer functionality.
    @ReactMethod
    override fun initSamplePlayer(playerId: Double, promise: Promise) {
        if (samplePlayers.containsKey(playerId)) {
            Errors.INTERNAL_ERROR.reject(promise, "Sample player ID is occupied")
            return
        }
        samplePlayers[playerId] = SamplePlayer()
        promise.resolve(null)
    }

    @ReactMethod
    override fun destroySamplePlayer(playerId: Double, promise: Promise) {
        val player = samplePlayers.remove(playerId)
        if (player == null) {
            Errors.UNKNOWN_PLAYER_ID.reject(promise)
            return
        }
        player.destroy()
        promise.resolve(null)
    }

    @ReactMethod
    override fun loadSample(
            playerId: Double,
            sampleName: String,
            samplePath: String,
            promise: Promise
    ) {
        val player = samplePlayers[playerId]
        if (player == null) Errors.UNKNOWN_PLAYER_ID.reject(promise) else player.load(sampleName, samplePath, promise)
    }

    @ReactMethod
    override fun playSample(
            playerId: Double,
            sampleName: String,
            loop: Boolean,
            promise: Promise
    ) {
        val player = samplePlayers[playerId]
        if (player == null) Errors.UNKNOWN_PLAYER_ID.reject(promise) else player.play(sampleName, loop, promise)
    }

    @ReactMethod
    override fun stopSample(playerId: Double, sampleName: String, promise: Promise) {
        val player = samplePlayers[playerId]
        if (player == null) Errors.UNKNOWN_PLAYER_ID.reject(promise) else player.stop(sampleName, promise)
    }

    @ReactMethod
    override fun unloadSample(playerId: Double, sampleName: String, promise: Promise) {
        val player = samplePlayers[playerId]
        if (player == null) Errors.UNKNOWN_PLAYER_ID.reject(promise) else player.unload(sampleName, promise)
    }

    companion object {
      const val NAME = "ReactNativeAudio"
    }
  override fun getName(): String {
    return NAME
  }
}
