package com.drpogodin.reactnativeaudio

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.Promise

abstract class ReactNativeAudioSpec(context: ReactApplicationContext?) : ReactContextBaseJavaModule(context) {
    abstract override fun getName(): String
    abstract fun configAudioSystem(promise: Promise)
    abstract fun getTypedExportedConstants(): MutableMap<String, Any>
    abstract fun destroySamplePlayer(playerId: Double, promise: Promise)
    override fun getConstants(): Map<String, Any>? {
        return getTypedExportedConstants()
    }

    abstract fun getInputAvailable(promise: Promise)
    abstract fun initSamplePlayer(playerId: Double, promise: Promise)
    abstract fun listen(
            streamId: Double,
            audioSource: Double,
            sampleRate: Double,
            channelConfig: Double,
            audioFormat: Double,
            samplingSize: Double,
            promise: Promise
    )
    abstract fun loadSample(
      playerId: Double,
      sampleName: String,
      samplePath: String,
      promise: Promise
    )
    abstract fun playSample(
      playerId: Double,
      sampleName: String,
      loop: Boolean,
      promise: Promise
    )
    abstract fun muteInputStream(streamId: Double, muted: Boolean)
    abstract fun unlisten(streamId: Double, promise: Promise)
    abstract fun addListener(eventName: String?)
    abstract fun removeListeners(count: Double)
    abstract fun stopSample(playerId: Double, sampleName: String, promise: Promise)
    abstract fun unloadSample(playerId: Double, sampleName: String, promise: Promise)
  }
