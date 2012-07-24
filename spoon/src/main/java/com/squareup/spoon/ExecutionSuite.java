package com.squareup.spoon;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.squareup.spoon.external.AXMLParser;
import com.squareup.spoon.model.Device;
import com.squareup.spoon.model.RunConfig;
import org.apache.commons.io.FileUtils;

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

    int targetCount = devices.size();
    if (targetCount == 0) {
      logger.info("No devices.");
      return;
    }

    logger.info("Executing instrumentation on " + targetCount + " devices.");

    try {
      FileUtils.deleteDirectory(config.output);
    } catch (IOException e) {
      throw new RuntimeException("Unable to clean output directory: " + config.output, e);
    }

    final ExecutionSummary summary = new ExecutionSummary(config);
    final CountDownLatch done = new CountDownLatch(targetCount);

    summary.testStart = System.currentTimeMillis();
    try {
      for (final Device device : devices) {
        new Thread(new Runnable() {
          @Override public void run() {
            try {
              ExecutionTarget target = new ExecutionTarget(sdkPath, config, testManifestPackage, device);
              summary.results.add(target.call());
            } catch (Exception e) {
              summary.exceptions.add(e);
            } finally {
              done.countDown();
            }
          }
        }).start();
      }

      done.await();
    } catch (Exception e) {
      summary.exceptions.add(e);
    }
    summary.testEnd = System.currentTimeMillis();

    // Write output files.
    summary.generateHtml();
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
      throw new RuntimeException("Unable to parse test app AndroidManifest.xml.", e);
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
