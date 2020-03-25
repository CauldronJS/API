package com.cauldronjs.core;

public interface EventEmitter {

  public void on(String key, Runnable callback);

  public void emit(String key);
}