package me.conji.cauldron.utils;

import java.util.logging.Level;

import me.conji.cauldron.CauldronAPI;
import me.conji.cauldron.Isolate;

public class Console {
  private static CauldronAPI cauldron() {
    return Isolate.activeIsolate().cauldron();
  }

  private static void l(Level level, Object... content) {
    for (Object item : content) {
      if (item.getClass().isArray()) {
        String result = "";
        Object[] arr = (Object[]) item;
        for (Object obj : arr) {
          result += System.lineSeparator() + "\t" + obj.toString();
        }
        cauldron().log(level, result);
      } else {
        cauldron().log(level, item.toString());
      }
    }
  }

  public static void log(Object... contents) {
    l(Level.INFO, contents);
  }

  public static void error(Object... contents) {
    l(Level.SEVERE, contents);
  }

  public static void debug(Object... contents) {
    if (cauldron().isDebugging()) {
      l(Level.FINE, contents);
    }
  }

  public static void warn(Object... contents) {
    l(Level.WARNING, contents);
  }
}