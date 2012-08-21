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
  private static final String TAG_MANIFEST = "manifest";
  private static final String TAG_INSTRUMENTATION = "instrumentation";
  private static final String ATTR_PACKAGE = "package";
  private static final String ATTR_TARGET_PACKAGE = "targetPackage";

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
    String[] packages = getManifestPackages(config.test);
    final String appPackage = packages[0];
    final String testPackage = packages[1];
    logger.info("Target app manifest package: " + appPackage);
    logger.info("Target test manifest package: " + testPackage);

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
              ExecutionTarget target = new ExecutionTarget(sdkPath, config, appPackage, testPackage, device);
              ExecutionResult result = target.call();
              summary.results.add(result);
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

  private static String[] getManifestPackages(File apkTestFile) {
    InputStream is = null;
    try {
      ZipFile zip = new ZipFile(apkTestFile);
      ZipEntry entry = zip.getEntry(ANDROID_MANIFEST_XML);
      is = zip.getInputStream(entry);

      AXMLParser parser = new AXMLParser(is);
      int eventType = parser.getType();

      String[] ret = new String[2];
      while (eventType != AXMLParser.END_DOCUMENT) {
        if (eventType == AXMLParser.START_TAG) {
          String parserName = parser.getName();
          if (TAG_MANIFEST.equals(parserName) || TAG_INSTRUMENTATION.equals(parserName)) {
            for (int i = 0; i < parser.getAttributeCount(); i++) {
              String parserAttributeName = parser.getAttributeName(i);
              if (ATTR_PACKAGE.equals(parserAttributeName)) {
                ret[1] = parser.getAttributeValueString(i);
                break;
              }
              if (ATTR_TARGET_PACKAGE.equals(parserAttributeName)) {
                ret[0] = parser.getAttributeValueString(i);
                break;
              }
            }
          }
        }
        eventType = parser.next();
      }
      if (ret[0] == null || ret[1] == null) {
        throw new IllegalStateException("Unable to find both app and test pacakge.");
      }
      return ret;
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
