package com.squareup.spoon.model;

/** Device display size. */
public enum ScreenSize {
  Small("small"), Normal("normal"), Large("large"), ExtraLarge("xlarge");

  private final String value;
  private ScreenSize(String value) {
    this.value = value;
  }

  @Override public String toString() {
    return value;
  }

  public static ScreenSize get(String value) {
    for (ScreenSize orientation : values()) {
      if (orientation.value.equalsIgnoreCase(value)) {
        return orientation;
      }
    }
    throw new IllegalArgumentException();
  }
}