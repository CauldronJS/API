package com.cauldronjs.api;

import org.graalvm.polyglot.Value;

@FunctionalInterface
public interface Thenable {
  public void then(Value onResolve, Value onReject);
}