package com.squareup.spoon;

/** Simple logger interface. */
public class SpoonLogger {
  public static void logInfo(String message, Object... args) {
    System.out.println(String.format(message, args));
  }

  public static void logDebug(boolean debug, String message, Object... args) {
    if (debug) System.out.println(String.format(message, args));
  }
}
