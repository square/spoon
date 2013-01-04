package com.squareup.spoon;

import org.junit.Test;

import static com.squareup.spoon.Utils.prettifyImageName;
import static com.squareup.spoon.Utils.prettifyTestName;
import static org.fest.assertions.api.Assertions.assertThat;

public class UtilsTest {
  @Test public void prettifyImageNameExamples() {
    // Simple cases.
    assertThat(prettifyImageName("click-the-button")).isEqualTo("Click The Button");
    assertThat(prettifyImageName("click_the_button")).isEqualTo("Click The Button");
  }

  @Test public void prettifyTestNameExamples() {
    // Simple cases.
    assertThat(prettifyTestName("testClickTheButton")).isEqualTo("Click The Button");
    assertThat(prettifyTestName("testClickTheButton_ShowsTheName")) //
        .isEqualTo("Click The Button, Shows The Name");
    assertThat(prettifyTestName("testClickTheButton_ShowsTheName_HidesTheInput")) //
        .isEqualTo("Click The Button, Shows The Name, Hides The Input");

    // Over-eager underscore users.
    assertThat(prettifyTestName("testOne__Two_Three")).isEqualTo("One, Two, Three");
    assertThat(prettifyTestName("testOne_Two____Three")).isEqualTo("One, Two, Three");
    assertThat(prettifyTestName("test__One_Two_Three")).isEqualTo("One, Two, Three");
    assertThat(prettifyTestName("testOne_Two_Three__")).isEqualTo("One, Two, Three");

    // Harder, edge cases.
    assertThat(prettifyTestName("testURLConnection")).isEqualTo("URL Connection");
    assertThat(prettifyTestName("testAHardCase")).isEqualTo("A Hard Case");
    assertThat(prettifyTestName("testOneATwo")).isEqualTo("One A Two");
  }

  @Test(expected = IllegalArgumentException.class)
  public void prettyTestNameFailsWhenNotPrefixedWithTest() {
    prettifyTestName("doingTheThings");
  }
}
