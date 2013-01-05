package com.squareup.spoon;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.squareup.spoon.DeviceTestResult.Status;

/** Model for representing the {@code index.html} page. */
final class HtmlIndex {
  static HtmlIndex from(SpoonSummary summary) {
    String started = STARTED_FORMAT.get().format(summary.getStarted());
    int deviceCount = summary.getResults().size();

    int testsRun = 0;
    int totalSuccess = 0;
    Set<String> tests = new HashSet<String>();
    List<Device> devices = new ArrayList<Device>();
    for (Map.Entry<String, DeviceResult> result : summary.getResults().entrySet()) {
      devices.add(Device.from(result.getKey(), result.getValue()));
      testsRun += result.getValue().getTestResults().size();
      for (DeviceTestResult methodResult : result.getValue().getTestResults()) {
        tests.add(methodResult.getClassName() + " " + methodResult.getMethodName());
        if (methodResult.getStatus() == Status.PASS) {
          totalSuccess += 1;
        }
      }
    }

    Collections.sort(devices);

    int totalFailure = testsRun - totalSuccess;

    String totalTestsRun = testsRun + " test" + (testsRun != 1 ? "s" : "");
    String totalDevices = deviceCount + " device" + (deviceCount != 1 ? "s" : "");

    long minutes = summary.getLength() / 60;
    long seconds = summary.getLength() - (minutes * 60);
    String length = seconds + " second" + (seconds != 1 ? "s" : "");
    if (minutes != 0) {
      length = minutes + " minute" + (minutes != 1 ? "s" : "") + ", " + length;
    }

    return new HtmlIndex(summary.getTitle(), totalTestsRun, totalDevices, totalSuccess,
        totalFailure, length, started, tests.size(), devices);
  }

  private static final ThreadLocal<Format> STARTED_FORMAT = new ThreadLocal<Format>() {
    @Override protected Format initialValue() {
      return new SimpleDateFormat("yyyy-MM-dd hh:mm a");
    }
  };

  public final String title;
  public final String totalTestsRun;
  public final String totalDevices;
  public final int totalSuccess;
  public final int totalFailure;
  public final String totalLength;
  public final String started;
  public final int testCount;
  public final List<Device> devices;

  HtmlIndex(String title, String totalTestsRun, String totalDevices, int totalSuccess,
      int totalFailure, String totalLength, String started, int testCount, List<Device> devices) {
    this.title = title;
    this.totalTestsRun = totalTestsRun;
    this.totalDevices = totalDevices;
    this.totalSuccess = totalSuccess;
    this.totalFailure = totalFailure;
    this.totalLength = totalLength;
    this.started = started;
    this.testCount = testCount;
    this.devices = devices;
  }

  static final class Device implements Comparable<Device> {
    static Device from(String serial, DeviceResult result) {
      List<TestResult> testResults = new ArrayList<TestResult>();
      for (DeviceTestResult testResult : result.getTestResults()) {
        testResults.add(TestResult.from(serial, testResult));
      }
      return new Device(serial, result.getDeviceDetails().getName(), testResults);
    }

    public final String serial;
    public final String name;
    public final List<TestResult> testResults;

    Device(String serial, String name, List<TestResult> testResults) {
      this.serial = serial;
      this.name = name;
      this.testResults = testResults;
    }

    @Override public int compareTo(Device other) {
      return name.compareTo(other.name);
    }
  }

  static final class TestResult implements Comparable<TestResult> {
    static TestResult from(String serial, DeviceTestResult testResult) {
      String className = testResult.getClassName();
      String methodName = testResult.getMethodName();
      String classSimpleName = HtmlUtils.getClassSimpleName(className);
      String prettyMethodName = HtmlUtils.prettifyMethodName(methodName);
      String testId = HtmlUtils.testClassAndMethodToId(className, methodName);
      String status = HtmlUtils.getStatusCssClass(testResult);
      return new TestResult(serial, classSimpleName, prettyMethodName, testId, status);
    }

    public final String serial;
    public final String classSimpleName;
    public final String prettyMethodName;
    public final String testId;
    public final String status;

    TestResult(String serial, String classSimpleName, String prettyMethodName, String testId,
        String status) {
      this.serial = serial;
      this.classSimpleName = classSimpleName;
      this.prettyMethodName = prettyMethodName;
      this.testId = testId;
      this.status = status;
    }

    @Override public int compareTo(TestResult other) {
      return 0;
    }
  }
}
