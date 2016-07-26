package com.squareup.spoon;

final class DeviceUtils {
  /** Scrub the model so that it does not contain redundant data. */
  static String scrubModel(String manufacturer, String model) {
    if (manufacturer == null || model == null) {
      return model;
    }
    if (model.regionMatches(true, 0, manufacturer, 0, manufacturer.length())) {
      model = model.substring(manufacturer.length());
    }
    if (model.length() > 0 && (model.charAt(0) == ' ' || model.charAt(0) == '-')) {
      model = model.substring(1);
    }
    return model;
  }

  /** Scrub the language so it does not contain bad data. */
  static String scrubLanguage(String language) {
    if ("ldpi".equals(language)
        || "mdpi".equals(language)
        || "hdpi".equals(language)
        || "xhdpi".equals(language)) {
      return null; // HTC, you suck!
    }
    return language;
  }
}
