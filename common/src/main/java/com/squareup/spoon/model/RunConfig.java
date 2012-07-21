package com.squareup.spoon.model;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.File;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

public class RunConfig {
  public String name;
  public File output = new File("spoon-output");
  public File app;
  public File test;

  @Override public String toString() {
    return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
  }
}
