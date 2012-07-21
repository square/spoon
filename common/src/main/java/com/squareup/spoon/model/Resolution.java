package com.squareup.spoon.model;

public class Resolution {
  public int width;
  public int height;

  @Override public String toString() {
    return width + "x" + height;
  }

  public static Resolution parse(String resolution) {
    try {
      String[] parts = resolution.split("x");
      Resolution r = new Resolution();
      r.width = Integer.parseInt(parts[0]);
      r.height = Integer.parseInt(parts[1]);
      return r;
    } catch (Exception e) {
      throw new IllegalArgumentException("Unable to parse resolution.", e);
    }
  }
}
