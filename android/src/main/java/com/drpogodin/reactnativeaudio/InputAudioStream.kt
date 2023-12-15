package com.drpogodin.reactnativeaudio

import android.media.AudioFormat
import android.media.AudioRecord

/**
 * Represents an input audio stream.
 */
class InputAudioStream internal constructor(audioSource: Int, sampleRate: Int, channelConfig: Int, audioFormat: Int,
                                            samplingSize: Int, listener: Listener) {
    /**
     * Interface definition for an audio chunk listener.
     */
    interface Listener {
        /**
         * Called when a new audio data chunk is ready.
         * @param chunkId Consequent chunk number (the count includes chunks which were not delivered
         * because of the stream being muted).
         * @param chunk
         */
        fun onChunk(chunkId: Int, chunk: ByteArray?)

        /**
         * Called when any error happens during the stream initialization or lifetime.
         * @param e
         */
        fun onError(e: Exception?)
    }

    /**
     * This flag governs whether current audio chunks are sent to the listener (if false),
     * or discarded silently (if true).
     */
    var muted = false

    /**
     * The execution thread.
     */
    private val thread: Thread

    /**
     * Creates and starts a new AudioStream.
     * @param audioSource Audio source. Valid values are:
     * https://developer.android.com/reference/android/media/MediaRecorder.AudioSource#summary
     * @param sampleRate Sample rate [Hz]. 44100 Hz is currently the only rate that is guaranteed
     * to work on all devices. Zero value means the default rate, which is usually
     * the audio source sample rate.
     * @param channelConfig Valid values are:
     * - AudioFormat.CHANNEL_IN_MONO
     * - AudioFormat.CHANNEL_IN_STEREO
     * @param audioFormat Valid values are:
     * - AudioFormat.ENCODING_PCM_8BIT
     * - AudioFormat.ENCODING_PCM_16BIT
     * - AudioFormat.ENCODING_PCM_FLOAT
     * @param samplingSize Number of samples in chunk (per channel).
     * @param listener Chunk listener.
     */
    init {
        thread = object : Thread() {
            override fun run() {
                var record: AudioRecord? = null
                try {
                    // Initialization.
                    val frameSize = getNumChannels(channelConfig) * getSampleSize(audioFormat) // bytes
                    val chunkSize = frameSize * samplingSize // bytes
                    val chunk = ByteArray(chunkSize)
                    val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
                    val bufferSize = Math.max(3 * chunkSize, minBufferSize) // bytes
                    record = AudioRecord(audioSource, sampleRate, channelConfig, audioFormat,
                            bufferSize)
                    if (record.state != AudioRecord.STATE_INITIALIZED) {
                        throw Exception("Failed to start audio recording")
                    }
                    record.startRecording()

                    // Main lifetime.
                    var chunkId = 0
                    while (!isInterrupted) {
                        record.read(chunk, 0, chunkSize)
                        if (!muted) listener.onChunk(chunkId, chunk)
                        ++chunkId
                    }

                    // De-initialization.
                    record.stop()
                } catch (e: SecurityException) {
                    listener.onError(e)
                } catch (e: Exception) {
                    listener.onError(e)
                }
                record?.release()
            }
        }
        thread.start()
    }

    /**
     * Stops the stream, and releases all related resources.
     */
    fun stop() {
        thread.interrupt()
    }

    companion object {
        /**
         * Returns the number of channels corresponding the given config.
         * @param channelConfig
         * @return Number of channels.
         * @throws Exception
         */
        @Throws(Exception::class)
        private fun getNumChannels(channelConfig: Int): Int {
            return when (channelConfig) {
                AudioFormat.CHANNEL_IN_MONO -> 1
                AudioFormat.CHANNEL_IN_STEREO -> 2
                else -> throw Exception("Invalid channel config")
            }
        }

        /**
         * Returns sample size [bytes] for the given audio format.
         * @param audioFormat
         * @return Sample size [bytes].
         * @throws Exception
         */
        @Throws(Exception::class)
        private fun getSampleSize(audioFormat: Int): Int {
            return when (audioFormat) {
                AudioFormat.ENCODING_PCM_8BIT -> 1
                AudioFormat.ENCODING_PCM_16BIT -> 2
                AudioFormat.ENCODING_PCM_FLOAT -> 4
                else -> throw Exception("Invalid audio format")
            }
        }
    }
}
