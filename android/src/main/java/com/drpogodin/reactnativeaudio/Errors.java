package com.drpogodin.reactnativeaudio;

import android.util.Log;
import com.facebook.react.bridge.Promise;

public enum Errors {
  INTERNAL_ERROR("Internal error"),
  NOT_IMPLEMENTED("Not implemented"),
  OPERATION_FAILED("Operation failed"),
  UNKNOWN_PLAYER_ID("Unknown player ID"),
  UNKNOWN_SAMPLE_NAME("Unknown sample name");

  private String message;
  public static final String LOGTAG = "RN_AUDIO";

  Errors(String message) {
    this.message = message;
  }

  public Error getError() {
    return new Error(this.getMessage());
  }

  public Exception getException() {
    return new Exception(this.getMessage());
  }

  public String getMessage() {
    return this.message;
  }

  public Errors log() {
    Log.e(Errors.LOGTAG, this.getMessage());
    return this;
  }

  public Errors log(Exception e) {
    Log.e(Errors.LOGTAG, e.toString());
    return this.log();
  }

  public void reject(Promise promise) {
    if (promise != null) {
      promise.reject(this.toString(), this.getMessage(), this.getError());
    }
  }

  public void reject(Promise promise, String details) {
    if (promise != null) {
      String message = this.getMessage();
      if (details != null) message += ": " + details;
      promise.reject(this.toString(), message, this.getError());
    }
  }

  public String toString() {
    return Errors.LOGTAG + ":" + this.name();
  }
}
