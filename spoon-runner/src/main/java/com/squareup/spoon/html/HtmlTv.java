package com.squareup.spoon.html;

import com.google.gson.Gson;
import com.squareup.spoon.DeviceDetails;
import com.squareup.spoon.DeviceResult;
import com.squareup.spoon.DeviceTestResult;
import com.squareup.spoon.SpoonSummary;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/** Model for representing the {@code tv.html} page. */
final class HtmlTv {
  static HtmlTv from(Gson gson, SpoonSummary summary, File outputPath) {
    String testDate = HtmlUtils.dateToTvString(summary.getStarted());
    String title = summary.getTitle();
    String duration = HtmlUtils.humanReadableDuration(summary.getDuration());

    List<Device> devices = summary.getResults()
        .entrySet()
        .stream()
        .map(result -> Device.from(result.getKey(), result.getValue(), outputPath))
        .sorted()
        .collect(Collectors.toList());

    return new HtmlTv(gson, title, testDate, duration, devices);
  }

  public final String title;
  public final String testDate;
  public final String duration;
  public final String outputJson;

  HtmlTv(Gson gson, String title, String testDate, String duration, List<Device> devices) {
    this.title = title;
    this.testDate = testDate;
    this.duration = duration;
    this.outputJson = gson.toJson(devices);
  }

  static final class Device implements Comparable<Device> {
    static Device from(String serial, DeviceResult result, File outputPath) {
      List<TestResult> testResults = result.getTestResults()
          .entrySet()
          .stream()
          .map(entry -> {
            String classSimpleName = HtmlUtils.getClassSimpleName(entry.getKey().getClassName());
            String prettyMethodName = entry.getKey().getMethodName();

            return TestResult.from(serial, classSimpleName, prettyMethodName, entry.getValue(),
                    outputPath);
          })
          .collect(toList());

      DeviceDetails deviceDetails = result.getDeviceDetails();
      String name = (deviceDetails != null) ? deviceDetails.getName() : serial;
      String details = HtmlUtils.deviceDetailsToString(deviceDetails);

      return new Device(serial, name, details, testResults);
    }

    public final String serial;
    public final String name;
    public final String details;
    public final List<TestResult> testResults;

    Device(String serial, String name, String details, List<TestResult> testResults) {
      this.serial = serial;
      this.name = name;
      this.details = details;
      this.testResults = testResults;
    }

    @Override public int compareTo(Device other) {
      if (name == null && other.name == null) {
        return serial.compareTo(other.serial);
      }
      if (name == null) {
        return 1;
      }
      if (other.name == null) {
        return -1;
      }
      return name.compareTo(other.name);
    }

    @Override public String toString() {
      return name != null ? name : serial;
    }
  }

  static final class TestResult implements Comparable<TestResult> {
    static TestResult from(String serial, String className, String name, DeviceTestResult result,
        File output) {
      String status = HtmlUtils.getStatusCssClass(result);

      List<HtmlUtils.Screenshot> screenshots = result.getScreenshots()
          .stream()
          .map(screenshot -> HtmlUtils.getScreenshot(screenshot, output))
          .collect(toList());
      return new TestResult(className, name, serial, status, screenshots);
    }

    public final String classSimpleName;
    public final String methodPrettyName;
    public final String serial;
    public final String status;
    public final List<HtmlUtils.Screenshot> screenshots;

    TestResult(String className, String methodPrettyName, String serial, String status,
        List<HtmlUtils.Screenshot> screenshots) {
      this.classSimpleName = className;
      this.methodPrettyName = methodPrettyName;
      this.serial = serial;
      this.status = status;
      this.screenshots = screenshots;
    }

    @Override public int compareTo(TestResult other) {
      return methodPrettyName.compareTo(other.methodPrettyName);
    }
  }
}
