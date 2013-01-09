package com.squareup.spoon;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.squareup.spoon.DeviceTestResult.Status;

/** Model for representing a {@code test.html} page. */
final class HtmlTest {
  public static HtmlTest from(DeviceTest test, SpoonSummary summary, File output) {
    int deviceCount = 0;
    int testsPassed = 0;
    int duration = 0;
    List<TestResult> devices = new ArrayList<TestResult>();
    for (Map.Entry<String, DeviceResult> entry : summary.getResults().entrySet()) {
      DeviceResult deviceResult = entry.getValue();
      DeviceTestResult testResult = deviceResult.getTestResults().get(test);
      if (testResult != null) {
        deviceCount += 1;
        if (testResult.getStatus() == Status.PASS) {
          testsPassed += 1;
          duration += testResult.getDuration();
        }
      }
      String serial = entry.getKey();
      DeviceDetails details = deviceResult.getDeviceDetails();
      String name = (details != null) ? details.getName() : serial;
      devices.add(TestResult.from(serial, name, testResult, output));
    }

    int testsFailed = deviceCount - testsPassed;
    String totalDevices = deviceCount + " device" + (deviceCount != 1 ? "s" : "");
    String averageLength;
    if (testsPassed > 0) {
      averageLength = HtmlUtils.humanReadableDuration(duration / testsPassed);
    } else {
      averageLength = HtmlUtils.humanReadableDuration(0);
    }
    String className = HtmlUtils.getClassSimpleName(test.getClassName());
    String testName = HtmlUtils.prettifyMethodName(test.getMethodName());

    return new HtmlTest(testName, className, totalDevices, testsPassed, testsFailed, averageLength,
        devices);
  }

  public final String testName;
  public final String classSimpleName;
  public final String totalDevices;
  public final int testsPassed;
  public final int testsFailed;
  public final String averageDuration;
  public final List<TestResult> devices;

  HtmlTest(String testName, String classSimpleName, String totalDevices, int testsPassed,
      int testsFailed, String averageDuration, List<TestResult> devices) {
    this.testName = testName;
    this.classSimpleName = classSimpleName;
    this.totalDevices = totalDevices;
    this.testsPassed = testsPassed;
    this.testsFailed = testsFailed;
    this.averageDuration = averageDuration;
    this.devices = devices;
  }

  static final class TestResult implements Comparable<TestResult> {
    static TestResult from(String serial, String name, DeviceTestResult result, File output) {
      String status = HtmlUtils.getStatusCssClass(result);

      List<HtmlUtils.Screenshot> screenshots = new ArrayList<HtmlUtils.Screenshot>();
      for (File screenshot : result.getScreenshots()) {
        screenshots.add(HtmlUtils.getScreenshot(screenshot, output));
      }
      String animatedGif = HtmlUtils.createRelativeUri(result.getAnimatedGif(), output);
      HtmlUtils.StackTrace exception = HtmlUtils.parseException(result.getException());

      return new TestResult(name, serial, status, screenshots, animatedGif, exception);
    }

    public final String name;
    public final String serial;
    public final String status;
    public final boolean hasScreenshots;
    public final List<HtmlUtils.Screenshot> screenshots;
    public final String animatedGif;
    public final HtmlUtils.StackTrace exception;

    TestResult(String name, String serial, String status, List<HtmlUtils.Screenshot> screenshots,
        String animatedGif, HtmlUtils.StackTrace exception) {
      this.name = name;
      this.serial = serial;
      this.status = status;
      this.hasScreenshots = !screenshots.isEmpty();
      this.screenshots = screenshots;
      this.animatedGif = animatedGif;
      this.exception = exception;
    }

    @Override public int compareTo(TestResult other) {
      return name.compareTo(other.name);
    }
  }
}
