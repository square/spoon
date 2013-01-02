package com.squareup.spoon;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.apache.commons.io.FileUtils;

import static com.android.ddmlib.FileListingService.FileEntry;
import static com.android.ddmlib.FileListingService.TYPE_DIRECTORY;

/** Helper methods for dealing with {@code adb}. */
final class AdbHelper {
  private static final String PLATFORM_TOOLS = "platform-tools";
  private static final String ADB_BINARY = "adb";

  /** Get an {@link AndroidDebugBridge} instance given an SDK path. */
  static AndroidDebugBridge init(File sdk) {
    AndroidDebugBridge.init(false);
    File adbPath = FileUtils.getFile(sdk, PLATFORM_TOOLS, ADB_BINARY);
    AndroidDebugBridge adb = AndroidDebugBridge.createBridge(adbPath.getAbsolutePath(), true);
    waitForAdb(adb);
    return adb;
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

  private AdbHelper() {
    // No instances.
  }
}
