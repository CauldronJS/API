package com.cauldronjs.exceptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.stream.Stream;

import com.cauldronjs.CauldronAPI;
import com.cauldronjs.sourceMap.MappingEntry;
import com.cauldronjs.sourceMap.SourceMap;
import com.cauldronjs.sourceMap.SourceMapParser;
import com.cauldronjs.sourceMap.SourcePosition;

public class JSException extends Exception {
  /**
   *
   */
  private static final long serialVersionUID = 1599937854838882128L;

  public JSException(CauldronAPI cauldron, Throwable throwable) {
    super("[" + throwable.getClass().getName() + "]: " + throwable.getMessage());
    boolean isDebugging = cauldron.isDebugging();
    Stream<StackTraceElement> stackTrace = Stream.of(throwable.getStackTrace());
    StackTraceElement[] cleanedTrace = stackTrace
            .filter(stackTraceElement ->
              isDebugging || !stackTraceElement.toString().contains("lib/internal/modules/loader.js")
            ).toArray(StackTraceElement[]::new);
    this.setStackTrace(cleanedTrace);
  }
}