package com.squareup.spoon.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.builder.ToStringBuilder;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

public class Device {
  public String api;
  public Resolution resolution;
  public Density density;
  public String locale;
  public Orientation orientation;
  public String serial;

  public Device() {}

  /* TODO use for cloning from defaults
  public Device(Device other) {
    api = other.api;
    resolution = other.resolution;
    density = other.density;
    locale = other.locale;
    orientation = other.orientation;
    serial = other.serial;
  }
  */

  @Override public String toString() {
    return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
  }

  public String id() {
    if (serial != null) {
      return serial;
    }
    // TODO unique, human-readable identifier for device (possibly joining config options with '-')
    throw new IllegalStateException("Emulators are not yet supported.");
  }

  @JsonIgnore public boolean isEmulator() {
    return serial == null;
  }
}
