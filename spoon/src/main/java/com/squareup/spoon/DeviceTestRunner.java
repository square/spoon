package com.squareup.spoon;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.InstallException;
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;

import static com.android.ddmlib.FileListingService.FileEntry;
import static com.squareup.spoon.DdmlibHelper.obtainDirectoryFileEntry;
import static com.squareup.spoon.DdmlibHelper.obtainRealDevice;
import static com.squareup.spoon.Screenshot.SPOON_SCREENSHOTS;
import static com.squareup.spoon.Utils.GSON;
import static com.squareup.spoon.Utils.QUIET_MONITOR;
import static java.util.logging.Level.SEVERE;

/** Represents a single device and the test configuration to be executed. */
public class DeviceTestRunner {
  private static final String FILE_EXECUTION = "execution.json";
  private static final String FILE_RESULT = "result.json";

  private final File sdk;
  private final File apk;
  private final File testApk;
  private final String serial;
  private final boolean debug;
  private final File output;
  private final String classpath;
  private final InstrumentationManifestInfo instrumentationInfo;

  /**
   * Create a test runner for a single device.
   *
   * @param sdk Path to the local Android SDK directory.
   * @param apk Path to application APK.
   * @param testApk Path to test application APK.
   * @param output Path to output directory.
   * @param serial Device to run the test on.
   * @param debug Whether or not debug logging is enabled.
   * @param classpath Custom JVM classpath or {@code null}.
   * @param instrumentationInfo Test apk manifest information.
   */
  DeviceTestRunner(File sdk, File apk, File testApk, File output, String serial, boolean debug,
      String classpath, InstrumentationManifestInfo instrumentationInfo) {
    this.sdk = sdk;
    this.apk = apk;
    this.testApk = testApk;
    this.serial = serial;
    this.debug = debug;
    this.output = new File(output, serial);
    this.classpath = classpath;
    this.instrumentationInfo = instrumentationInfo;
  }

  /** Serialize ourself to disk and start {@link #main(String...)} in another process. */
  public ExecutionResult runInNewProcess() throws IOException, InterruptedException {
    // Create the output directory.
    output.mkdirs();

    // Write our configuration to a file in the output directory.
    FileWriter executionWriter = new FileWriter(new File(output, FILE_EXECUTION));
    GSON.toJson(this, executionWriter);
    executionWriter.close();

    // Kick off a new process to interface with ADB and perform the real execution.
    String name = DeviceTestRunner.class.getName();
    Process process = new ProcessBuilder("java", "-cp", classpath, name,
      output.getAbsolutePath()).start();
    process.waitFor();

    // Read the result from a file in the output directory.
    FileReader resultFile = new FileReader(new File(output, FILE_RESULT));
    ExecutionResult result = GSON.fromJson(resultFile, ExecutionResult.class);
    resultFile.close();

    return result;
  }

  /** Execute instrumentation on the target device and return a result summary. */
  public ExecutionResult run() {
    ExecutionResult result = new ExecutionResult(serial);

    String appPackage = instrumentationInfo.getApplicationPackage();
    String testPackage = instrumentationInfo.getInstrumentationPackage();
    String testRunner = instrumentationInfo.getTestRunnerClass();

    if (debug) {
      DdmlibHelper.setInternalLoggingLevel();
    }

    AndroidDebugBridge adb = DdmlibHelper.initAdb(sdk);

    IDevice realDevice = obtainRealDevice(adb, serial);
    result.configureFor(realDevice);

    // Install the main application and the testApk package.
    try {
      if (realDevice.installPackage(apk.getAbsolutePath(), true) != null) {
        result.setException(new RuntimeException("Unable to install application APK."));
        return result;
      }
      if (realDevice.installPackage(testApk.getAbsolutePath(), true) != null) {
        result.setException(new RuntimeException("Unable to install instrumentation APK."));
        return result;
      }
    } catch (InstallException e) {
      result.setException(e);
      return result; // Installation failed, exit early.
    }

    // Run all the tests! o/
    result.testStart = System.nanoTime();

    try {
      new RemoteAndroidTestRunner(testPackage, testRunner, realDevice).run(result);
    } catch (Exception e) {
      result.setException(e);
    }

    result.testEnd = System.nanoTime();
    result.testCompleted = new Date();

    try {
      // Sync device screenshots, if any, to the local filesystem.
      output.mkdirs();
      String dirName = "app_" + SPOON_SCREENSHOTS;
      String localDirName = output.getAbsolutePath();
      FileEntry deviceDir = obtainDirectoryFileEntry("/data/data/" + appPackage + "/" + dirName);
      realDevice.getSyncService().pull(new FileEntry[] {deviceDir}, localDirName, QUIET_MONITOR);

      File screenshotDir = new File(output, dirName);
      if (screenshotDir.exists()) {
        // Move all children of the screenshot directory into the output folder.
        File[] classNameDirs = screenshotDir.listFiles();
        if (classNameDirs != null) {
          for (File classNameDir : classNameDirs) {
            File destDir = new File(output, classNameDir.getName());
            FileUtils.deleteDirectory(destDir);
            FileUtils.moveDirectory(classNameDir, destDir);
            result.addScreenshotDirectory(destDir);
          }
        }
        FileUtils.deleteDirectory(screenshotDir);
      }
    } catch (Exception e) {
      result.setException(e);
    }

    return result;
  }

  /////////////////////////////////////////////////////////////////////////////
  ////  Secondary Per-Device Process  /////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////

  /** Deserialize ourselves from disk, run the tests, and serialize the result back to disk. */
  public static void main(String... args) {
    if (args.length != 1) {
      throw new IllegalArgumentException("Must be started with a device directory.");
    }

    Logger log = Logger.getLogger(DeviceTestRunner.class.getSimpleName());
    try {
      String outputDirName = args[0];
      File outputDir = new File(outputDirName);
      File executionFile = new File(outputDir, FILE_EXECUTION);
      if (!executionFile.exists()) {
        throw new IllegalArgumentException("Device directory and/or execution file doesn't exist.");
      }

      FileReader reader = new FileReader(executionFile);
      DeviceTestRunner target = GSON.fromJson(reader, DeviceTestRunner.class);
      reader.close();

      ExecutionResult result = target.run();
      if (result.getException() != null) {
        log.log(SEVERE, "Unable to execute test for target.\n\n%s", result.getException());
      }

      // Write device result file.
      FileWriter writer = new FileWriter(new File(outputDir, FILE_RESULT));
      GSON.toJson(result, writer);
      writer.close();
    } catch (Exception ex) {
      log.log(SEVERE, "Unable to execute test for target.", ex);
    }

    // Trigger a runtime exit (ensuring ADB connection is closed).
    System.exit(0);
  }
}
