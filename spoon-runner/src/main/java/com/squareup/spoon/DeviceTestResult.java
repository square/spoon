package com.squareup.spoon;

import com.android.ddmlib.logcat.LogCatMessage;
import com.squareup.spoon.misc.StackTrace;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.squareup.spoon.SpoonLogger.logError;
import static java.util.Collections.unmodifiableList;

/** Represents the result of a single test method on a single device. */
public final class DeviceTestResult {
  public enum Status {
    PASS, FAIL
  }

  private final Status status;
  private final StackTrace exception;
  private final long duration;
  private final List<File> screenshots;
  private final List<File> files;
  private final File animatedGif;
  private final List<LogCatMessage> log;

  private DeviceTestResult(Status status, StackTrace exception, long duration,
      List<File> screenshots, File animatedGif, List<LogCatMessage> log, List<File> files) {
    this.status = status;
    this.exception = exception;
    this.duration = duration;
    this.screenshots = unmodifiableList(new ArrayList<>(screenshots));
    this.files = unmodifiableList(new ArrayList<>(files));
    this.animatedGif = animatedGif;
    this.log = unmodifiableList(new ArrayList<>(log));
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

  /** Arbitrary files saved from the test */
  public List<File> getFiles() {
    return files;
  }

  public List<LogCatMessage> getLog() {
    return log;
  }

  public static class Builder {
    private final List<File> screenshots = new ArrayList<>();
    private final List<File> files = new ArrayList<>();
    private Status status = Status.PASS;
    private StackTrace exception;
    private long start;
    private long duration = -1;
    private File animatedGif;
    private List<LogCatMessage> log;

    public Builder markTestAsFailed(String message) {
      checkNotNull(message);
      if (status != Status.PASS) {
        logError("Status was already marked as failed!");
      }
      status = Status.FAIL;
      exception = StackTrace.from(message);
      return this;
    }

    public Builder setLog(List<LogCatMessage> log) {
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
      if (duration != -1) {
        logError("Test was already marked as ended!");
      }
      duration = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start);
      return this;
    }

    public Builder addScreenshot(File screenshot) {
      checkNotNull(screenshot);
      screenshots.add(screenshot);
      return this;
    }

    public Builder addFile(File file) {
      checkNotNull(file);
      files.add(file);
      return this;
    }

    public Builder setAnimatedGif(File animatedGif) {
      checkNotNull(animatedGif);
      checkArgument(this.animatedGif == null, "Animated GIF already set.");
      this.animatedGif = animatedGif;
      return this;
    }

    public DeviceTestResult build() {
      if (log == null) {
        log = Collections.emptyList();
      }
      return new DeviceTestResult(status, exception, duration,
              screenshots, animatedGif, log, files);
    }
  }
}
