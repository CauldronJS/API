package com.cauldronjs.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Level;

import com.cauldronjs.CauldronAPI;
import com.cauldronjs.isolate.Isolate;

import com.cauldronjs.sourceMap.MappingEntry;
import com.cauldronjs.sourceMap.SourceMap;
import com.cauldronjs.sourceMap.SourceMapParser;
import com.cauldronjs.sourceMap.SourcePosition;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.HostAccess.Export;

public class JsUtils {
  @Export
  public static Value createPromise(Isolate isolate) {
    return isolate.getContext().getBindings("js").getMember("Promise");
  }

  @Export
  public static byte[] toByteArray(String data) {
    return data.getBytes();
  }

  @Export
  public static String stringFromByteArray(byte[] data, String encoding) {
    return new String(data, Charset.forName(encoding));
  }

  public static StackTraceElement[] applySourceMapToStack(Isolate isolate, StackTraceElement[] stackTrace) {
    return Arrays.stream(stackTrace).map(stackTraceElement -> {
      if (stackTraceElement == null) {
        return null;
      }
      String filename = stackTraceElement.getFileName();
      if (filename == null) {
        // if this happens, we can't parse anyways
        return stackTraceElement;
      }
      if (!filename.endsWith(".js")) {
        return stackTraceElement;
      }
      File mapFile = Paths.get(isolate.cwd().toString(), stackTraceElement.getFileName() + ".map").toFile();
      if (mapFile.exists()) {
        try {
          SourceMapParser parser = new SourceMapParser(isolate.getContext());
          SourceMap map = parser.parseSourceMap(new FileInputStream(mapFile));
          // TODO: get the column number here, Java doesn't support it
          SourcePosition position = new SourcePosition(stackTraceElement.getLineNumber(), 0);
          MappingEntry mappingEntry = map.getEntryFromGeneratedPosition(position);
          return new StackTraceElement(
                  stackTraceElement.getClassName(),
                  mappingEntry.getOriginalName(),
                  mappingEntry.getOriginalFilename(),
                  mappingEntry.getOriginalSourcePosition().getLineNumber()
          );
        } catch (FileNotFoundException ex) {
          // this shouldn't happen, fail gracefully
        } catch (SecurityException | IOException ex) {
          isolate.cauldron().log(Level.WARNING, "Failed to open map file for " + mapFile.getAbsolutePath());
        }
      }
      return stackTraceElement;
    }).toArray(StackTraceElement[]::new);
  }
}