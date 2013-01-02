package com.squareup.spoon;

import com.android.ddmlib.AndroidDebugBridge;
import java.io.File;
import org.apache.commons.io.FileUtils;

/** Helper methods for dealing with {@code adb}. */
final class AdbHelper {
  private static final String PLATFORM_TOOLS = "platform-tools";
  private static final String ADB_BINARY = "adb";

  public static AndroidDebugBridge init(File sdk) {
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

  private AdbHelper() {
    // No instances.
  }
}
