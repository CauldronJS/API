package com.cauldronjs.isolate;

import com.cauldronjs.CauldronAPI;
import com.cauldronjs.isolate.Isolate;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;

public class IsolateManager {
  private final HashMap<UUID, Isolate> isolates;
  private final HashMap<UUID, Integer> inactiveTimes;
  private final CauldronAPI cauldron;
  private Isolate primaryIsolate;

  public IsolateManager(CauldronAPI cauldron) {
    this.cauldron = cauldron;
    this.isolates = new HashMap<>();
    this.inactiveTimes = new HashMap<>();
  }

  public Isolate initialize() {
    Isolate isolate = new Isolate(this.cauldron);
    this.registerIsolate(isolate);
    Thread runner = new Thread(this.watchIsolates());
    runner.start();
    return isolate;
  }

  public boolean isPrimaryIsolate(Isolate isolate) {
    return isolate.equals(this.primaryIsolate);
  }

  public synchronized void activateIsolate(Isolate isolate) {
    try {
      if (isolate.activate()) {
        // move off of warm/cold collection
        this.inactiveTimes.remove(isolate.uuid());
        if (!this.isolates.containsKey(isolate.uuid())) {
          this.isolates.put(isolate.uuid(), isolate);
        }
      }
    } catch (IOException ex) {
      // the isolate is invalid. Should dispose and remove
    }
  }

  public synchronized void pauseIsolate(Isolate isolate) {
    // once the work is done on it, put it on the warm stack
    isolate.pause();
    this.inactiveTimes.put(isolate.uuid(), 0);
  }

  public synchronized void closeIsolate(Isolate isolate) {
    // once the work is done, dispose of it
    isolate.dispose();
    this.unregisterIsolate(isolate);
  }

  public synchronized Isolate getWarmIsolate() {
    if (this.inactiveTimes.size() == 0)
      return this.getColdIsolate();
    UUID uuid = this.inactiveTimes.keySet().iterator().next();
    Isolate isolate = this.isolates.get(uuid);
    this.activateIsolate(isolate);
    return isolate;
  }

  public synchronized Isolate getColdIsolate() {
    // creates a new isolate
    Isolate isolate = new Isolate(this.cauldron);
    this.activateIsolate(isolate);
    return isolate;
  }

  private Runnable watchIsolates() {
    return () -> {
      while (this.cauldron.isRunning()) {
        this.inactiveTimes.forEach((uuid, time) -> {
          if (this.isolates.containsKey(uuid)) {
            Isolate isolate = this.isolates.get(uuid);
            // each isolate keeps its state for 60 seconds
            if (time > 1000 * 60) {
              isolate.dispose();
              this.isolates.remove(uuid);
              this.inactiveTimes.remove(uuid);
            } else {
              time += 1000;
            }
          } else {
            this.inactiveTimes.remove(uuid);
          }
        });
        try {
          Thread.sleep(1000);
        } catch (InterruptedException ex) {
          this.cauldron.log(Level.WARNING, "Failed to sleep watcher thread");
        }
      }
    };
  }

  private void registerIsolate(Isolate isolate) {
    this.isolates.put(isolate.uuid(), isolate);
    if (this.primaryIsolate == null) {
      this.primaryIsolate = isolate;
    }
  }

  private void unregisterIsolate(Isolate isolate) {
    this.isolates.remove(isolate.uuid());
    this.inactiveTimes.remove(isolate.uuid());
  }
}