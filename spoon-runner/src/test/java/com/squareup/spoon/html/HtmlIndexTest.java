package com.squareup.spoon.html;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.squareup.spoon.html.HtmlIndex.Device;
import static com.squareup.spoon.html.HtmlIndex.TestResult;

public final class HtmlIndexTest {
  private static final List<TestResult> NO_TESTS = Collections.emptyList();

  @Test public void devicesWithNoNamesAreSortedLast() {
    Device device1 = new Device("4567", null, NO_TESTS, false);
    Device device2 = new Device("1234", "a", NO_TESTS, false);
    Device device3 = new Device("1234", null, NO_TESTS, false);
    Device device4 = new Device("1234", "b", NO_TESTS, false);
    Device device5 = new Device("1212", null, NO_TESTS, false);
    List<Device> devices = Arrays.asList(device1, device2, device3, device4, device5);
    Collections.sort(devices);
    List<Device> expected = Arrays.asList(device2, device4, device5, device3, device1);
    assertThat(devices).isEqualTo(expected);
  }
}
