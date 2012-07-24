package com.squareup.spoon.model;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class ResolutionTest {
  public void testNamedParsing() {
    for (Resolution r : Resolution.RESOLUTIONS) {
      assertThat(Resolution.parse(r.name)).isEqualTo(r);
    }
  }

  public void testCustomParsing() {
    Resolution r = Resolution.parse("1024x768");
    assertThat(r.name).isNull();
    assertThat(r.width).isEqualTo(1024);
    assertThat(r.height).isEqualTo(768);
  }

  public void testInvalidCustomParsing() {
    try {
      Resolution.parse("12x12");
      fail();
    } catch (IllegalArgumentException expected) {
    }
    try {
      Resolution.parse("FORK");
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }
}
