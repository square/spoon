package com.squareup.spoon;

import com.squareup.spoon.misc.StackTrace;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.unmodifiableList;

public final class DeviceTestResult {
  /** Separator between screenshot timestamp and tag. */
  public static final String SCREENSHOT_SEPARATOR = Spoon.NAME_SEPARATOR;

  public enum Status {
    PASS, FAIL, ERROR
  }

  private final Status status;
  private final StackTrace exception;
  private final long duration;
  private final List<File> screenshots;
  private final File animatedGif;
  private final List<DeviceLogMessage> log;

  private DeviceTestResult(Status status, StackTrace exception, long duration,
      List<File> screenshots, File animatedGif, List<DeviceLogMessage> log) {
    this.status = status;
    this.exception = exception;
    this.duration = duration;
    this.screenshots = unmodifiableList(new ArrayList<File>(screenshots));
    this.animatedGif = animatedGif;
    this.log = log;
  }

  /** Execution status. */
  public Status getStatus() {
    return status;
  }

  /** Exception thrown during execution. */
  public StackTrace getException() {
    return exception;
  }

  /** Length of test execution, in seconds. */
  public long getDuration() {
    return duration;
  }

  /** Screenshots taken during test. */
  public List<File> getScreenshots() {
    return screenshots;
  }

  /** Animated GIF of screenshots. */
  public File getAnimatedGif() {
    return animatedGif;
  }

  public List<DeviceLogMessage> getLog() {
    return log;
  }

  public static class Builder {
    private final List<File> screenshots = new ArrayList<File>();
    private Status status = Status.PASS;
    private StackTrace exception;
    private long start;
    private long duration = -1;
    private File animatedGif;
    private List<DeviceLogMessage> log;

    public Builder markTestAsFailed(String message) {
      checkNotNull(message);
      checkArgument(status == Status.PASS, "Status was already marked as " + status);
      status = Status.FAIL;
      exception = StackTrace.from(message);
      return this;
    }

    public Builder markTestAsError(String message) {
      checkNotNull(message);
      checkArgument(status == Status.PASS, "Status was already marked as " + status);
      status = Status.ERROR;
      exception = StackTrace.from(message);
      return this;
    }

    public Builder setLog(List<DeviceLogMessage> log) {
      checkNotNull(log);
      checkArgument(this.log == null, "Log already added.");
      this.log = log;
      return this;
    }

    public Builder startTest() {
      checkArgument(start == 0, "Start already called.");
      start = System.nanoTime();
      return this;
    }

    public Builder endTest() {
      checkArgument(start != 0, "Start was not called.");
      checkArgument(duration == -1, "End was already called.");
      duration = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start);
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
      return new DeviceTestResult(status, exception, duration, screenshots, animatedGif, log);
    }
  }
}
