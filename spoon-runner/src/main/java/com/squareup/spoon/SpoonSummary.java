package com.squareup.spoon;

import com.android.ddmlib.testrunner.IRemoteAndroidTestRunner;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.unmodifiableMap;

/** Result summary of executing instrumentation on multiple devices. */
public final class SpoonSummary {
  private final String title;
  private final IRemoteAndroidTestRunner.TestSize testSize;
  private final long started;
  private final long duration;
  private final Map<String, DeviceResult> results;

  private SpoonSummary(String title, IRemoteAndroidTestRunner.TestSize testSize, long started,
      long duration, Map<String, DeviceResult> results) {
    this.title = title;
    this.testSize = testSize;
    this.started = started;
    this.duration = duration;
    this.results = unmodifiableMap(new HashMap<>(results));
  }

  /** Execution title. */
  public String getTitle() {
    return title;
  }

  /** Size of tests. */
  public IRemoteAndroidTestRunner.TestSize getTestSize() {
    return testSize;
  }

  /** Execution start time. */
  public long getStarted() {
    return started;
  }

  /** Length of overall execution, in seconds. */
  public long getDuration() {
    return duration;
  }

  /** Individual device results by serial number. */
  public Map<String, DeviceResult> getResults() {
    return results;
  }

  static class Builder {
    private final Map<String, DeviceResult> results = new HashMap<>();
    private String title;
    private IRemoteAndroidTestRunner.TestSize testSize;
    private long started;
    private long start;
    private long duration = -1;

    Builder setTitle(String title) {
      checkNotNull(title);
      checkState(this.title == null, "Title already set.");
      this.title = title;
      return this;
    }

    Builder setTestSize(IRemoteAndroidTestRunner.TestSize testSize) {
      checkNotNull(testSize);
      checkState(this.testSize == null, "Test size already set.");
      this.testSize = testSize;
      return this;
    }

    Builder addResult(String serial, DeviceResult result) {
      checkNotNull(serial);
      checkNotNull(result);
      checkState(start != 0, "Start must be called before results can be added.");
      synchronized (results) {
        checkArgument(!results.containsKey(serial), "Result for serial already added.");
        results.put(serial, result);
      }
      return this;
    }

    Builder start() {
      checkState(start == 0, "Start already called.");
      start = System.nanoTime();
      started = new Date().getTime();
      return this;
    }

    Builder end() {
      checkState(start != 0, "Start must be called before end.");
      checkState(duration == -1, "End already called.");
      duration = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start);
      return this;
    }

    SpoonSummary build() {
      checkState(title != null, "Title is required.");
      checkState(started != 0, "Never started.");

      return new SpoonSummary(title, testSize, started, duration, results);
    }
  }
}
