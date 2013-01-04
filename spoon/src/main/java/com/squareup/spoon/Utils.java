package com.squareup.spoon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.File;
import java.io.IOException;
import java.util.logging.Handler;
import java.util.logging.Logger;

import static com.android.ddmlib.SyncService.ISyncProgressMonitor;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.INFO;

final class Utils {
  static final Gson GSON = new GsonBuilder() //
      .registerTypeAdapter(File.class, new TypeAdapter<File>() {
        @Override public void write(JsonWriter jsonWriter, File file) throws IOException {
          jsonWriter.value(file.getAbsolutePath());
        }

        @Override public File read(JsonReader jsonReader) throws IOException {
          return new File(jsonReader.nextString());
        }
      }) //
      .setPrettyPrinting() //
      .create();

  static final ISyncProgressMonitor QUIET_MONITOR = new ISyncProgressMonitor() {
        @Override public void start(int totalWork) {
        }

        @Override public void stop() {
        }

        @Override public boolean isCanceled() {
          return false;
        }

        @Override public void startSubTask(String name) {
        }

        @Override public void advance(int work) {
        }
      };

  /** Fake Class#getSimpleName logic. */
  static String getClassSimpleName(String className) {
    int lastPeriod = className.lastIndexOf(".");
    if (lastPeriod != -1) {
      return className.substring(lastPeriod + 1);
    }
    return className;
  }

  /** Convert a test name from {@code testThisThing_DoesThat} to "This Thing, Does That". */
  static String prettifyTestName(String testName) {
    if (!testName.startsWith("test")) {
      throw new IllegalArgumentException("Test name does not start with 'test'.");
    }
    StringBuilder pretty = new StringBuilder();
    String[] parts = testName.substring(4).split("_");
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

  /** Convert an image tag from {@code this-here-is-it} to "This Here Is It". */
  static String prettifyImageName(String imageName) {
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

  static Logger getConfiguredLogger(Object instance, boolean debug) {
    Logger logger = Logger.getLogger(instance.getClass().getSimpleName());
    logger.setLevel(FINEST); // Keep track of all log messages.
    for (Handler handler : logger.getHandlers()) {
      // Only record higher than INFO for debug executions.
      handler.setLevel(debug ? FINEST : INFO);
    }
    return logger;
  }

  private Utils() {
    // No instances.
  }
}
