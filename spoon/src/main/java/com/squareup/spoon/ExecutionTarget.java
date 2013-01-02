package com.squareup.spoon;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.SyncException;
import com.android.ddmlib.TimeoutException;
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import static com.android.ddmlib.FileListingService.FileEntry;
import static com.squareup.spoon.DdmlibHelper.obtainDirectoryFileEntry;
import static com.squareup.spoon.DdmlibHelper.obtainRealDevice;
import static com.squareup.spoon.Screenshot.SPOON_SCREENSHOTS;
import static com.squareup.spoon.Utils.GSON;
import static com.squareup.spoon.Utils.QUIET_MONITOR;

/** Represents a single device and the test configuration to be executed. */
public class ExecutionTarget implements Callable<ExecutionResult> {
  static final String FILE_RESULT = "result.json";
  static final String OUTPUT_FILE = "output.txt";
  private static final String FILE_EXECUTION = "execution.json";

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
  ExecutionTarget(File sdk, File apk, File testApk, File output, String serial,
      boolean debug, String classpath, InstrumentationManifestInfo instrumentationInfo) {
    this.sdk = sdk;
    this.apk = apk;
    this.testApk = testApk;
    this.serial = serial;
    this.debug = debug;
    this.output = new File(output, serial);
    this.classpath = classpath;
    this.instrumentationInfo = instrumentationInfo;
  }

  /////////////////////////////////////////////////////////////////////////////
  ////  Main ExecutionSuite Process  //////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////

  /** Serialize ourself to disk and start {@link #main(String...)} in another process. */
  @Override public ExecutionResult call() throws IOException, InterruptedException {
    // Create the output directory.
    output.mkdirs();

    // Write our configuration to a file in the output directory.
    FileWriter execution = new FileWriter(new File(output, FILE_EXECUTION));
    GSON.toJson(this, execution);
    execution.close();

    // Kick off a new process to interface with ADB and perform the real execution.
    String name = ExecutionTarget.class.getName();
    Process process = new ProcessBuilder("java", "-cp", classpath, name,
      output.getAbsolutePath()).start();
    process.waitFor();
    IOUtils.copy(process.getErrorStream(), System.out);

    return GSON.fromJson(new FileReader(new File(output, FILE_RESULT)), ExecutionResult.class);
  }

  /////////////////////////////////////////////////////////////////////////////
  ////  Secondary Per-Device Process  /////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////

  public static void main(String... args)
      throws IOException, ShellCommandUnresponsiveException, AdbCommandRejectedException,
      TimeoutException, SyncException {
    Logger log = Logger.getLogger(ExecutionTarget.class.getSimpleName());
    try {
      if (args.length != 1) {
        throw new IllegalArgumentException("Must be started with a device directory.");
      }

      String outputDirName = args[0];
      File outputDir = new File(outputDirName);
      File executionFile = new File(outputDir, FILE_EXECUTION);
      if (!executionFile.exists()) {
        throw new IllegalArgumentException("Device directory and/or execution file doesn't exist.");
      }

      ExecutionTarget target = GSON.fromJson(new FileReader(executionFile), ExecutionTarget.class);
      ExecutionResult result = new ExecutionResult(target.serial);

      FileHandler handler = new FileHandler(new File(outputDir, OUTPUT_FILE).getAbsolutePath());
      handler.setFormatter(new SimpleFormatter());
      log.addHandler(handler);
      log.setLevel(target.debug ? Level.FINE : Level.INFO);

      final String appPackage = target.instrumentationInfo.getApplicationPackage();
      final String testPackage = target.instrumentationInfo.getInstrumentationPackage();
      final String testRunner = target.instrumentationInfo.getTestRunnerClass();

      if (target.debug) {
        DdmlibHelper.setInternalLoggingLevel();
      }

      AndroidDebugBridge adb = DdmlibHelper.init(target.sdk);

      IDevice realDevice = obtainRealDevice(adb, target.serial);
      result.configureFor(realDevice);

      // Install the main application and the testApk package.
      realDevice.installPackage(target.apk.getAbsolutePath(), true);
      realDevice.installPackage(target.testApk.getAbsolutePath(), true);

      // Run all the tests! o/
      result.testStart = System.nanoTime();
      new RemoteAndroidTestRunner(testPackage, testRunner, realDevice).run(result);
      result.testEnd = System.nanoTime();
      result.testCompleted = new Date();

      // Sync device screenshots, if any, to the local filesystem.
      String dirName = "app_" + SPOON_SCREENSHOTS;
      FileEntry deviceDir = obtainDirectoryFileEntry("/data/data/" + appPackage + "/" + dirName);
      realDevice.getSyncService().pull(new FileEntry[] {deviceDir}, outputDirName, QUIET_MONITOR);

      File screenshotDir = new File(outputDir, dirName);
      if (screenshotDir.exists()) {
        // Move all children of the screenshot directory into the output folder.
        File[] classNameDirs = screenshotDir.listFiles();
        if (classNameDirs != null) {
          for (File classNameDir : classNameDirs) {
            File destDir = new File(outputDir, classNameDir.getName());
            FileUtils.deleteDirectory(destDir);
            FileUtils.moveDirectory(classNameDir, destDir);
            result.addScreenshotDirectory(destDir);
          }
        }
        FileUtils.deleteDirectory(screenshotDir);
      }

      // Write device result file.
      FileWriter writer = new FileWriter(new File(outputDir, FILE_RESULT));
      GSON.toJson(result, writer);
      writer.close();
    } catch (IllegalArgumentException ex) {
      // Arguments thrown by us, log them before dying.
      log.severe(ex.getMessage());
    } catch (Exception ex) {
      log.throwing(ExecutionTarget.class.getSimpleName(), "main", ex);
    } finally {
      try {
        AndroidDebugBridge.terminate();
      } catch (Exception ignore) {
      }
    }
  }
}
