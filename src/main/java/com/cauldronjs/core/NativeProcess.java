package com.cauldronjs.core;

import java.util.ArrayList;
import java.util.HashMap;

import com.cauldronjs.Isolate;
import com.cauldronjs.api.BoundType;

import org.graalvm.polyglot.HostAccess.Export;

public class NativeProcess implements EventEmitter {
  private final Isolate isolate;
  private final HashMap<String, ArrayList<Runnable>> handlers;

  protected NativeProcess(Isolate isolate) {
    this.isolate = isolate;
    this.handlers = new HashMap<>();
  }

  private void createHandlers() {
    //
  }

  public void chdir(String dir) {
    // TODO expose directory API to public access
  }

  public String cwd() {
    return this.isolate.cwd().getAbsolutePath();
  }

  @Export
  public static NativeProcess build(Isolate isolate) {
    NativeProcess proc = new NativeProcess(isolate);

    return proc;
  }

  @Override
  public void on(String key, Runnable callback) {
    if (!this.handlers.containsKey(key)) {
      this.handlers.put(key, new ArrayList<>());
    }
    this.handlers.get(key).add(callback);
  }

  @Override
  public void emit(String key) {
    if (!this.handlers.containsKey(key)) {
      return;
    }
    for (Runnable callback : this.handlers.get(key)) {
      callback.run();
      // TODO: consider allowing these to run async
    }
  }
}