package com.drpogodin.reactnativeaudio

import android.util.Log
import com.facebook.react.bridge.Promise

enum class Errors(val message: String) {
    INTERNAL_ERROR("Internal error"),
    OPERATION_FAILED("Operation failed"),
    UNKNOWN_PLAYER_ID("Unknown player ID"),
    UNKNOWN_SAMPLE_NAME("Unknown sample name");

    val error: Error
        get() = Error(message)
    val exception: Exception
        get() = Exception(message)

    fun log(): Errors {
        Log.e(LOG_TAG, message)
        return this
    }

    fun log(e: Exception): Errors {
        Log.e(LOG_TAG, e.toString())
        return this.log()
    }

    fun reject(promise: Promise?) {
        promise?.reject(this.toString(), message, error)
    }

    fun reject(promise: Promise?, details: String?) {
        if (promise != null) {
            var message = message
            if (details != null) message += ": $details"
            promise.reject(this.toString(), message, error)
        }
    }

    override fun toString(): String {
        return "${LOG_TAG}:${name}"
    }

    companion object {
        const val LOG_TAG = "RN_AUDIO"
    }
}
