package com.squareup.spoon;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.unmodifiableMap;

/** Result summary of executing instrumentation on multiple devices. */
public final class SpoonSummary {
  private final String title;
  private final long started;
  private final long duration;
  private final Map<String, DeviceResult> results;

  private SpoonSummary(String title, long started, long duration,
      Map<String, DeviceResult> results) {
    this.title = title;
    this.started = started;
    this.duration = duration;
    this.results = unmodifiableMap(new HashMap<String, DeviceResult>(results));
  }

  /** Execution title. */
  public String getTitle() {
    return title;
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
    private final Map<String, DeviceResult> results = new HashMap<String, DeviceResult>();
    private String title;
    private long started;
    private long start;
    private long duration = -1;

    Builder setTitle(String title) {
      checkNotNull(title);
      checkArgument(title != null, "Title already set.");
      this.title = title;
      return this;
    }

    Builder addResult(String serial, DeviceResult result) {
      checkNotNull(serial);
      checkNotNull(result);
      checkArgument(start != 0, "Start must be called before results can be added.");
      synchronized (results) {
        checkArgument(!results.containsKey(serial), "Result for serial already added.");
        results.put(serial, result);
      }
      return this;
    }

    Builder start() {
      checkArgument(start == 0, "Start already called.");
      start = System.nanoTime();
      started = new Date().getTime();
      return this;
    }

    Builder end() {
      checkArgument(start != 0, "Start must be called before end.");
      checkArgument(duration == -1, "End already called.");
      duration = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start);
      return this;
    }

    SpoonSummary build() {
      checkNotNull(title, "Title is required.");
      checkNotNull(started, "Never started.");

      return new SpoonSummary(title, started, duration, results);
    }
  }
}
