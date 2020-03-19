package com.cauldronjs.core;

import org.graalvm.polyglot.Value;

public class JsRunnable implements Runnable {
  private Value fn;

  public JsRunnable() {
    //
  }

  JsRunnable(Value fn) {
    this.fn = fn;
  }

  @Override
  public void run() {
    this.fn.executeVoid();
  }

  public JsRunnable create(Value fn) {
    return new JsRunnable(fn);
  }
}