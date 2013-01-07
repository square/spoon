package com.squareup.spoon;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.unmodifiableList;

public final class DeviceTestResult {
  public enum Status {
    PASS, FAIL, ERROR
  }

  private final String className;
  private final String methodName;
  private final Status status;
  private final String exception;
  private final long length;
  private final List<File> screenshots;
  private final File animatedGif;

  private DeviceTestResult(String className, String methodName, Status status, String exception,
      long length, List<File> screenshots, File animatedGif) {
    this.className = className;
    this.methodName = methodName;
    this.status = status;
    this.exception = exception;
    this.length = length;
    this.screenshots = unmodifiableList(new ArrayList<File>(screenshots));
    this.animatedGif = animatedGif;
  }

  /** Class name of test. */
  public String getClassName() {
    return className;
  }

  /** Method name of test. */
  public String getMethodName() {
    return methodName;
  }

  /** Execution status. */
  public Status getStatus() {
    return status;
  }

  /** Exception thrown during execution. */
  public String getException() {
    return exception;
  }

  /** Length of test execution, in seconds. */
  public long getLength() {
    return length;
  }

  /** Screenshots taken during test. */
  public List<File> getScreenshots() {
    return screenshots;
  }

  /** Animated GIF of screenshots. */
  public File getAnimatedGif() {
    return animatedGif;
  }

  public static class Builder {
    private final List<File> screenshots = new ArrayList<File>();
    private String className;
    private String methodName;
    private Status status = Status.PASS;
    private String exception;
    private long start;
    private long length = -1;
    private File animatedGif;

    public Builder setClassName(String className) {
      checkNotNull(className);
      checkArgument(this.className == null, "Class name already assigned.");
      this.className = className;
      return this;
    }

    public Builder setMethodName(String methodName) {
      checkNotNull(methodName);
      checkArgument(this.methodName == null, "Method name already assigned.");
      this.methodName = methodName;
      return this;
    }

    public Builder markTestAsFailed(String exception) {
      checkNotNull(exception);
      checkArgument(status == Status.PASS, "Status was already marked as " + status);
      status = Status.FAIL;
      this.exception = exception;
      return this;
    }

    public Builder markTestAsError(String exception) {
      checkNotNull(exception);
      checkArgument(status == Status.PASS, "Status was already marked as " + status);
      status = Status.ERROR;
      this.exception = exception;
      return this;
    }

    public Builder startTest() {
      checkArgument(start == 0, "Start already called.");
      start = System.nanoTime();
      return this;
    }

    public Builder endTest() {
      checkArgument(start != 0, "Start was not called.");
      checkArgument(length == -1, "End was already called.");
      length = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start);
      return this;
    }

    public Builder addScreenshot(File screenshot) {
      checkNotNull(screenshot);
      screenshots.add(screenshot);
      return this;
    }

    public Builder setAnimatedGif(File animatedGif) {
      checkNotNull(animatedGif);
      checkArgument(this.animatedGif == null, "Animated GIF already set.");
      this.animatedGif = animatedGif;
      return this;
    }

    public DeviceTestResult build() {
      checkNotNull(className, "Class name never set.");
      checkNotNull(methodName, "Method name never set.");
      return new DeviceTestResult(className, methodName, status, exception, length, screenshots,
          animatedGif);
    }
  }
}
