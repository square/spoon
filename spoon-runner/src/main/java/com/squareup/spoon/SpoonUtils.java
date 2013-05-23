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
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.imageio.ImageIO;
import org.apache.commons.io.FileUtils;

import static com.android.ddmlib.FileListingService.FileEntry;
import static com.android.ddmlib.FileListingService.TYPE_DIRECTORY;
import static com.android.ddmlib.Log.LogLevel.DEBUG;

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
    Set<String> devices = new LinkedHashSet<String>();
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
    encoder.setDelay(1500 /* 1.5 seconds */);
    encoder.setQuality(1 /* highest */);
    encoder.setRepeat(0 /* infinite */);
    encoder.setTransparent(Color.WHITE);

    int width = 0;
    int height = 0;
    for (File testScreenshot : testScreenshots) {
      BufferedImage bufferedImage = ImageIO.read(testScreenshot);
      width = Math.max(bufferedImage.getWidth(), width);
      height = Math.max(bufferedImage.getHeight(), height);
    }
    encoder.setSize(width, height);

    for (File testScreenshot : testScreenshots) {
      encoder.addFrame(ImageIO.read(testScreenshot));
    }

    encoder.finish();
  }

  private static void waitForAdb(AndroidDebugBridge adb) {
    for (int i = 1; i < 10; i++) {
      try {
        Thread.sleep(i * 100);
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
