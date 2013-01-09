package com.squareup.spoon;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.madgag.gif.fmsware.AnimatedGifEncoder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import static com.android.ddmlib.FileListingService.FileEntry;
import static com.android.ddmlib.FileListingService.TYPE_DIRECTORY;
import static com.android.ddmlib.Log.LogLevel.DEBUG;
import static com.android.ddmlib.SyncService.ISyncProgressMonitor;
import static java.util.logging.Level.FINEST;
import static java.util.logging.Level.INFO;

/** Utilities for executing instrumentation tests on devices. */
final class SpoonUtils {
  static final Gson GSON = new GsonBuilder() //
      .registerTypeAdapter(File.class, new TypeAdapter<File>() {
        @Override public void write(JsonWriter jsonWriter, File file) throws IOException {
          if (file == null) {
            jsonWriter.nullValue();
          } else {
            jsonWriter.value(file.getAbsolutePath());
          }
        }

        @Override public File read(JsonReader jsonReader) throws IOException {
          return new File(jsonReader.nextString());
        }
      }) //
      .enableComplexMapKeySerialization() //
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

  static Logger getConfiguredLogger(Object instance, boolean debug) {
    Logger logger = Logger.getLogger(instance.getClass().getSimpleName());
    logger.setLevel(FINEST); // Keep track of all log messages.
    for (Handler handler : logger.getHandlers()) {
      // Only record higher than INFO for debug executions.
      handler.setLevel(debug ? FINEST : INFO);
    }
    return logger;
  }

  static void copyResourceToOutput(String resource, File output) {
    InputStream is = null;
    OutputStream os = null;
    try {
      is = SpoonUtils.class.getResourceAsStream("/" + resource);
      os = new FileOutputStream(new File(output, resource));
      IOUtils.copy(is, os);
    } catch (IOException e) {
      throw new RuntimeException("Unable to copy resource " + resource + " to " + output, e);
    } finally {
      IOUtils.closeQuietly(is);
      IOUtils.closeQuietly(os);
    }
  }

  /** Fetch or create a real device that corresponds to a device model. */
  static IDevice obtainRealDevice(AndroidDebugBridge adb, String serial) {
    // Get an existing real device.
    for (IDevice adbDevice : adb.getDevices()) {
      if (adbDevice.getSerialNumber().equals(serial)) {
        return adbDevice;
      }
    }
    throw new IllegalArgumentException("Unknown device serial: " + serial);
  }

  /** Get a {@link FileEntry} for an arbitrary path. */
  static FileEntry obtainDirectoryFileEntry(String path) {
    try {
      FileEntry lastEntry = null;
      Constructor<FileEntry> c =
          FileEntry.class.getDeclaredConstructor(FileEntry.class, String.class, int.class,
              boolean.class);
      c.setAccessible(true);
      for (String part : path.split("/")) {
        lastEntry = c.newInstance(lastEntry, part, TYPE_DIRECTORY, lastEntry == null);
      }
      return lastEntry;
    } catch (NoSuchMethodException ignored) {
    } catch (InvocationTargetException ignored) {
    } catch (InstantiationException ignored) {
    } catch (IllegalAccessException ignored) {
    }
    return null;
  }

  /** Turn on debug logging in ddmlib classes. */
  static void setDdmlibInternalLoggingLevel() {
    try {
      Field level = Log.class.getDeclaredField("mLevel");
      level.setAccessible(true);
      level.set(Log.class, DEBUG);
    } catch (NoSuchFieldException ignored) {
    } catch (IllegalAccessException ignored) {
    }
  }

  /** Find all device serials that are plugged in through ADB. */
  static Set<String> findAllDevices(AndroidDebugBridge adb) {
    Set<String> devices = new HashSet<String>();
    for (IDevice realDevice : adb.getDevices()) {
      devices.add(realDevice.getSerialNumber());
    }
    return devices;
  }

  /** Get an {@link com.android.ddmlib.AndroidDebugBridge} instance given an SDK path. */
  static AndroidDebugBridge initAdb(File sdk) {
    AndroidDebugBridge.init(false);
    File adbPath = FileUtils.getFile(sdk, "platform-tools", "adb");
    AndroidDebugBridge adb = AndroidDebugBridge.createBridge(adbPath.getAbsolutePath(), true);
    waitForAdb(adb);
    return adb;
  }

  static void createAnimatedGif(List<File> testScreenshots, File animatedGif) throws IOException {
    AnimatedGifEncoder encoder = new AnimatedGifEncoder();
    encoder.start(animatedGif.getAbsolutePath());
    encoder.setDelay(1000 /* 1 second */);
    encoder.setQuality(1 /* highest */);
    encoder.setRepeat(0 /* infinite */);
    for (File testScreenshot : testScreenshots) {
      encoder.addFrame(ImageIO.read(testScreenshot));
    }
    encoder.finish();
  }

  private static void waitForAdb(AndroidDebugBridge adb) {
    for (int i = 10; i > 0; i--) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      if (adb.isConnected()) {
        return;
      }
    }
    throw new RuntimeException("Unable to connect to adb.");
  }

  private SpoonUtils() {
    // No instances.
  }
}
