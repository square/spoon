package com.squareup.spoon;

import com.android.ddmlib.IDevice;
import org.apache.commons.lang3.builder.ToStringBuilder;

import static com.google.common.base.Strings.emptyToNull;

/** Device configuration and hardware information. */
public final class DeviceDetails {
  private final String model;
  private final String manufacturer;
  private final String version;
  private final int apiLevel;
  private final String language;
  private final String region;

  private DeviceDetails(String model, String manufacturer, String version, int apiLevel,
      String language, String region) {
    this.model = model;
    this.manufacturer = manufacturer;
    this.version = version;
    this.apiLevel = apiLevel;
    this.language = language;
    this.region = region;
  }

  /** Product manufacturer and model. */
  public String getName() {
    return manufacturer + " " + model;
  }

  /** Product model. */
  public String getModel() {
    return model;
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
    String manufacturer = emptyToNull(device.getProperty("ro.product.manufacturer"));
    String model = emptyToNull(device.getProperty("ro.product.model"));
    model = DeviceUtils.scrubModel(manufacturer, model);

    String version = emptyToNull(device.getProperty("ro.build.version.release"));
    String api = emptyToNull(device.getProperty("ro.build.version.sdk"));
    int apiLevel = api != null ? Integer.parseInt(api) : 0;

    String language = emptyToNull(device.getProperty("ro.product.locale.language"));
    language = DeviceUtils.scrubLanguage(language);

    String region = emptyToNull(device.getProperty("ro.product.locale.region"));

    return new DeviceDetails(model, manufacturer, version, apiLevel, language, region);
  }

  @Override public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }
}
