package com.squareup.spoon.model;

import org.apache.commons.lang3.builder.EqualsBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Device display resolution. */
public class Resolution {
  private static final Pattern CUSTOM = Pattern.compile("(\\d{3,})x(\\d{3,})");
  public static final Resolution HVGA = new Resolution("HVGA", 320, 480);
  public static final Resolution QVGA = new Resolution("QVGA", 240, 320);
  public static final Resolution WQVGA_400 = new Resolution("WQVGA400", 240, 400);
  public static final Resolution WQVGA_432 = new Resolution("WQVGA432", 240, 432);
  public static final Resolution WSVGA = new Resolution("WSVGA", 1076, 654);
  public static final Resolution WVGA_800 = new Resolution("WVGA800", 480, 800);
  public static final Resolution WVGA_850 = new Resolution("WVGA850", 480, 854);
  public static final Resolution WXGA_720 = new Resolution("WXGA720", 773, 1334);
  public static final Resolution WXGA_800 = new Resolution("WXGA800", 1333, 855);
  public static final Resolution WXGA800_7IN = new Resolution("WXGA800-7in", 853, 1334);
  public static final Resolution HD720 = new Resolution("720p", 1280, 720);
  public static final Resolution HD1080 = new Resolution("1080p", 1920, 1080);
  static final List<Resolution> RESOLUTIONS = Arrays.asList(
      HVGA, QVGA, WQVGA_400, WQVGA_432, WSVGA, WVGA_800, WVGA_850, WXGA_720, WXGA_800, WXGA800_7IN, HD720, HD1080
  );

  public final String name;
  public final int width;
  public final int height;

  public Resolution(String name, int width, int height) {
    this.name = name;
    this.width = width;
    this.height = height;
  }

  @Override public String toString() {
    StringBuilder builder = new StringBuilder();
    boolean hasName = name != null && !"".equals(name);
    if (hasName) {
      builder.append(name).append(" (");
    }
    builder.append(width).append("x").append(height);
    if (hasName) {
      builder.append(")");
    }
    return builder.toString();
  }

  @Override public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof Resolution)) {
      return false;
    }
    Resolution other = (Resolution) o;
    return new EqualsBuilder()
        .append(name, other.name)
        .append(width, other.width)
        .append(height, other.height)
        .build();
  }

  public static Resolution parse(String resolution) {
    Matcher match = CUSTOM.matcher(resolution);
    if (match.matches()) {
      try {
        int width = Integer.parseInt(match.group(1));
        int height = Integer.parseInt(match.group(2));
        return new Resolution(null, width, height);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Unable to parse resolution: " + resolution, e);
      }
    }

    for (Resolution r : RESOLUTIONS) {
      if (r.name.equals(resolution)) {
        return r;
      }
    }

    throw new IllegalArgumentException("Unknown resolution: " + resolution);
  }
}
