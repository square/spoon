package com.squareup.spoon;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.InstallException;
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import static com.android.ddmlib.FileListingService.FileEntry;
import static com.squareup.spoon.Spoon.SPOON_SCREENSHOTS;
import static com.squareup.spoon.SpoonLogger.logDebug;
import static com.squareup.spoon.SpoonLogger.logInfo;
import static com.squareup.spoon.SpoonUtils.GSON;
import static com.squareup.spoon.SpoonUtils.QUIET_MONITOR;
import static com.squareup.spoon.SpoonUtils.createAnimatedGif;
import static com.squareup.spoon.SpoonUtils.obtainDirectoryFileEntry;
import static com.squareup.spoon.SpoonUtils.obtainRealDevice;

/** Represents a single device and the test configuration to be executed. */
public final class SpoonDeviceRunner {
  private static final String FILE_EXECUTION = "execution.json";
  private static final String FILE_RESULT = "result.json";
  static final String TEMP_DIR = "work";

  private final File sdk;
  private final File apk;
  private final File testApk;
  private final String serial;
  private final boolean debug;
  private final File output;
  private final String className;
  private final String methodName;
  private final File work;
  private final String classpath;
  private final SpoonInstrumentationInfo instrumentationInfo;

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
   * @param className Test class name to run or {@code null} to run all tests.
   * @param methodName Test method name to run or {@code null} to run all tests.  Must also pass
   *     {@code className}.
   */
  SpoonDeviceRunner(File sdk, File apk, File testApk, File output, String serial, boolean debug,
      String classpath, SpoonInstrumentationInfo instrumentationInfo, String className,
      String methodName) {
    this.sdk = sdk;
    this.apk = apk;
    this.testApk = testApk;
    this.serial = serial;
    this.debug = debug;
    this.output = output;
    this.className = className;
    this.methodName = methodName;
    this.work = FileUtils.getFile(output, TEMP_DIR, serial);
    this.classpath = classpath;
    this.instrumentationInfo = instrumentationInfo;
  }

  /** Serialize ourself to disk and start {@link #main(String...)} in another process. */
  public DeviceResult runInNewProcess() throws IOException, InterruptedException {
    logDebug(debug, "SpoonDeviceRunner.runInNewProcess for [%s]", serial);

    // Create the output directory.
    work.mkdirs();

    // Write our configuration to a file in the output directory.
    FileWriter executionWriter = new FileWriter(new File(work, FILE_EXECUTION));
    GSON.toJson(this, executionWriter);
    executionWriter.close();

    // Kick off a new process to interface with ADB and perform the real execution.
    String name = SpoonDeviceRunner.class.getName();
    //logDebug(debug, "[%s] java -cp %s %s %s", serial, classpath, name, work.getAbsolutePath());
    Process process =
        new ProcessBuilder("java", "-cp", classpath, name, work.getAbsolutePath()).start();
    final int exitCode = process.waitFor();
    printStream(process.getInputStream(), "STDOUT");
    printStream(process.getErrorStream(), "STDERR");
    logDebug(debug, "Process.waitFor() finished for [%s] with exitCode %d", serial, exitCode);

    // Read the result from a file in the output directory.
    FileReader resultFile = new FileReader(new File(work, FILE_RESULT));
    DeviceResult result = GSON.fromJson(resultFile, DeviceResult.class);
    resultFile.close();

    return result;
  }

  private void printStream(InputStream stream, String tag) throws IOException {
    BufferedReader stdout = new BufferedReader(new InputStreamReader(stream));
    String s;
    while ((s = stdout.readLine()) != null) {
      logDebug(debug, "[%s] %s %s", serial, tag, s);
    }
  }

