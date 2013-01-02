package com.squareup.spoon;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

final class Utils {
  /** Find all device serials that are plugged in through ADB. */
  static Set<String> findAllDevices(File sdkPath) {
    Set<String> devices = new HashSet<String>();
    AndroidDebugBridge adb = AdbHelper.init(sdkPath);
    for (IDevice realDevice : adb.getDevices()) {
      devices.add(realDevice.getSerialNumber());
    }
    AndroidDebugBridge.terminate();
    return devices;
  }
}
