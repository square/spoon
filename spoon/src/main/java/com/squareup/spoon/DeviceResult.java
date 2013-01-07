package com.squareup.spoon;

import com.android.ddmlib.testrunner.TestIdentifier;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.unmodifiableList;

/** Represents the results of executing instrumentation tests on a single device. */
public final class DeviceResult {
  private final boolean installFailed;
  private final String installMessage;
  private final DeviceDetails deviceDetails;
  private final List<DeviceTestResult> testResults;
  private final Date started;
  private final long length;
  private final List<String> exceptions;

  private DeviceResult(boolean installFailed, String installMessage, DeviceDetails deviceDetails,
      List<DeviceTestResult> testResults, Date started, long length, List<String> exceptions) {
    this.installFailed = installFailed;
    this.installMessage = installMessage;
    this.deviceDetails = deviceDetails;
    this.started = started;
    this.testResults = unmodifiableList(new ArrayList<DeviceTestResult>(testResults));
    this.length = length;
    this.exceptions = unmodifiableList(new ArrayList<String>(exceptions));
  }

  /**
   * {@code true} if either application or instrumentation APK failed to install.
   *
   * @see #getInstallMessage()
   */
  public boolean getInstallFailed() {
    return installFailed;
  }

  /**
   * Installation failure message. Only present if application or instrumentation APK installation
   * failed.
   *
   * @see #getInstallFailed()
   */
  public String getInstallMessage() {
    return installMessage;
  }

  /** Configuration and hardware device details. */
  public DeviceDetails getDeviceDetails() {
    return deviceDetails;
  }

  /** Individual test results. */
  public List<DeviceTestResult> getTestResults() {
    return testResults;
  }

  /** Execution start time. */
  public Date getStarted() {
    return started;
  }

  /** Length (in seconds) of execution of all tests on device, or {@code -1} if none ran. */
  public long getLength() {
    return length;
  }

  /** Exceptions that occurred during execution. */
  public List<String> getExceptions() {
    return exceptions;
  }

  static class Builder {
    private boolean installFailed = false;
    private String installMessage = null;
    private final Map<TestIdentifier, DeviceTestResult.Builder> testResultBuilders =
        new HashMap<TestIdentifier, DeviceTestResult.Builder>();
    private DeviceDetails deviceDetails = null;
    private final Date started = new Date();
    private long start;
    private long length = -1;
    private final List<String> exceptions = new ArrayList<String>();

    public Builder addTestResultBuilder(TestIdentifier test,
        DeviceTestResult.Builder methodResultBuilder) {
      checkArgument(!installFailed, "Cannot add test result builder when install failed.");
      checkNotNull(methodResultBuilder);
      testResultBuilders.put(test, methodResultBuilder);
      return this;
    }

    public DeviceTestResult.Builder getMethodResultBuilder(TestIdentifier test) {
      return testResultBuilders.get(test);
    }

    public Builder setDeviceDetails(DeviceDetails deviceDetails) {
      checkNotNull(deviceDetails);
      this.deviceDetails = deviceDetails;
      return this;
    }

    public Builder markInstallAsFailed(String message) {
      checkNotNull(message);
      checkArgument(!installFailed, "Install already marked as failed.");
      installFailed = true;
      installMessage = message;
      return this;
    }

    public Builder startTests() {
      checkArgument(!installFailed, "Cannot start tests when install failed.");
      checkArgument(start == 0, "Start already called.");
      start = System.nanoTime();
      return this;
    }

    public Builder endTests() {
      checkArgument(start != 0, "Start was not called.");
      checkArgument(length == -1, "End was already called.");
      length = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start);
      return this;
    }

    public Builder addException(Throwable exception) {
      checkNotNull(exception);
      StringWriter sw = new StringWriter();
      exception.printStackTrace(new PrintWriter(sw));
      exceptions.add(sw.toString());
      return this;
    }

    public DeviceResult build() {
      // Convert builders to actual instances.
      List<DeviceTestResult> testResults = new ArrayList<DeviceTestResult>();
      for (DeviceTestResult.Builder builder : testResultBuilders.values()) {
        testResults.add(builder.build());
      }

      return new DeviceResult(installFailed, installMessage, deviceDetails, testResults, started,
          length, exceptions);
    }
  }
}
