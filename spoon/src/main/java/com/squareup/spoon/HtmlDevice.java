package com.squareup.spoon;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Model for representing a {@code device.html} page. */
final class HtmlDevice {
  static HtmlDevice from(String serial, DeviceResult result, File output) {
    List<TestResult> testResults = new ArrayList<TestResult>();
    for (DeviceTestResult testResult : result.getTestResults()) {
      testResults.add(TestResult.from(serial, testResult, output));
    }
    return new HtmlDevice(serial, result.getDeviceDetails().getName(), testResults);
  }

  public final String serial;
  public final String name;
  public final List<TestResult> testResults;

  HtmlDevice(String serial, String name, List<TestResult> testResults) {
    this.serial = serial;
    this.name = name;
    this.testResults = testResults;
  }

  static final class TestResult implements Comparable<TestResult> {
    static TestResult from(String serial, DeviceTestResult testResult, File output) {
      String className = testResult.getClassName();
      String methodName = testResult.getMethodName();
      String classSimpleName = HtmlUtils.getClassSimpleName(className);
      String prettyMethodName = HtmlUtils.prettifyMethodName(methodName);
      String testId = HtmlUtils.testClassAndMethodToId(className, methodName);
      String status = HtmlUtils.getStatusCssClass(testResult);
      List<HtmlUtils.Screenshot> screenshots = new ArrayList<HtmlUtils.Screenshot>();
      for (File screenshot : testResult.getScreenshots()) {
        screenshots.add(HtmlUtils.getScreenshot(screenshot, output));
      }
      boolean hasScreenshots = !screenshots.isEmpty();
      HtmlUtils.StackTrace exception = HtmlUtils.parseException(testResult.getException());
      return new TestResult(serial, className, methodName, classSimpleName, prettyMethodName,
          testId, status, hasScreenshots, screenshots, exception);
    }

    public final String serial;
    public final String className;
    public final String methodName;
    public final String classSimpleName;
    public final String prettyMethodName;
    public final String testId;
    public final String status;
    public final boolean hasScreenshots;
    public final List<HtmlUtils.Screenshot> screenshots;
    public final HtmlUtils.StackTrace exception;

    TestResult(String serial, String className, String methodName, String classSimpleName,
        String prettyMethodName, String testId, String status, boolean hasScreenshots,
        List<HtmlUtils.Screenshot> screenshots, HtmlUtils.StackTrace exceptions) {
      this.serial = serial;
      this.className = className;
      this.methodName = methodName;
      this.classSimpleName = classSimpleName;
      this.prettyMethodName = prettyMethodName;
      this.testId = testId;
      this.status = status;
      this.hasScreenshots = hasScreenshots;
      this.screenshots = screenshots;
      this.exception = exceptions;
    }

    @Override public int compareTo(TestResult other) {
      return 0;
    }
  }
}
