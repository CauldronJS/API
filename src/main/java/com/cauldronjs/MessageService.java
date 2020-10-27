package com.cauldronjs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;

import org.graalvm.polyglot.Value;

public class MessageService {
  // this class is in charge of keeping a separate bridge between
  // isolates and allowing messaging between them asynchronously
  private static final HashMap<String, ArrayList<Consumer<Value>>> handlers = new HashMap<>();
  private static Thread runner;

  private static void initialize() {
    runner = new Thread(() -> {
      while (true) {
        synchronized (handlers) {

        }
      }
    });
  }

  public static synchronized void subscribe(String name, Consumer<Value> handler) {
    if (runner == null) {
      initialize();
    }
    if (!handlers.containsKey(name)) {
      handlers.put(name, new ArrayList<>());
    }

    handlers.get(name).add(handler);
  }

  public static synchronized void publish(String name, Value arg) {
    if (handlers.containsKey(name)) {
      handlers.get(name).forEach(handler -> {
        handler.accept(arg);
      });
    }
  }

  public static class MessagingSubscription {
    private int id;
  }
}