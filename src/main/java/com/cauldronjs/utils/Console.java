package com.cauldronjs.utils;

import java.util.logging.Level;

import com.cauldronjs.CauldronAPI;

public class Console {
  private final CauldronAPI cauldron;

  public Console(CauldronAPI cauldron) {
    this.cauldron = cauldron;
  }

  public void clear() {

  }

  public void count() {
    this.count("default");
  }

  public void count(String label) {

  }

  public void countReset() {
    this.countReset("default");
  }

  public void countReset(String label) {

  }

  public void debug(Object data, Object...args) {

  }

  public void dir(Object obj, Object... options) {

  }

  public void error(Object data, Object... args) {

  }

  public void group(String... label) {

  }

  public void groupCollapsed() {

  }

  public void groupEnd() {

  }

  public void info(Object data, Object... args) {

  }

  public void log(Object data, Object... args) {

  }

  public void table(Object tabularData, Object... properties) {

  }

  public void time() {
    this.time("default");
  }

  public void time(String label) {

  }

  public void timeEnd() {
    this.timeEnd("default");
  }

  public void timeEnd(String label) {

  }

  public void timeLog() {
    this.timeLog("default");
  }

  public void timeLog(String label, Object... data) {

  }

  public void trace(Object message, Object...args) {

  }

  public void warn(Object data, Object... args) {

  }

  private static final String coloredText = "\u001B[";

  private static String getColorFor(Level level) {
    if (level == Level.INFO) {
      return coloredText + "0m";
    } else if (level == Level.WARNING) {
      return coloredText + "33m";
    } else if (level == Level.SEVERE) {
      return coloredText + "31m";
    } else if (level == Level.FINE) {
      return coloredText + "35m";
    } else if (level == Level.FINER) {
      return coloredText + "36m";
    } else if (level == Level.FINEST) {
      return coloredText + "36m";
    } else {
      return coloredText + "0m";
    }
  }

  private static void l(CauldronAPI cauldron, Level level, Object... content) {
    for (Object item : content) {
      if (item.getClass().isArray()) {
        StringBuilder result = new StringBuilder();
        Object[] arr = (Object[]) item;
        for (Object obj : arr) {
          result.append(System.lineSeparator()).append("\t").append(obj.toString());
        }
        cauldron.log(level, result.toString());
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