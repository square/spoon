package com.squareup.spoon;

import com.android.ddmlib.IDevice;
import org.apache.commons.lang3.builder.ToStringBuilder;

/** Device configuration and hardware information. */
public final class DeviceDetails {
  private final String name;
  private final String manufacturer;
  private final String version;
  private final int apiLevel;
  private final String language;
  private final String region;

  private DeviceDetails(String name, String manufacturer, String version, int apiLevel,
      String language, String region) {
    this.name = name;
    this.manufacturer = manufacturer;
    this.version = version;
    this.apiLevel = apiLevel;
    this.language = language;
    this.region = region;
  }

  /** Product model. */
  public String getName() {
    return name;
  }

  /** Produce manufacturer. */
  public String getManufacturer() {
    return manufacturer;
  }

  /** Android version. */
  public String getVersion() {
    return version;
  }

  /** Android API level. */
  public int getApiLevel() {
    return apiLevel;
  }

  /** Device language. */
  public String getLanguage() {
    return language;
  }

  /** Device region. */
  public String getRegion() {
    return region;
  }

  static DeviceDetails createForDevice(IDevice device) {
    String name = device.getProperty("ro.product.model");
    String manufacturer = device.getProperty("ro.product.manufacturer");
    String version = device.getProperty("ro.build.version.release");
    String api = device.getProperty("ro.build.version.sdk");
    int apiLevel = api != null ? Integer.parseInt(api) : 0;
    String language = device.getProperty("ro.product.locale.language");
    String region = device.getProperty("ro.product.locale.region");

    return new DeviceDetails(name, manufacturer, version, apiLevel, language, region);
  }

  @Override public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }
}
