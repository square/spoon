package com.squareup.spoon;

import java.io.File;
import org.junit.Test;

import static com.squareup.spoon.HtmlUtils.createRelativeUri;
import static com.squareup.spoon.HtmlUtils.prettifyImageName;
import static com.squareup.spoon.HtmlUtils.prettifyMethodName;
import static com.squareup.spoon.HtmlUtils.humanReadableDuration;
import static org.fest.assertions.api.Assertions.assertThat;

public class HtmlUtilsTest {
  @Test public void prettifyImageNameExamples() {
    // Simple cases.
    assertThat(prettifyImageName("012344567_click-the-button")).isEqualTo("Click The Button");
    assertThat(prettifyImageName("987245978_click_the_button")).isEqualTo("Click The Button");
  }

  @Test public void prettifyTestNameExamples() {
    // Simple cases.
    assertThat(prettifyMethodName("testClickTheButton")).isEqualTo("Click The Button");
    assertThat(prettifyMethodName("testClickTheButton_ShowsTheName")) //
        .isEqualTo("Click The Button, Shows The Name");
    assertThat(prettifyMethodName("testClickTheButton_ShowsTheName_HidesTheInput")) //
        .isEqualTo("Click The Button, Shows The Name, Hides The Input");

    // Over-eager underscore users.
    assertThat(prettifyMethodName("testOne__Two_Three")).isEqualTo("One, Two, Three");
    assertThat(prettifyMethodName("testOne_Two____Three")).isEqualTo("One, Two, Three");
    assertThat(prettifyMethodName("test__One_Two_Three")).isEqualTo("One, Two, Three");
    assertThat(prettifyMethodName("testOne_Two_Three__")).isEqualTo("One, Two, Three");

    // Harder, edge cases.
    assertThat(prettifyMethodName("testURLConnection")).isEqualTo("URL Connection");
    assertThat(prettifyMethodName("testAHardCase")).isEqualTo("A Hard Case");
    assertThat(prettifyMethodName("testOneATwo")).isEqualTo("One A Two");
  }

  @Test(expected = IllegalArgumentException.class)
  public void prettyTestNameFailsWhenNotPrefixedWithTest() {
    prettifyMethodName("doingTheThings");
  }

  @Test public void relativeUriCreation() {
    File file = new File("/path/to/image/this/that/whatever.png");
    File output = new File("/path/to");
    assertThat(createRelativeUri(file, output)).isEqualTo("image/this/that/whatever.png");
  }

  @Test(expected = IllegalArgumentException.class)
  public void relativeUriCreationFailsIfSame() {
    File output = new File("/path/to");
    createRelativeUri(output, output);
  }

  @Test public void humanReadableDurationCases() {
    assertThat(humanReadableDuration(0)).isEqualTo("0 seconds");
    assertThat(humanReadableDuration(1)).isEqualTo("1 second");
    assertThat(humanReadableDuration(2)).isEqualTo("2 seconds");
    assertThat(humanReadableDuration(59)).isEqualTo("59 seconds");
    assertThat(humanReadableDuration(60)).isEqualTo("1 minute");
    assertThat(humanReadableDuration(61)).isEqualTo("1 minute, 1 second");
    assertThat(humanReadableDuration(62)).isEqualTo("1 minute, 2 seconds");
    assertThat(humanReadableDuration(122)).isEqualTo("2 minutes, 2 seconds");
    assertThat(humanReadableDuration(3661)).isEqualTo("61 minutes, 1 second");
  }
}
