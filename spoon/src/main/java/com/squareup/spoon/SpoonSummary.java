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
  private final Date started;
  private final long length;
  private final Map<String, DeviceResult> results;

  private SpoonSummary(String title, Date started, long length, Map<String, DeviceResult> results) {
    this.title = title;
    this.started = started;
    this.length = length;
    this.results = unmodifiableMap(new HashMap<String, DeviceResult>(results));
  }

  /** Execution title. */
  public String getTitle() {
    return title;
  }

  /** Execution start time. */
  public Date getStarted() {
    return started;
  }

  /** Length of overall execution, in seconds. */
  public long getLength() {
    return length;
  }

  /** Individual device results by serial number. */
  public Map<String, DeviceResult> getResults() {
    return results;
  }

  static class Builder {
    private final Map<String, DeviceResult> results = new HashMap<String, DeviceResult>();
    private String title;
    private Date started;
    private long start;
    private long length = -1;

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
      started = new Date();
      return this;
    }

    Builder end() {
      checkArgument(start != 0, "Start must be called before end.");
      checkArgument(length == -1, "End already called.");
      length = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start);
      return this;
    }

    SpoonSummary build() {
      checkNotNull(title, "Title is required.");
      checkNotNull(started, "Never started.");

      return new SpoonSummary(title, started, length, results);
    }
  }
}
