package com.cauldronjs.exceptions;

import com.cauldronjs.isolate.Isolate;

public class IsolateException extends Exception {

  /**
   *
   */
  private static final long serialVersionUID = 1599937854838882129L;

  private final Isolate isolate;

  public IsolateException(Isolate isolate, String message) {
    super(message);
    this.isolate = isolate;
  }

  public Isolate getIsolate() {
    return this.isolate;
  }
}