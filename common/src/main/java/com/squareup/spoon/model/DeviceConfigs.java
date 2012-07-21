package com.squareup.spoon.model;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

/** Collection of different device configurations. This class is used for YML deserialization. */
public class DeviceConfigs {
  /** Defaults which are applied to all created emulators. */
  public Device defaults;
  /** List of pre-configured devices to create emulators for. */
  public List<String> devices;
  /** Custom device configurations. This includes both emulators and physical devices by serial number. */
  public Map<String, Device> custom;
  /** All physically present devices. */
  public boolean all;

  @Override public String toString() {
    return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
  }
}
