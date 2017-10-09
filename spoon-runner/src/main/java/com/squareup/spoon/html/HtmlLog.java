package com.squareup.spoon.html;

import com.android.ddmlib.logcat.LogCatMessage;
import com.squareup.spoon.DeviceTest;
import com.squareup.spoon.DeviceTestResult;

import java.io.File;
import java.util.List;

import static java.util.stream.Collectors.toList;

/** Model for representing a {@code log.html} page. */
final class HtmlLog {
  public static HtmlLog from(String name, DeviceTest test, File rawFile, DeviceTestResult result) {
    String status;
    switch (result.getStatus()) {
      case PASS:
        status = "passed";
        break;
      case IGNORED:
        status = "ignored";
        break;
      case FAIL:
        status = "failed";
        break;
      case ASSUMPTION_FAILURE:
        status = "assumption violated";
        break;
      default:
        throw new IllegalArgumentException("Unknown status: " + result.getStatus());
    }

    String title = test.getMethodName();
    String subtitle = "Test " + status
        + " in " + HtmlUtils.humanReadableDuration(result.getDuration())
        + " on " + name;

    List<LogEntry> log = result.getLog().stream().map(LogEntry::from).collect(toList());

    return new HtmlLog(title, subtitle, rawFile.getAbsolutePath(), log);
  }

  public final String title;
  public final String subtitle;
  public final String rawFile;
  public final List<LogEntry> log;

  HtmlLog(String title, String subtitle, String rawFile, List<LogEntry> log) {
    this.title = title;
    this.subtitle = subtitle;
    this.rawFile = rawFile;
    this.log = log;
  }

  static class LogEntry {
    static LogEntry from(LogCatMessage message) {
      String rowClass;
      switch (message.getLogLevel()) {
        case ERROR:
          rowClass = "error";
          break;
        case WARN:
          rowClass = "warning";
          break;
        case INFO:
          rowClass = "info";
          break;
        default:
          rowClass = "";
      }

      String timestamp = message.getTimestamp().toString();
      String level = message.getLogLevel().getStringValue();
      return new LogEntry(rowClass, timestamp, level, message.getTag(), message.getMessage());
    }

    public final String rowClass;
    public final String timestamp;
    public final String level;
    public final String tag;
    public final String message;

    LogEntry(String rowClass, String timestamp, String level, String tag, String message) {
      this.rowClass = rowClass;
      this.timestamp = timestamp;
      this.level = level;
      this.tag = tag;
      this.message = message;
    }
  }
}
