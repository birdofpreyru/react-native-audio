package com.drpogodin.reactnativeaudio

import android.util.Log
import com.facebook.react.bridge.Promise

enum class Errors(val message: String) {
    INTERNAL_ERROR("Internal error"),
    NOT_IMPLEMENTED("Not implemented"),
    OPERATION_FAILED("Operation failed"),
    UNKNOWN_PLAYER_ID("Unknown player ID"),
    UNKNOWN_SAMPLE_NAME("Unknown sample name");

    val error: Error
        get() = Error(message)
    val exception: Exception
        get() = Exception(message)

    fun log(): Errors {
        Log.e(LOGTAG, message)
        return this
    }

    fun log(e: Exception): Errors {
        Log.e(LOGTAG, e.toString())
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
        return "${LOGTAG}:${name}"
    }

    companion object {
        const val LOGTAG = "RN_AUDIO"
    }
}
