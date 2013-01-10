// Copyright 2013 Square, Inc.
package com.squareup.spoon;

import java.io.File;

final class RootDetection {
  private static final String[] SU_LOCATIONS = {
      "/sbin/su",
      "/system/bin/su",
      "/system/xbin/su",
      "/data/local/xbin/su",
      "/data/local/bin/su",
      "/system/sd/xbin/su"
  };

  static boolean detectIfRooted() {
    for (String suLocation : SU_LOCATIONS) {
      if (new File(suLocation).exists()) {
        return true;
      }
    }
    return false;
  }
}
