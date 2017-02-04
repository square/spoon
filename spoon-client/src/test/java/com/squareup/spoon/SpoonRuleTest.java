// Copyright 2012 Square, Inc.
package com.squareup.spoon;

import org.junit.Rule;
import org.junit.Test;

public final class SpoonRuleTest {
  @Rule public final SpoonRule spoon = new SpoonRule();

  @Test(expected = IllegalArgumentException.class)
  public void invalidTagThrowsException() {
    spoon.screenshot(null, "!@#$%^&*()");
  }
}
