package com.squareup.spoon.template;

import java.util.Arrays;

public class StackTrace {
  public final String title;
  public final String[] body;

  public StackTrace(String title, String[] body) {
    this.title = title;
    this.body = body;
  }

  public static StackTrace fromString(String trace) {
    if (trace == null) {
      return null;
    }

    String[] lines = trace.replaceAll("\r\n", "\n").split("\n");

    String title = lines[0].trim();
    String[] body = Arrays.copyOfRange(lines, 1, lines.length);

    return new StackTrace(title, body);
  }
}