  /** Execute instrumentation on the target device and return a result summary. */
  public DeviceResult run(AndroidDebugBridge adb) {
    String appPackage = instrumentationInfo.getApplicationPackage();
    String testPackage = instrumentationInfo.getInstrumentationPackage();
    String testRunner = instrumentationInfo.getTestRunnerClass();
    logDebug(debug, "InstrumentationInfo: [%s]", instrumentationInfo);

    if (debug) {
      SpoonUtils.setDdmlibInternalLoggingLevel();
    }

    DeviceResult.Builder result = new DeviceResult.Builder();

    IDevice device = obtainRealDevice(adb, serial);
    logDebug(debug, "SpoonDeviceRunner.run got realDevice for [%s]", serial);

    // Get relevant device information.
    result.setDeviceDetails(DeviceDetails.createForDevice(device));
    logDebug(debug, "SpoonDeviceRunner.run setDeviceDetails for [%s]", serial);

    // Install the main application and the instrumentation application.
    try {
      String installError = device.installPackage(apk.getAbsolutePath(), true);
      if (installError != null) {
        logInfo("[%s] app apk install failed.  Error [%s]", serial, installError);
        return result.markInstallAsFailed("Unable to install application APK.").build();
      }
      installError = device.installPackage(testApk.getAbsolutePath(), true);
      if (installError != null) {
        logInfo("[%s] test apk install failed.  Error [%s]", serial, installError);
        return result.markInstallAsFailed("Unable to install instrumentation APK.").build();
      }
    } catch (InstallException e) {
      logInfo("SpoonDeviceRunner.run got an InstallException on device [%s]", serial);
      e.printStackTrace();
      return result.markInstallAsFailed(e.getMessage()).build();
    }

    // Initiate device logging.
    SpoonDeviceLogger deviceLogger = new SpoonDeviceLogger(device);

    // Run all the tests! o/
    try {
      logDebug(debug, "About to actually run tests for [%s]", serial);
      RemoteAndroidTestRunner runner = new RemoteAndroidTestRunner(testPackage, testRunner, device);
      if (!Strings.isNullOrEmpty(className)) {
        if (Strings.isNullOrEmpty(methodName)) {
          runner.setClassName(className);
        } else {
          runner.setMethodName(className, methodName);
        }
      }
      runner.run(new SpoonTestRunListener(result, debug));
    } catch (Exception e) {
      result.addException(e);
    }

    // Grab all the parsed logs and map them to individual tests.
    Map<DeviceTest, List<DeviceLogMessage>> logs = deviceLogger.getParsedLogs();
    for (Map.Entry<DeviceTest, List<DeviceLogMessage>> entry : logs.entrySet()) {
      DeviceTestResult.Builder builder = result.getMethodResultBuilder(entry.getKey());
      if (builder != null) {
        builder.setLog(entry.getValue());
      }
    }

    try {
      logDebug(debug, "About to grab screenshots and prepare output for [%s]", serial);
      // Create the output directory, if it does not already exist.
      work.mkdirs();

      // Sync device screenshots, if any, to the local filesystem.
      String dirName = "app_" + SPOON_SCREENSHOTS;
      String localDirName = work.getAbsolutePath();
      final String devicePath = "/data/data/" + appPackage + "/" + dirName;
      FileEntry deviceDir = obtainDirectoryFileEntry(devicePath);
      logDebug(debug, "Pulling screenshots from [%s] %s", serial, devicePath);

      device.getSyncService().pull(new FileEntry[] {deviceDir}, localDirName, QUIET_MONITOR);

      File screenshotDir = new File(work, dirName);
      if (screenshotDir.exists()) {
        File imageDir = FileUtils.getFile(output, "image", serial);
        imageDir.mkdirs();

        // Move all children of the screenshot directory into the image folder.
        File[] classNameDirs = screenshotDir.listFiles();
        if (classNameDirs != null) {
          Multimap<DeviceTest, File> screenshots = ArrayListMultimap.create();
          for (File classNameDir : classNameDirs) {
            String className = classNameDir.getName();
            File destDir = new File(imageDir, className);
            FileUtils.moveDirectory(classNameDir, destDir);
            for (File screenshot : FileUtils.listFiles(destDir, TrueFileFilter.INSTANCE,
                TrueFileFilter.INSTANCE)) {
              String methodName = screenshot.getParentFile().getName();

              // Add screenshot to appropriate method result.
              DeviceTest testIdentifier = new DeviceTest(className, methodName);
              screenshots.put(testIdentifier, screenshot);
              result.getMethodResultBuilder(testIdentifier).addScreenshot(screenshot);
            }
          }

          // Make animated GIFs for all the tests which have screenshots.
          for (DeviceTest deviceTest : screenshots.keySet()) {
            List<File> testScreenshots = new ArrayList<File>(screenshots.get(deviceTest));
            if (testScreenshots.size() == 1) {
              continue; // Do not make an animated GIF if there is only one screenshot.
            }
            Collections.sort(testScreenshots);
            File animatedGif = FileUtils.getFile(imageDir, deviceTest.getClassName(),
                deviceTest.getMethodName() + ".gif");
            createAnimatedGif(testScreenshots, animatedGif);
            result.getMethodResultBuilder(deviceTest).setAnimatedGif(animatedGif);
          }
        }
        try {
          FileUtils.deleteDirectory(screenshotDir);
        } catch (IOException ignored) {
          // DDMS r16 bug on Windows. Le sigh.
          logInfo(
              "Warning: IOException when trying to delete %s.  If you're not on Windows, panic.",
              screenshotDir);
        }
      }
    } catch (Exception e) {
      result.addException(e);
    }

    return result.build();
  }

  /////////////////////////////////////////////////////////////////////////////
  ////  Secondary Per-Device Process  /////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////

  /** Deserialize ourselves from disk, run the tests, and serialize the result back to disk. */
  public static void main(String... args) {
    if (args.length != 1) {
      throw new IllegalArgumentException("Must be started with a device directory.");
    }

    try {
      String outputDirName = args[0];
      File outputDir = new File(outputDirName);
      File executionFile = new File(outputDir, FILE_EXECUTION);
      if (!executionFile.exists()) {
        throw new IllegalArgumentException("Device directory and/or execution file doesn't exist.");
      }

      FileReader reader = new FileReader(executionFile);
      SpoonDeviceRunner target = GSON.fromJson(reader, SpoonDeviceRunner.class);
      reader.close();

      AndroidDebugBridge adb = SpoonUtils.initAdb(target.sdk);
      DeviceResult result = target.run(adb);
      AndroidDebugBridge.terminate();

      // Write device result file.
      FileWriter writer = new FileWriter(new File(outputDir, FILE_RESULT));
      GSON.toJson(result, writer);
      writer.close();
    } catch (Exception ex) {
      System.out.println("ERROR: Unable to execute test for target.");
      ex.printStackTrace();
    }
  }
}
