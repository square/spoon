package com.squareup.spoon.model;

/** Device display shape. */
public enum ScreenLong {
  Long("long"), NotLong("notlong");

  private final String value;
  private ScreenLong(String value) {
    this.value = value;
  }

  @Override public String toString() {
    return value;
  }

  public static ScreenLong get(String value) {
    for (ScreenLong orientation : values()) {
      if (orientation.value.equalsIgnoreCase(value)) {
        return orientation;
      }
    }
    throw new IllegalArgumentException();
  }
}