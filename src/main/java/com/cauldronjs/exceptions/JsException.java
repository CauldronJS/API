package com.cauldronjs.exceptions;

import java.util.stream.Stream;

import com.cauldronjs.Isolate;

public class JsException extends Exception {
  /**
   *
   */
  private static final long serialVersionUID = 1599937854838882128L;

  public JsException(Throwable throwable) {
    super("[" + throwable.getClass().getName() + "]: " + throwable.getMessage());
    boolean isDebugging = Isolate.activeIsolate().cauldron().isDebugging();
    Stream<StackTraceElement> stackTrace = Stream.of(throwable.getStackTrace());
    StackTraceElement[] cleanedTrace = stackTrace.filter(stackTraceElement -> {
      return isDebugging ? true : !stackTraceElement.toString().contains("lib/internal/modules/loader.js");
    }).toArray(StackTraceElement[]::new);
    this.setStackTrace(cleanedTrace);
  }
}