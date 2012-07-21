package com.squareup.spoon.model;

public enum Orientation {
  Portrait("port"), Landscape("land"), Square("square");

  private final String value;

  private Orientation(String value) {
    this.value = value;
  }

  @Override public String toString() {
    return value;
  }

  public static Orientation get(String value) {
    for (Orientation orientation : values()) {
      if (orientation.value.equalsIgnoreCase(value)) {
        return orientation;
      }
    }
    throw new IllegalArgumentException();
  }
}