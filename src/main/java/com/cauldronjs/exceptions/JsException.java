package com.cauldronjs.exceptions;

import java.util.stream.Stream;

import com.cauldronjs.CauldronAPI;
import com.cauldronjs.Isolate;

public class JsException extends Exception {
  /**
   *
   */
  private static final long serialVersionUID = 1599937854838882128L;

  public JsException(CauldronAPI cauldron, Throwable throwable) {
    super("[" + throwable.getClass().getName() + "]: " + throwable.getMessage());
    boolean isDebugging = cauldron.isDebugging();
    Stream<StackTraceElement> stackTrace = Stream.of(throwable.getStackTrace());
    StackTraceElement[] cleanedTrace = stackTrace.filter(stackTraceElement -> {
      return isDebugging ? true : !stackTraceElement.toString().contains("lib/internal/modules/loader.js");
    }).toArray(StackTraceElement[]::new);
    this.setStackTrace(cleanedTrace);
  }
}