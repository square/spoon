package com.squareup.spoon;

import com.android.ddmlib.AndroidDebugBridge;

/** Helper methods for dealing with {@code adb}. */
final class AdbHelper {
  private static final String PLATFORM_TOOLS_ADB = "/platform-tools/adb";

  public static AndroidDebugBridge init(String sdkPath) {
    AndroidDebugBridge.init(false);
    AndroidDebugBridge adb = AndroidDebugBridge.createBridge(sdkPath + PLATFORM_TOOLS_ADB, true);
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
