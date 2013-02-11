package com.squareup.spoon;

import com.squareup.spoon.misc.StackTrace;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.squareup.spoon.DeviceTestResult.Status;

/** Model for representing a {@code device.html} page. */
final class HtmlDevice {
  static HtmlDevice from(String serial, DeviceResult result, File output) {
    List<TestResult> testResults = new ArrayList<TestResult>();
    int testsPassed = 0;
    for (Map.Entry<DeviceTest, DeviceTestResult> entry : result.getTestResults().entrySet()) {
      DeviceTestResult testResult = entry.getValue();
      testResults.add(TestResult.from(serial, entry.getKey(), testResult, output));
      if (testResult.getStatus() == Status.PASS) {
        testsPassed += 1;
      }
    }

    int testsRun = result.getTestResults().size();
    int testsFailed = testsRun - testsPassed;
    String totalTestsRun = testsRun + " test" + (testsRun != 1 ? "s" : "");
    DeviceDetails details = result.getDeviceDetails();
    String title = (details != null) ? details.getName() : serial;

    List<HtmlUtils.ExceptionInfo> exceptions = new ArrayList<HtmlUtils.ExceptionInfo>();
    for (StackTrace exception : result.getExceptions()) {
      exceptions.add(HtmlUtils.processStackTrace(exception));
    }

    StringBuilder subtitle = new StringBuilder();
    subtitle.append(totalTestsRun).append(" run");
    if (testsPassed > 0) {
      subtitle.append(" with ")
          .append(testsPassed)
          .append(" passing and ")
          .append(testsFailed)
          .append(" failing in ")
          .append(HtmlUtils.humanReadableDuration(result.getDuration()));
    }
    subtitle.append(" at ")
        .append(HtmlUtils.dateToString(result.getStarted()));

    return new HtmlDevice(serial, title, subtitle.toString(), testResults, exceptions);
  }

  public final String serial;
  public final String title;
  public final String subtitle;
  public final List<TestResult> testResults;
  public final boolean hasExceptions;
  public final List<HtmlUtils.ExceptionInfo> exceptions;

  HtmlDevice(String serial, String title, String subtitle, List<TestResult> testResults,
      List<HtmlUtils.ExceptionInfo> exceptions) {
    this.serial = serial;
    this.title = title;
    this.subtitle = subtitle;
    this.testResults = testResults;
    this.hasExceptions = !exceptions.isEmpty();
    this.exceptions = exceptions;
  }

  static final class TestResult implements Comparable<TestResult> {
    static TestResult from(String serial, DeviceTest test, DeviceTestResult result, File output) {
      String className = test.getClassName();
      String methodName = test.getMethodName();
      String classSimpleName = HtmlUtils.getClassSimpleName(className);
      String prettyMethodName = HtmlUtils.prettifyMethodName(methodName);
      String testId = HtmlUtils.testClassAndMethodToId(className, methodName);
      String status = HtmlUtils.getStatusCssClass(result);
      List<HtmlUtils.Screenshot> screenshots = new ArrayList<HtmlUtils.Screenshot>();
      for (File screenshot : result.getScreenshots()) {
        screenshots.add(HtmlUtils.getScreenshot(screenshot, output));
      }
      String animatedGif = HtmlUtils.createRelativeUri(result.getAnimatedGif(), output);
      HtmlUtils.ExceptionInfo exception = HtmlUtils.processStackTrace(result.getException());
      return new TestResult(serial, className, methodName, classSimpleName, prettyMethodName,
          testId, status, screenshots, animatedGif, exception);
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
    public final String animatedGif;
    public final HtmlUtils.ExceptionInfo exception;

    TestResult(String serial, String className, String methodName, String classSimpleName,
        String prettyMethodName, String testId, String status,
        List<HtmlUtils.Screenshot> screenshots, String animatedGif,
        HtmlUtils.ExceptionInfo exception) {
      this.serial = serial;
      this.className = className;
      this.methodName = methodName;
      this.classSimpleName = classSimpleName;
      this.prettyMethodName = prettyMethodName;
      this.testId = testId;
      this.status = status;
      this.hasScreenshots = !screenshots.isEmpty();
      this.screenshots = screenshots;
      this.animatedGif = animatedGif;
      this.exception = exception;
    }

    @Override public int compareTo(TestResult other) {
      int classComparison = className.compareTo(other.className);
      if (classComparison != 0) {
        return classComparison;
      }
      return methodName.compareTo(other.methodName);
    }
  }
}
