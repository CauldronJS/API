package com.cauldronjs.api;

import org.graalvm.polyglot.Value;

@FunctionalInterface
public interface PromiseExecutor {
  void onPromiseCreated(Value onResolve, Value onReject);
}