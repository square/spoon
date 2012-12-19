package com.squareup.spoon;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

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

    final CountDownLatch done = new CountDownLatch(targetCount);
    final ExecutionSummary.Builder summaryBuilder = new ExecutionSummary.Builder()
        .setTitle(title)
        .setOutputDirectory(output)
        .start();

    try {
      for (final String serial : serials) {
        new Thread(new Runnable() {
          @Override public void run() {
            // Create an empty result just in case the execution fails before target.call() returns.
            ExecutionResult result = new ExecutionResult(serial);
            try {
              ExecutionTarget target =
                  new ExecutionTarget(sdkPath, apk, testApk, output, serial, debug);
              result = target.call();
              summaryBuilder.addResult(result);
            } catch (FileNotFoundException e) {
              // No results file means fatal exception before it could be written.
              String outputFolder = FilenameUtils.concat(output.getName(), serial);
              if (e.getMessage().contains(FilenameUtils.concat(outputFolder,
                ExecutionTarget.FILE_RESULT))) {
                logger.severe(String.format(
                  "Fatal exception while running on %s, please check %s for exception.",
                  serial, FilenameUtils.concat(outputFolder, ExecutionTarget.OUTPUT_FILE)));
              } else {
                logger.severe(e.toString());
              }
            } catch (Exception e) {
              logger.severe(e.toString());
              result.setRuntimeException(e);
            } finally {
              done.countDown();
            }
          }
        }).start();
      }

      done.await();
    } catch (Exception e) {
      summaryBuilder.setException(e);
    }

    ExecutionSummary summary = summaryBuilder.end();

    // Write output files.
    summary.writeHtml();
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
