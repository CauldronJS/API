package com.cauldronjs.core;

import com.cauldronjs.Isolate;

public class NativeProcess {
  private Isolate isolate;

  public NativeProcess(Isolate isolate) {
    this.isolate = isolate;
  }

  public void chdir(String dir) {
    // TODO expose directory API to public access
  }

  public String cwd() {
    return this.isolate.cwd().getAbsolutePath();
  }
}