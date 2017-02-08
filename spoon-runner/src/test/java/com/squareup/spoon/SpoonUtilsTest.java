package com.squareup.spoon;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public final class SpoonUtilsTest {
  @Test public void serialSanitizer() {
    assertThat(SpoonUtils.sanitizeSerial("1234")).isEqualTo("1234");
    assertThat(SpoonUtils.sanitizeSerial("FooBarBaz")).isEqualTo("FooBarBaz");
    assertThat(SpoonUtils.sanitizeSerial("ST-398H984")).isEqualTo("ST-398H984");
    assertThat(SpoonUtils.sanitizeSerial("10.0.0.1:1234")).isEqualTo("10_0_0_1_1234");
  }
}
