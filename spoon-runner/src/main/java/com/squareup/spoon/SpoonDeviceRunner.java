package com.squareup.spoon;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.InstallException;
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import static com.android.ddmlib.FileListingService.FileEntry;
import static com.squareup.spoon.Spoon.SPOON_SCREENSHOTS;
import static com.squareup.spoon.SpoonUtils.GSON;
import static com.squareup.spoon.SpoonUtils.QUIET_MONITOR;
import static com.squareup.spoon.SpoonUtils.createAnimatedGif;
import static com.squareup.spoon.SpoonUtils.obtainDirectoryFileEntry;
import static com.squareup.spoon.SpoonUtils.obtainRealDevice;
import static java.util.logging.Level.SEVERE;

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
   */
  SpoonDeviceRunner(File sdk, File apk, File testApk, File output, String serial, boolean debug,
      String classpath, SpoonInstrumentationInfo instrumentationInfo) {
    this.sdk = sdk;
    this.apk = apk;
    this.testApk = testApk;
    this.serial = serial;
    this.debug = debug;
    this.output = output;
    this.work = FileUtils.getFile(output, TEMP_DIR, serial);
    this.classpath = classpath;
    this.instrumentationInfo = instrumentationInfo;
  }

  /** Serialize ourself to disk and start {@link #main(String...)} in another process. */
  public DeviceResult runInNewProcess() throws IOException, InterruptedException {
    // Create the output directory.
    work.mkdirs();

    // Write our configuration to a file in the output directory.
    FileWriter executionWriter = new FileWriter(new File(work, FILE_EXECUTION));
    GSON.toJson(this, executionWriter);
    executionWriter.close();

    // Kick off a new process to interface with ADB and perform the real execution.
    String name = SpoonDeviceRunner.class.getName();
    Process process =
        new ProcessBuilder("java", "-cp", classpath, name, work.getAbsolutePath()).start();
    process.waitFor();

    // Read the result from a file in the output directory.
    FileReader resultFile = new FileReader(new File(work, FILE_RESULT));
    DeviceResult result = GSON.fromJson(resultFile, DeviceResult.class);
    resultFile.close();

    return result;
  }

  /** Execute instrumentation on the target device and return a result summary. */
  public DeviceResult run(AndroidDebugBridge adb) {
    String appPackage = instrumentationInfo.getApplicationPackage();
    String testPackage = instrumentationInfo.getInstrumentationPackage();
    String testRunner = instrumentationInfo.getTestRunnerClass();

    if (debug) {
      SpoonUtils.setDdmlibInternalLoggingLevel();
    }

    DeviceResult.Builder result = new DeviceResult.Builder();

    IDevice device = obtainRealDevice(adb, serial);

    // Get relevant device information.
    result.setDeviceDetails(DeviceDetails.createForDevice(device));

    // Install the main application and the instrumentation application.
    try {
      if (device.installPackage(apk.getAbsolutePath(), true) != null) {
        return result.markInstallAsFailed("Unable to install application APK.").build();
      }
      if (device.installPackage(testApk.getAbsolutePath(), true) != null) {
        return result.markInstallAsFailed("Unable to install instrumentation APK.").build();
      }
    } catch (InstallException e) {
      return result.markInstallAsFailed(e.getMessage()).build();
    }

    // Run all the tests! o/
    try {
      new RemoteAndroidTestRunner(testPackage, testRunner, device) //
          .run(new SpoonTestRunListener(result));
    } catch (Exception e) {
      result.addException(e);
    }

    try {
      // Create the output directory, if it does not already exist.
      work.mkdirs();

      // Sync device screenshots, if any, to the local filesystem.
      String dirName = "app_" + SPOON_SCREENSHOTS;
      String localDirName = work.getAbsolutePath();
      FileEntry deviceDir = obtainDirectoryFileEntry("/data/data/" + appPackage + "/" + dirName);
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
        } catch (Exception ignored) {
          // DDMS r16 bug on Windows. Le sigh.
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

    Logger log = Logger.getLogger(SpoonDeviceRunner.class.getSimpleName());
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
      log.log(SEVERE, "Unable to execute test for target.", ex);
    }
  }
}
