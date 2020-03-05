package com.cauldronjs;

import java.io.File;
import java.io.InputStream;
import java.util.logging.Level;

public interface CauldronAPI {
  public Isolate getMainIsolate();

  public TargetDescriptor getTarget();

  public InputStream getResource(String name);

  public void log(Level level, String content);

  public boolean isDebugging();

  public int scheduleRepeatingTask(Runnable runnable, int interval, int timeout);

  public int scheduleTask(Runnable runnable, int timeout);

  public boolean cancelTask(int id);

  public File getDefaultCwd();
}