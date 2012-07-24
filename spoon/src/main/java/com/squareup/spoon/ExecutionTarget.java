package com.squareup.spoon;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner;
import com.squareup.spoon.model.Device;
import com.squareup.spoon.model.RunConfig;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

/** Represents a single device and the test configuration to be executed. */
public class ExecutionTarget implements Callable<ExecutionResult> {
  private static final String FILE_EXECUTION = "execution.json";
  private static final String FILE_RESULT = "result.json";

  private String sdkPath;
  private RunConfig config;
  private String testPackage;
  private Device device;
  private File outputDirectory;

  public ExecutionTarget() {
    // Used by Jackson for deserialization.
  }

  /**
   * Create a test runner for a single device.
   *
   * @param sdkPath Path to the local Android SDK directory.
   * @param config Test run configuration.
   * @param testPackage Application package to instrument.
   * @param device Device to run the test on.
   */
  public ExecutionTarget(String sdkPath, RunConfig config, String testPackage, Device device) {
    this.sdkPath = sdkPath;
    this.config = config;
    this.testPackage = testPackage;
    this.device = device;

    this.outputDirectory = new File(config.output, device.uniqueIdentifier());
  }


  /////////////////////////////////////////////////////////////////////////////
  ////  Main ExecutionSuite Process  //////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////

  /** Serialize ourself to disk and start {@link #main(String...)} in another process. */
  @Override public ExecutionResult call() throws IOException, InterruptedException {
    // Create the output directory.
    outputDirectory.mkdirs();

    // Write our configuration to a file in the output directory.
    File fileExecution = new File(outputDirectory, FILE_EXECUTION);
    SpoonMapper.getInstance().writeValue(fileExecution, this);

    // Kick off a new process to interface with ADB and perform the real execution.
    String classpath = System.getProperty("java.class.path");
    new ProcessBuilder("java", "-cp", classpath, ExecutionTarget.class.getName(), outputDirectory.getAbsolutePath())
        .start()
        .waitFor();

    File fileResult = new File(outputDirectory, FILE_RESULT);
    return SpoonMapper.getInstance().readValue(fileResult, ExecutionResult.class);
  }


  /////////////////////////////////////////////////////////////////////////////
  ////  Secondary Pre-Device Process  /////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////

  public static void main(String... args) throws IOException {
    if (args.length != 1) {
      throw new IllegalArgumentException("Must be started with a device directory.");
    }

    String outputDirectory = args[0];
    File executionFile = new File(outputDirectory, FILE_EXECUTION);
    if (!executionFile.exists()) {
      throw new IllegalArgumentException("Device directory and/or execution file does not exist.");
    }

    ExecutionTarget target = SpoonMapper.getInstance().readValue(executionFile, ExecutionTarget.class);
    ExecutionResult result = new ExecutionResult(target.device);

    IDevice realDevice = null;
    try {
      AndroidDebugBridge adb = AdbHelper.init(target.sdkPath);

      realDevice = obtainRealDevice(adb, target.device);
      result.configureFor(realDevice);

      // Install the main application and the instrumentation package.
      realDevice.installPackage(target.config.app.getAbsolutePath(), true);
      realDevice.installPackage(target.config.test.getAbsolutePath(), true);

      // Run all the tests! o/
      result.testStart = System.currentTimeMillis();
      new RemoteAndroidTestRunner(target.testPackage, realDevice).run(result);
      result.testEnd = System.currentTimeMillis();

    } catch (Exception e) {
      e.printStackTrace();
      // TODO record exception
    } finally {
      try {
        if (realDevice != null) {
          releaseRealDevice(realDevice);
        }
        AndroidDebugBridge.terminate();
      } catch (Exception e) {
        e.printStackTrace();
        // TODO log
      }
    }

    File resultFile = new File(outputDirectory, FILE_RESULT);
    SpoonMapper.getInstance().writeValue(resultFile, result);
  }

  /** Fetch or create a real device that corresponds to a device model. */
  private static IDevice obtainRealDevice(AndroidDebugBridge adb, Device device) {
    if (!device.isEmulator()) {
      // Get an existing real device.
      for (IDevice adbDevice : adb.getDevices()) {
        if (adbDevice.getSerialNumber().equals(device.serial)) {
          return adbDevice;
        }
      }
      throw new IllegalArgumentException("Unknown serial ID: " + device.serial);
    } else {
      // Create an emulator with a matching configuration.
      // TODO create, start, and wait for an emulator
      throw new IllegalArgumentException("TODO");
    }
  }

  /** Tear down a device. This only performs operations if the real device is an emulator. */
  private static void releaseRealDevice(IDevice realDevice) {
    if (realDevice.isEmulator()) {
      // TODO shut down emulator
    }
  }

  private static String deviceName(IDevice realDevice) {
    return realDevice.isEmulator() ? realDevice.getAvdName() : realDevice.getSerialNumber();
  }
}
