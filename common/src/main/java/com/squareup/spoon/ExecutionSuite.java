package com.squareup.spoon;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.squareup.spoon.external.AXMLParser;
import com.squareup.spoon.model.Device;
import com.squareup.spoon.model.RunConfig;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Represents a collection of devices and the test configuration to be executed. */
public class ExecutionSuite implements Runnable {
  private static final String ANDROID_MANIFEST_XML = "AndroidManifest.xml";
  private static final String MANIFEST = "manifest";
  private static final String PACKAGE = "package";

  private final Logger logger;
  private final String sdkPath;
  private final RunConfig config;
  private final Collection<Device> devices;

  /**
   * Create a test suite for the specified devices and configuration.
   *
   * @param sdkPath Path to the local Android SDK directory.
   * @param config Test run configuration.
   * @param devices List of devices to run the tests on.
   * @param includeAllPhysical Whether or not to include all physical devices in the suite.
   */
  public ExecutionSuite(String sdkPath, RunConfig config, Collection<Device> devices, boolean includeAllPhysical) {
    this.logger = Logger.getLogger("Spoon");
    this.sdkPath = sdkPath;
    this.config = config;
    this.devices = devices;

    if (includeAllPhysical) {
      devices.addAll(findAllDevices(sdkPath));
    }
  }

  @Override public void run() {
    final String testManifestPackage = getTestManifestPackage(config.test);
    logger.info("Target test manifest package: " + testManifestPackage);

    final int targetCount = devices.size();
    if (targetCount == 0) {
      logger.info("No devices.");
      return;
    }

    logger.info("Executing instrumentation on " + targetCount + " devices.");

    deletePath(config.output);

    final CountDownLatch done = new CountDownLatch(targetCount);
    try {
      for (final Device device : devices) {
        new Thread(new Runnable() {
          @Override public void run() {
            try {
              ExecutionTarget target = new ExecutionTarget(sdkPath, config, testManifestPackage, device);
              ExecutionResult result = target.call();
              //TODO aggregate result
            } catch (InterruptedException e) {
              e.printStackTrace();
            } catch (IOException e) {
              e.printStackTrace();
            } finally {
              done.countDown();
            }
          }
        }).start();
      }

      done.await();

      // TODO assemble final index.html
    } catch (Exception e) {
      e.printStackTrace();
      // TODO record exception
    }
  }

  /** Recursively delete a directory. */
  private static void deletePath(File path) {
    if (path.isDirectory()) {
      for (File childPath : path.listFiles()) {
        deletePath(childPath);
      }
    }
    path.delete();
  }

  private static String getTestManifestPackage(File apkTestFile) {
    InputStream is = null;
    try {
      ZipFile zip = new ZipFile(apkTestFile);
      ZipEntry entry = zip.getEntry(ANDROID_MANIFEST_XML);
      is = zip.getInputStream(entry);

      AXMLParser parser = new AXMLParser(is);
      int eventType = parser.getType();

      while (eventType != AXMLParser.END_DOCUMENT) {
        if (eventType == AXMLParser.START_TAG) {
          if (MANIFEST.equals(parser.getName())) {
            for (int i = 0; i < parser.getAttributeCount(); i++) {
              if (PACKAGE.equals(parser.getAttributeName(i))) {
                return parser.getAttributeValueString(i);
              }
            }
          }
        }
        eventType = parser.next();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          // Ignored.
        }
      }
    }
    throw new IllegalArgumentException("Could not locate manifest package name from " + apkTestFile.getAbsolutePath());
  }

  /** Find all devices that are plugged in through ADB. */
  private static Collection<Device> findAllDevices(String sdkPath) {
    List<Device> devices = new ArrayList<Device>();
    AndroidDebugBridge adb = AdbHelper.init(sdkPath);
    for (IDevice realDevice : adb.getDevices()) {
      Device device = new Device();
      device.serial = realDevice.getSerialNumber();
      devices.add(device);
    }
    AndroidDebugBridge.terminate();
    return devices;
  }
}
