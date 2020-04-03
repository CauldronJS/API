package com.cauldronjs.utils;

import java.nio.charset.Charset;
import java.util.Arrays;

import com.cauldronjs.Isolate;

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
}