package com.squareup.spoon;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Simple logger interface. */
final class SpoonLogger {
  private static final ThreadLocal<DateFormat> DATE_FORMAT = new ThreadLocal<DateFormat>() {
    @Override protected DateFormat initialValue() {
      return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }
  };

  static void logError(String message, Object... args) {
    System.err.println(getPrefix() + String.format(message, args));
  }

  static void logInfo(String message, Object... args) {
    System.out.println(getPrefix() + String.format(message, args));
  }

  static void logDebug(boolean debug, String message, Object... args) {
    if (debug) System.out.println(getPrefix() + String.format(message, args));
  }

  private static String getPrefix() {
    StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
    if (stackTrace == null || stackTrace.length < 4) return "[BOGUS]";
    String className = stackTrace[3].getClassName();
    String methodName = stackTrace[3].getMethodName();
    className = className.replaceAll("[a-z\\.]", "");
    String timestamp = DATE_FORMAT.get().format(new Date());
    return String.format("%s [%s.%s] ", timestamp, className, methodName);
  }
}
