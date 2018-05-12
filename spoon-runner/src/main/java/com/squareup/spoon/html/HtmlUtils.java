package com.squareup.spoon.html;

import com.squareup.spoon.DeviceDetails;
import com.squareup.spoon.DeviceTestResult;
import com.squareup.spoon.misc.StackTrace;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.File;
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.stream.Collectors.toList;

/** Utilities for representing the execution in HTML. */
final class HtmlUtils {
  private static final String INVALID_ID_CHARS = "[^a-zA-Z0-9]";
  // TODO use local date/time format instances instead?
  private static final ThreadLocal<Format> DATE_FORMAT =
      ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US));
  private static final ThreadLocal<Format> DATE_FORMAT_TV =
      ThreadLocal.withInitial(() -> new SimpleDateFormat("EEEE, MMMM dd, h:mm a", Locale.US));

  static String deviceDetailsToString(DeviceDetails details) {
    if (details == null) return null;

    StringBuilder builder = new StringBuilder();
    builder.append("Running Android ")
        .append(details.getVersion())
        .append(" (API ")
        .append(details.getApiLevel())
        .append(")");

    if (details.getLanguage() != null || details.getRegion() != null) {
      builder.append(" with locale ");
      if (details.getLanguage() != null) {
        builder.append(details.getLanguage());
        if (details.getRegion() != null) {
          builder.append("-");
        }
        if (details.getRegion() != null) {
          builder.append(details.getRegion());
        }
      }
    }

    return builder.toString();
  }

  static String dateToString(long date) {
    return DATE_FORMAT.get().format(new Date(date));
  }

  public static String dateToTvString(long date) {
    return DATE_FORMAT_TV.get().format(new Date(date));
  }

  /** Convert a class name and method name to a single HTML ID. */
  static String testClassAndMethodToId(String className, String methodName) {
    return className.replaceAll(INVALID_ID_CHARS, "-") + "-" //
        + methodName.replaceAll(INVALID_ID_CHARS, "-");
  }

  /** Fake Class#getSimpleName logic. */
  static String getClassSimpleName(String className) {
    int lastPeriod = className.lastIndexOf(".");
    if (lastPeriod != -1) {
      return className.substring(lastPeriod + 1);
    }
    return className;
  }

  /** Convert a test result status into an HTML CSS class. */
  static String getStatusCssClass(DeviceTestResult testResult) {
    String status;
    switch (testResult.getStatus()) {
      case PASS:
        status = "pass";
        break;
      case IGNORED:
        status = "ignored";
        break;
      case FAIL:
        status = "fail";
        break;
      case ASSUMPTION_FAILURE:
        status = "assumption-violation";
        break;
      default:
        throw new IllegalArgumentException("Unknown result status: " + testResult.getStatus());
    }
    return status;
  }

  /** Get a relative URI for {@code file} from {@code output} folder. */
  static String createRelativeUri(File file, File output) {
    if (file == null) {
      return null;
    }
    try {
      file = file.getCanonicalFile();
      output = output.getCanonicalFile();
      if (file.equals(output)) {
        throw new IllegalArgumentException("File path and output folder are the same.");
      }
      StringBuilder builder = new StringBuilder();
      while (!file.equals(output)) {
        if (builder.length() > 0) {
          builder.insert(0, "/");
        }
        builder.insert(0, file.getName());
        file = file.getParentFile().getCanonicalFile();
      }
      return builder.toString();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /** Get a HTML representation of a screenshot with respect to {@code output} directory. */
  static Screenshot getScreenshot(File screenshot, File output) {
    String relativePath = createRelativeUri(screenshot, output);
    String caption = screenshot.getName();
    return new Screenshot(relativePath, caption);
  }

  public static HtmlUtils.SavedFile getFile(File file, File output) {
    return new SavedFile(createRelativeUri(file, output), file.getName());
  }

  /** Parse the string representation of an exception to a {@link ExceptionInfo} instance. */
  static ExceptionInfo processStackTrace(StackTrace exception) {
    if (exception == null) {
      return null;
    }
    // Escape any special HTML characters in the exception that would otherwise break the HTML
    // rendering (e.g. the angle brackets around the default toString() for enums).
    String message = StringEscapeUtils.escapeHtml4(exception.toString());

    // Newline characters are usually stripped out by the parsing code in
    // {@link StackTrace#from(String)}, but they can sometimes remain (e.g. when the stack trace
    // is not in an expected format).  This replacement needs to be done after any HTML escaping.
    message = message.replace("\n", "<br/>");

    List<String> lines = exception.getElements()
        .stream()
        .map(element -> "&nbsp;&nbsp;&nbsp;&nbsp;at " + element.toString())
        .collect(toList());
    while (exception.getCause() != null) {
      exception = exception.getCause();
      String causeMessage = StringEscapeUtils.escapeHtml4(exception.toString());
      lines.add("Caused by: " + causeMessage.replace("\n", "<br/>"));
    }
    return new ExceptionInfo(message, lines);
  }

  static String humanReadableDuration(long length) {
    long minutes = length / 60;
    long seconds = length - (minutes * 60);
    StringBuilder builder = new StringBuilder();
    if (minutes != 0) {
      builder.append(minutes).append(" minute");
      if (minutes != 1) {
        builder.append("s");
      }
    }
    if (seconds != 0 || minutes == 0) {
      if (builder.length() > 0) {
        builder.append(", ");
      }
      builder.append(seconds).append(" second");
      if (seconds != 1) {
        builder.append("s");
      }
    }
    return builder.toString();
  }

  static final class Screenshot {
    private static final AtomicLong ID = new AtomicLong(0);

    public final long id;
    public final String path;
    public final String caption;

    Screenshot(String path, String caption) {
      this.id = ID.getAndIncrement();
      this.path = path;
      this.caption = caption;
    }
  }

  static final class SavedFile {
    private static final AtomicLong ID = new AtomicLong(0);
    private final long id;
    public final String path;
    public final String name;

    SavedFile(String path, String name) {
      this.id = ID.incrementAndGet();
      this.path = path;
      this.name = name;
    }
  }

  static final class ExceptionInfo {
    private static final AtomicLong ID = new AtomicLong(0);

    public final long id;
    public final String title;
    public final List<String> body;

    ExceptionInfo(String title, List<String> body) {
      this.id = ID.getAndIncrement();
      this.title = title;
      this.body = body;
    }
  }
}
