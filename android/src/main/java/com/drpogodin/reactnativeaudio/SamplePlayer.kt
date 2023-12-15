package com.drpogodin.reactnativeaudio

import android.media.SoundPool
import com.facebook.react.bridge.Promise

class SamplePlayer internal constructor() : SoundPool.OnLoadCompleteListener {
    private val loadPromises = HashMap<Int, Promise>()
    private val pool: SoundPool
    private val soundIds = HashMap<String, Int>()
    private val streamIds = HashMap<String, Int>()

    init {
        pool = builder!!.build()
        pool.setOnLoadCompleteListener(this)
    }

    fun destroy() {
        pool.release()
    }

    fun load(name: String, path: String?, promise: Promise) {
        var soundId = soundIds[name]
        if (soundId != null) pool.unload(soundId)
        soundId = pool.load(path, 1)
        loadPromises[soundId] = promise
        soundIds[name] = soundId
    }

    override fun onLoadComplete(soundPool: SoundPool, sampleId: Int, status: Int) {
        if (soundPool !== pool) return
        val promise = loadPromises.remove(sampleId)
        if (promise != null) {
            if (status == 0) promise.resolve(null) else Errors.OPERATION_FAILED.reject(promise, "Status: $status")
        }
    }

    fun play(sampleName: String, loop: Boolean, promise: Promise) {
        val soundId = soundIds[sampleName]
        if (soundId == null) {
            Errors.UNKNOWN_SAMPLE_NAME.reject(promise, sampleName)
            return
        }
        var streamId = streamIds.remove(sampleName)
        if (streamId != null) pool.stop(streamId)
        streamId = pool.play(soundId, 1f, 1f, 1, if (loop) 1 else 0, 1f)
        if (streamId == 0) Errors.OPERATION_FAILED.reject(promise) else streamIds[sampleName] = streamId
        promise.resolve(null)
    }

    fun stop(sampleName: String, promise: Promise) {
        val streamId = streamIds.remove(sampleName)
        if (streamId != null) pool.stop(streamId)
        promise.resolve(null)
    }

    fun unload(name: String, promise: Promise) {
        val soundId = soundIds.remove(name)
        if (soundId == null) {
            Errors.UNKNOWN_SAMPLE_NAME.reject(promise, name)
            return
        }
        val res = pool.unload(soundId)
        if (res) promise.resolve(null) else Errors.OPERATION_FAILED.reject(promise, "Sample is not loaded")
    }

    companion object {
        private var builder: SoundPool.Builder? = null

        init {
            builder = SoundPool.Builder()
            builder!!.setMaxStreams(4)
        }
    }
}
