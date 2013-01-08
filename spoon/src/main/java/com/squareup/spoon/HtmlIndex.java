package com.squareup.spoon;

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
    int testsRun = 0;
    int totalSuccess = 0;
    Set<DeviceTest> tests = new HashSet<DeviceTest>();
    List<Device> devices = new ArrayList<Device>();
    for (Map.Entry<String, DeviceResult> result : summary.getResults().entrySet()) {
      devices.add(Device.from(result.getKey(), result.getValue()));
      Map<DeviceTest, DeviceTestResult> testResults = result.getValue().getTestResults();
      testsRun += testResults.size();
      for (Map.Entry<DeviceTest, DeviceTestResult> entry : testResults.entrySet()) {
        tests.add(entry.getKey());
        if (entry.getValue().getStatus() == Status.PASS) {
          totalSuccess += 1;
        }
      }
    }

    Collections.sort(devices);

    int totalFailure = testsRun - totalSuccess;

    int deviceCount = summary.getResults().size();
    String started = HtmlUtils.dateToString(summary.getStarted());
    String totalTestsRun = testsRun + " test" + (testsRun != 1 ? "s" : "");
    String totalDevices = deviceCount + " device" + (deviceCount != 1 ? "s" : "");
    String totalLength = HtmlUtils.secondsToTimeString(summary.getLength());

    return new HtmlIndex(summary.getTitle(), totalTestsRun, totalDevices, totalSuccess,
        totalFailure, totalLength, started, tests.size(), devices);
  }

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
      for (Map.Entry<DeviceTest, DeviceTestResult> entry : result.getTestResults().entrySet()) {
        testResults.add(TestResult.from(serial, entry.getKey(), entry.getValue()));
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
    static TestResult from(String serial, DeviceTest test, DeviceTestResult testResult) {
      String className = test.getClassName();
      String methodName = test.getMethodName();
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
