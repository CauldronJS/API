package com.cauldronjs.utils;

import com.cauldronjs.Isolate;

import org.graalvm.polyglot.Value;

public class JsUtils {
  public static Value createPromise(Isolate isolate) {
    return isolate.getContext().getBindings("js").getMember("Promise");
  }
}