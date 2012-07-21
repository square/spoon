package com.squareup.spoon.model;

/** Device display density. */
public enum Density {
  Low("ldpi"), Medium("mdpi"), High("hdpi"), ExtraHigh("xhdpi"), TV("tvdpi"), ExtraExtraHigh("xxhdpi");

  private final String value;
  private Density(String value) {
    this.value = value;
  }

  @Override public String toString() {
    return value;
  }

  public static Density get(String value) {
    for (Density orientation : values()) {
      if (orientation.value.equalsIgnoreCase(value)) {
        return orientation;
      }
    }
    throw new IllegalArgumentException();
  }
}