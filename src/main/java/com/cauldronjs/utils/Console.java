package com.cauldronjs.utils;

import java.util.logging.Level;

import com.cauldronjs.CauldronAPI;
import com.cauldronjs.Isolate;

public class Console {

  private static void l(CauldronAPI cauldron, Level level, Object... content) {
    for (Object item : content) {
      if (item.getClass().isArray()) {
        String result = "";
        Object[] arr = (Object[]) item;
        for (Object obj : arr) {
          result += System.lineSeparator() + "\t" + obj.toString();
        }
        cauldron.log(level, result);
      } else {
        cauldron.log(level, item.toString());
      }
    }
  }

  public static void log(CauldronAPI cauldron, Object... contents) {
    l(cauldron, Level.INFO, contents);
  }

  public static void error(CauldronAPI cauldron, Object... contents) {
    l(cauldron, Level.SEVERE, contents);
  }

  public static void debug(CauldronAPI cauldron, Object... contents) {
    if (cauldron.isDebugging()) {
      l(cauldron, Level.FINE, contents);
    }
  }

  public static void warn(CauldronAPI cauldron, Object... contents) {
    l(cauldron, Level.WARNING, contents);
  }
}