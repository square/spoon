package com.squareup.spoon;

import org.junit.Test;

import static com.squareup.spoon.DeviceUtils.scrubLanguage;
import static com.squareup.spoon.DeviceUtils.scrubModel;
import static org.fest.assertions.api.Assertions.assertThat;

public class DeviceUtilsTest {
  @Test public void scrubModelExamples() {
    assertThat(scrubModel(null, null)).isNull();
    assertThat(scrubModel("DynaTAC", null)).isNull();
    assertThat(scrubModel(null, "8000x")).isEqualTo("8000x");
    assertThat(scrubModel(null, "DynaTAC 8000x")).isEqualTo("DynaTAC 8000x");
    assertThat(scrubModel("DynaTAC", "8000x")).isEqualTo("8000x");
    assertThat(scrubModel("DynaTAC", "DynaTAC 8000x")).isEqualTo("8000x");
    assertThat(scrubModel("DynaTAC", "DynaTAC-8000x")).isEqualTo("8000x");
    assertThat(scrubModel("dynatac", "DynaTAC-8000x")).isEqualTo("8000x");
    assertThat(scrubModel("DynaTAC", "DYNATAC-8000x")).isEqualTo("8000x");
  }

  @Test public void scrubLanguageExamples() {
    assertThat(scrubLanguage(null)).isNull();
    assertThat(scrubLanguage("ldpi")).isNull();
    assertThat(scrubLanguage("mdpi")).isNull();
    assertThat(scrubLanguage("hdpi")).isNull();
    assertThat(scrubLanguage("xhdpi")).isNull();
    assertThat(scrubLanguage("en")).isEqualTo("en");
  }
}
