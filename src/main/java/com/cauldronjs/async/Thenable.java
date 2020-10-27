package com.cauldronjs.async;

import org.graalvm.polyglot.Value;

@FunctionalInterface
public interface Thenable {
  void then(Value onResolve, Value onReject);
}