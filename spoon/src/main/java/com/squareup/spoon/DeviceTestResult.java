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

  private final Status status;
  private final String exception;
  private final long length;
  private final List<File> screenshots;
  private final File animatedGif;

  private DeviceTestResult(Status status, String exception, long length, List<File> screenshots,
      File animatedGif) {
    this.status = status;
    this.exception = exception;
    this.length = length;
    this.screenshots = unmodifiableList(new ArrayList<File>(screenshots));
    this.animatedGif = animatedGif;
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
    private Status status = Status.PASS;
    private String exception;
    private long start;
    private long length = -1;
    private File animatedGif;

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
      return new DeviceTestResult(status, exception, length, screenshots, animatedGif);
    }
  }
}
