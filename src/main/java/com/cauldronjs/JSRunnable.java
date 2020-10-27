package com.cauldronjs;

import org.graalvm.polyglot.Value;

public class JSRunnable implements Runnable {
  private Value fn;

  public JSRunnable() {
    //
  }

  JSRunnable(Value fn) {
    this.fn = fn;
  }

  @Override
  public void run() {
    this.fn.executeVoid();
  }

  public JSRunnable create(Value fn) {
    return new JSRunnable(fn);
  }
}