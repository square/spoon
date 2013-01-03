// Copyright 2012 Square, Inc.
package com.squareup.spoon;

import java.io.File;
import org.junit.Before;
import org.junit.Test;

import static com.squareup.spoon.ExecutionTestResult.Screenshot.HTML_SEPARATOR;
import static com.squareup.spoon.ExecutionTestResult.Screenshot.INVALID_CHARS;
import static com.squareup.spoon.Screenshot.EXTENSION;
import static com.squareup.spoon.Screenshot.NAME_SEPARATOR;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExecutionTestResultTest {
  private static final String TAG = "say_cheese";
  private static final String NAME = TAG + EXTENSION;
  private static final String CLASS_NAME = "com.example.Test";
  private static final String SIMPLE_NAME = "Test";
  private static final String TEST_NAME = "testSomeTest";

  private ExecutionTestResult result;
  private long taken;
  private File screenshotFile;

  @Before public void setUp() {
    InstrumentationTest test = new InstrumentationTest(CLASS_NAME, SIMPLE_NAME, TEST_NAME);
    result = new ExecutionTestResult(test);
    assertThat(result.screenshots).isEmpty();

    taken = System.currentTimeMillis();
    screenshotFile = mock(File.class);
  }

  @Test public void validScreenshot() {
    when(screenshotFile.getName()).thenReturn(taken + NAME_SEPARATOR + NAME);

    result.addScreenshot(screenshotFile);
    assertThat(result.screenshots).hasSize(1);

    ExecutionTestResult.Screenshot screenshot = result.screenshots.get(0);
    assertThat(screenshot.file).isEqualTo(screenshotFile);
    assertThat(screenshot.taken.getTime()).isEqualTo(taken);
    assertThat(screenshot.tag).isEqualTo(TAG);

    String expectedId = CLASS_NAME + HTML_SEPARATOR + TEST_NAME + HTML_SEPARATOR + TAG;
    expectedId = expectedId.replaceAll(INVALID_CHARS, "-");
    assertThat(screenshot.id).isEqualTo(expectedId);

    String expectedGroup = SIMPLE_NAME + HTML_SEPARATOR + TEST_NAME;
    expectedGroup = expectedGroup.replaceAll(INVALID_CHARS, "-");
    assertThat(screenshot.screenshotGroup).isEqualTo(expectedGroup);
  }

  @Test(expected = IllegalArgumentException.class)
  public void missingTimestamp() {
    when(screenshotFile.getName()).thenReturn(NAME);
    result.addScreenshot(screenshotFile);
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidExtension() {
    when(screenshotFile.getName()).thenReturn(taken + NAME_SEPARATOR + TAG + ".jpg");
    result.addScreenshot(screenshotFile);
  }
}
