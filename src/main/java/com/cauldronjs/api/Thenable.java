package com.cauldronjs.api;

import org.graalvm.polyglot.Value;

public interface Thenable {
  void then(Value onResolve, Value onReject);
}