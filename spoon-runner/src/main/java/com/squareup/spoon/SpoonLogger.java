package com.squareup.spoon;

/** Simple logger interface. */
public interface SpoonLogger {
  void info(String message, Object... args);

  void fine(String message, Object... args);
}
