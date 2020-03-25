package com.cauldronjs.api;

import java.lang.reflect.Constructor;

import com.cauldronjs.Isolate;

import org.graalvm.polyglot.Value;

public class BoundType<T extends BoundType<T>> {
  private Isolate isolate;

  public BoundType(Isolate isolate) {
    super();
    this.isolate = isolate;
  }

  public Isolate getIsolate() {
    return this.isolate;
  }
}