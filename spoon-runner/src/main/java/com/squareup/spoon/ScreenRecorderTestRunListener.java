package com.squareup.spoon;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.squareup.spoon.SpoonLogger.logError;
import static org.apache.commons.io.IOUtils.closeQuietly;

import com.android.ddmlib.CollectingOutputReceiver;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.ddmlib.testrunner.TestIdentifier;

final class ScreenRecorderTestRunListener implements ITestRunListener, Closeable {

  private final IDevice device;
  private final String deviceDirectoryPath;
  private final ExecutorService executorService;
  private final boolean debug;

  private final Map<TestIdentifier, ScreenRecorder> screenRecorders = new ConcurrentHashMap<>();

  ScreenRecorderTestRunListener(
      IDevice device,
      String deviceDirectoryPath,
      boolean debug) {
    this(device, deviceDirectoryPath,  Executors.newSingleThreadExecutor(), debug);
  }

  ScreenRecorderTestRunListener(
      IDevice device,
      String deviceDirectoryPath,
      ExecutorService executorService,
      boolean debug) {
    this.device = device;
    this.deviceDirectoryPath = deviceDirectoryPath;
    this.executorService = executorService;
    this.debug = debug;
  }

  @Override
  public void testRunStarted(String runName, int testCount) {
  }

  @Override
  public void testStarted(TestIdentifier test) {
    String deviceDirectory = createDeviceDirectoryFor(test);
    if (deviceDirectory != null) {
      screenRecorders.put(
          test, ScreenRecorder.open(device, deviceDirectory, executorService, debug));
    }
  }

  @Override
  public void testFailed(TestIdentifier test, String trace) {
  }

  @Override
  public void testAssumptionFailure(TestIdentifier test, String trace) {
  }

  @Override
  public void testIgnored(TestIdentifier test) {
  }

  @Override
  public void testEnded(TestIdentifier test, Map<String, String> testMetrics) {
    closeQuietly(screenRecorders.remove(test));
  }

  @Override
  public void testRunFailed(String errorMessage) {
  }

  @Override
  public void testRunStopped(long elapsedTime) {
  }

  @Override
  public void testRunEnded(long elapsedTime, Map<String, String> runMetrics) {
  }

  @Override
  public void close() throws IOException {
    executorService.shutdown();
  }

  private String createDeviceDirectoryFor(TestIdentifier testIdentifier) {
    try {
      String deviceTestDirectory = deviceDirectoryPath + '/'
          + testIdentifier.getClassName() + '/' + testIdentifier.getTestName();
      CollectingOutputReceiver outputReceiver = new CollectingOutputReceiver();
      device.executeShellCommand("mkdir -p " + deviceTestDirectory, outputReceiver);
      return deviceTestDirectory;
    } catch (Exception e) {
      logError("Failed to create device directory for test [%s] due to [%s]", testIdentifier, e);
      return null;
    }
  }
}
