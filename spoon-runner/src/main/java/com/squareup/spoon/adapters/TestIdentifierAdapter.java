package com.squareup.spoon.adapters;

import org.apache.commons.lang3.text.WordUtils;

import com.android.ddmlib.testrunner.TestIdentifier;

public enum TestIdentifierAdapter {

  JUNIT() {
    @Override
    public TestIdentifier adapt(TestIdentifier test) {
      return test;
    }
  },
  CUCUMBER() {
    @Override
    public TestIdentifier adapt(TestIdentifier test) {
      String className = WordUtils.capitalize(test.getClassName()).replaceAll(
          "[^a-zA-Z0-9_]+", "");
      String testName = "test"
          + WordUtils.capitalize(test.getTestName()).replaceAll(
              "[^a-zA-Z0-9_]+", "");

      return new TestIdentifier(className, testName);
    }
  };

  private static final String CUCUMBER_NAME = "cucumber.api.android.CucumberInstrumentation";

  public abstract TestIdentifier adapt(TestIdentifier test);

  public static TestIdentifierAdapter fromTestRunner(String name) {
    return CUCUMBER_NAME.equals(name) ? CUCUMBER : JUNIT;
  }
}