package com.squareup.spoon.model;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Device dipslay resolution. */
public class Resolution {
  private static final Pattern CUSTOM = Pattern.compile("(\\d{3,})x(\\d{3,})");
  private static final List<Resolution> RESOLUTIONS = Arrays.asList(
      new Resolution("HVGA", 320, 480),
      new Resolution("QVGA", 240, 320),
      new Resolution("WQVGA400", 240, 400),
      new Resolution("WQVGA432", 240, 432),
      new Resolution("WSVGA", 1076, 654),
      new Resolution("WVGA800", 480, 800),
      new Resolution("WVGA850", 480, 854),
      new Resolution("WXGA720", 773, 1334),
      new Resolution("WXGA800", 1333, 855),
      new Resolution("WXGA800-7in", 853, 1334),
      new Resolution("720p", 1280, 720),
      new Resolution("1080p", 1920, 1080)
  );

  public String name;
  public int width;
  public int height;

  public Resolution() {
  }

  public Resolution(String name, int width, int height) {
    this.name = name;
    this.width = width;
    this.height = height;
  }

  @Override public String toString() {
    StringBuilder builder = new StringBuilder();
    boolean hasName = !"".equals(name);
    if (hasName) {
      builder.append(name).append(" (");
    }
    builder.append(width).append("x").append(height);
    if (hasName) {
      builder.append(")");
    }
    return builder.toString();
  }

  public static Resolution parse(String resolution) {
    Matcher match = CUSTOM.matcher(resolution);
    if (match != null) {
      try {
        Resolution r = new Resolution();
        r.width = Integer.parseInt(match.group(1));
        r.height = Integer.parseInt(match.group(2));
        return r;
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
