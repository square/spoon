package com.squareup.spoon;

/** Simple logger interface. */
final class SpoonLogger {
  static void logInfo(String message, Object... args) {
    System.out.println(String.format(message, args));
  }

  static void logDebug(boolean debug, String message, Object... args) {
    if (debug) System.out.println(String.format(message, args));
  }
}
