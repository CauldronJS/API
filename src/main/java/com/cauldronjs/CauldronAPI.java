package com.cauldronjs;

import java.io.File;
import java.io.InputStream;
import java.util.logging.Level;

import com.cauldronjs.isolate.Isolate;
import com.cauldronjs.isolate.IsolateManager;
import org.graalvm.polyglot.Value;

public interface CauldronAPI {
  IsolateManager getIsolateManager();

  Isolate getMainIsolate();

  TargetDescriptor getTarget();

  InputStream getResource(String name);

  void log(Level level, String content);

  boolean isDebugging();

  boolean isRunning();

  int scheduleRepeatingTask(Value fn, int interval, int timeout);

  int scheduleTask(Value fn, int timeout);

  int scheduleRepeatingTask(Runnable runnable, int interval, int timeout);

  int scheduleTask(Runnable runnable, int timeout);

  boolean cancelTask(int id);

  public File getDefaultCwd();
}