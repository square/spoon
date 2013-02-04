package com.squareup.spoon;

import java.io.File;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static com.squareup.spoon.Spoon.NAME_SEPARATOR;

/** Utilities for representing the execution in HTML. */
final class HtmlUtils {
  private static final String INVALID_ID_CHARS = "[^a-zA-Z0-9]";
  private static final ThreadLocal<Format> DATE_FORMAT = new ThreadLocal<Format>() {
    @Override protected Format initialValue() {
      return new SimpleDateFormat("yyyy-MM-dd hh:mm a");
    }
  };

  static String dateToString(long date) {
    return DATE_FORMAT.get().format(new Date(date));
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
      case FAIL:
        status = "fail";
        break;
      case ERROR:
        status = "error";
        break;
      default:
        throw new IllegalArgumentException("Unknown result status: " + testResult.getStatus());
    }
    return status;
  }

  /** Convert a method name from {@code testThisThing_DoesThat} to "This Thing, Does That". */
  static String prettifyMethodName(String methodName) {
    if (!methodName.startsWith("test")) {
      throw new IllegalArgumentException("Method name does not start with 'test'.");
    }
    StringBuilder pretty = new StringBuilder();
    String[] parts = methodName.substring(4).split("_");
    for (String part : parts) {
      if ("".equals(part.trim())) {
        continue; // Skip empty parts.
      }
      if (pretty.length() > 0) {
        pretty.append(",");
      }
      boolean inUpper = true;
      for (char letter : part.toCharArray()) {
        boolean isUpper = Character.isUpperCase(letter);
        if (!isUpper && inUpper && pretty.length() > 1 //
            && pretty.charAt(pretty.length() - 2) != ' ') {
          // Lowercase coming from an uppercase, insert a space before uppercase if not present.
          pretty.insert(pretty.length() - 1, " ");
        } else if (isUpper && !inUpper) {
          // Uppercase coming from a lowercase, add a space.
          pretty.append(" ");
        }
        inUpper = isUpper; // Update current upper/lower status.
        pretty.append(letter); // Append ourselves!
      }
    }
    return pretty.toString();
  }

  /** Convert an image name from {@code 87243508_this-here-is-it} to "This Here Is It". */
  static String prettifyImageName(String imageName) {
    // Remove the timestamp information.
    imageName = imageName.split(NAME_SEPARATOR, 2)[1];

    StringBuilder pretty = new StringBuilder();
    for (String part : imageName.replace('_', '-').split("-")) {
      if ("".equals(part.trim())) {
        continue; // Skip empty parts.
      }

      pretty.append(Character.toUpperCase(part.charAt(0)));
      pretty.append(part, 1, part.length());
      pretty.append(" ");
    }

    return pretty.deleteCharAt(pretty.length() - 1).toString();
  }

  /** Get a relative URI for {@code file} from {@code output} folder. */
  static String createRelativeUri(File file, File output) {
    if (file == null) {
      return null;
    }
    if (file.equals(output)) {
      throw new IllegalArgumentException("File path and output folder are the same.");
    }
    StringBuilder builder = new StringBuilder();
    while (!file.equals(output)) {
      if (builder.length() > 0) {
        builder.insert(0, "/");
      }
      builder.insert(0, file.getName());
      file = file.getParentFile();
    }
    return builder.toString();
  }

  /** Get a HTML representation of a screenshot with respect to {@code output} directory. */
  static Screenshot getScreenshot(File screenshot, File output) {
    String relativePath = createRelativeUri(screenshot, output);
    String caption = prettifyImageName(screenshot.getName());
    return new Screenshot(relativePath, caption);
  }

  /** Parse the string representation of an exception to a {@link StackTrace} instance. */
  static StackTrace parseException(String exception) {
    if (exception == null) {
      return null;
    }
    String[] lines = exception.replaceAll("\r\n", "\n").split("\n");
    String title = lines[0];
    return new StackTrace(title, Arrays.asList(lines).subList(1, lines.length));
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
  static final class StackTrace {
    private static final AtomicLong ID = new AtomicLong(0);

    public final long id;
    public final String title;
    public final List<String> body;

    StackTrace(String title, List<String> body) {
      this.id = ID.getAndIncrement();
      this.title = title;
      this.body = body;
    }
  }
}
