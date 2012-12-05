package com.squareup.spoon;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;

/** Represents a collection of devices and the test configuration to be executed. */
public class ExecutionSuite implements Runnable {
  private final Logger logger;
  private final String title;
  private final String sdkPath;
  private final File apk;
  private final File testApk;
  private final File output;
  private final boolean debug;
  private final Collection<String> serials;

  /**
   * Create a test suite for the specified devices and configuration.
   *
   * @param title Identifying title for this execution.
   * @param sdkPath Path to the local Android SDK directory.
   * @param apk Path to application APK.
   * @param testApk Path to test application APK.
   * @param output Path to output directory.
   * @param debug Whether or not debug logging is enabled.
   */
  public ExecutionSuite(String title, String sdkPath, File apk, File testApk, File output,
      boolean debug) {
    this.logger = Logger.getLogger("Spoon");
    this.title = title;
    this.sdkPath = sdkPath;
    this.apk = apk;
    this.testApk = testApk;
    this.output = output;
    this.debug = debug;
    this.serials = findAllDevices(sdkPath);
  }

  @Override public void run() {
    int targetCount = serials.size();
    if (targetCount == 0) {
      logger.info("No devices.");
      return;
    }

    logger.info("Executing instrumentation on " + targetCount + " devices.");

    try {
      FileUtils.deleteDirectory(output);
    } catch (IOException e) {
      throw new RuntimeException("Unable to clean output directory: " + output, e);
    }

    final ExecutionSummary summary = new ExecutionSummary(title, output);
    final CountDownLatch done = new CountDownLatch(targetCount);

    summary.testStart = System.nanoTime();
    try {
      for (final String serial : serials) {
        new Thread(new Runnable() {
          @Override public void run() {
            try {
              ExecutionTarget target =
                  new ExecutionTarget(sdkPath, apk, testApk, output, serial, debug);
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
    summary.testEnd = System.nanoTime();
    summary.testCompleted = new Date();

    // Write output files.
    summary.generateHtml();
  }

  /** Find all device serials that are plugged in through ADB. */
  private static Collection<String> findAllDevices(String sdkPath) {
    List<String> devices = new ArrayList<String>();
    AndroidDebugBridge adb = AdbHelper.init(sdkPath);
    for (IDevice realDevice : adb.getDevices()) {
      devices.add(realDevice.getSerialNumber());
    }
    AndroidDebugBridge.terminate();
    return devices;
  }
}
