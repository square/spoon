package com.squareup.spoon.uiautomator;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IShellEnabledDevice;
import com.android.ddmlib.Log;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import com.android.ddmlib.testrunner.IRemoteAndroidTestRunner;
import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.ddmlib.testrunner.InstrumentationResultParser;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Class RemoteUiAutomatorTestRunner equivalent of RemoteAndroidTestRunner for
 *
 * @author Ilja Bobkevic 2013-09-13 initial version
 */
public class RemoteUiAutomatorTestRunner implements IRemoteAndroidTestRunner {

  private static final String LOG_TAG = "RemoteUiAutomatorTest";
  private static final String DEFAULT_RUNNER_NAME = "com.android.uiautomator.testrunner."
      + "UiAutomatorTestRunner";
  private static final char CLASS_SEPARATOR = ',';
  private static final char METHOD_SEPARATOR = '#';
  // defined test runner argument names
  private static final String CLASS_ARG_NAME = "class";
  private static final String DEBUG_ARG_NAME = "debug";
  private static final String RUNNER_ARG_NAME = "runner";
  private final String mPackageName;
  private final String mRunnerName;
  private IShellEnabledDevice mRemoteDevice;
  // default to no timeout
  private int mMaxTimeToOutputResponse = 0;
  private String mRunName = null;
  /**
   * map of name-value instrumentation argument pairs
   */
  private InstrumentationResultParser mParser;
  private Map<String, String> mArgMap;


  /**
   * Creates a remote Android test runner.
   *
   * @param packageName  the Android application package that contains the tests to run
   * @param runnerName   the instrumentation test runner to execute. If null, will use default
   *                     runner
   * @param remoteDevice the Android device to execute tests on
   */
  public RemoteUiAutomatorTestRunner(String packageName,
                                     String runnerName,
                                     IShellEnabledDevice remoteDevice) {

    mPackageName = packageName;
    mRunnerName = runnerName;
    mRemoteDevice = remoteDevice;
    mArgMap = new HashMap<String, String>();
  }

  /**
   * Alternate constructor. Uses default instrumentation runner.
   *
   * @param packageName  the Android application package that contains the tests to run
   * @param remoteDevice the Android device to execute tests on
   */
  public RemoteUiAutomatorTestRunner(String packageName,
                                     IShellEnabledDevice remoteDevice) {
    this(packageName, null, remoteDevice);
  }

  public String getPackageName() {
    return mPackageName;
  }

  public String getRunnerName() {
    if (mRunnerName == null) {
      return DEFAULT_RUNNER_NAME;
    }
    return mRunnerName;
  }

  public void setClassName(String className) {
    addInstrumentationArg(CLASS_ARG_NAME, className);
  }

  public void setClassNames(String[] classNames) {
    StringBuilder classArgBuilder = new StringBuilder();

    for (int i = 0; i < classNames.length; i++) {
      if (i != 0) {
        classArgBuilder.append(CLASS_SEPARATOR);
      }
      classArgBuilder.append(classNames[i]);
    }
    setClassName(classArgBuilder.toString());
  }

  @Override
  public void setMethodName(String className, String testName) {
    setClassName(className + METHOD_SEPARATOR + testName);
  }

  @Override
  public void setTestPackageName(String packageName) {
    throw new UnsupportedOperationException("Test package names are not available in UiAutomator");
  }

  @Override
  public void setTestSize(TestSize size) {
    throw new UnsupportedOperationException("Test size is not available in UiAutomator");
  }

  @Override
  public void addInstrumentationArg(String name, String value) {
    if (name == null || value == null) {
      throw new IllegalArgumentException("name or value arguments cannot be null");
    }
    mArgMap.put(name, value);
  }

  @Override
  public void removeInstrumentationArg(String name) {
    if (name == null) {
      throw new IllegalArgumentException("name argument cannot be null");
    }
    mArgMap.remove(name);
  }

  public void addBooleanArg(String name, boolean value) {
    addInstrumentationArg(name, Boolean.toString(value));
  }

  @Override
  public void setLogOnly(boolean logOnly) {
    throw new UnsupportedOperationException("Set only logging test mode is not available in "
        + "UiAutomator");
  }

  public void setDebug(boolean debug) {
    addBooleanArg(DEBUG_ARG_NAME, debug);
  }

  @Override
  public void setCoverage(boolean coverage) {
    throw new UnsupportedOperationException("Coverage is not available in UiAutomator");
  }

  public void setMaxtimeToOutputResponse(int maxTimeToOutputResponse) {
    mMaxTimeToOutputResponse = maxTimeToOutputResponse;
  }

  public void setRunName(String runName) {
    mRunName = runName;
  }

  public void run(ITestRunListener... listeners)
      throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException,
      IOException {
    run(Arrays.asList(listeners));
  }

  public void run(Collection<ITestRunListener> listeners)
      throws TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException,
      IOException {
    final String runCaseCommandStr = String.format("uiautomator runtest %1$s %2$s %3$s",
        getPackageName(), getArgsCommand(), getRunnerCommand());
    Log.i(LOG_TAG, String.format("Running %1$s on %2$s", runCaseCommandStr,
        mRemoteDevice.getName()));
    System.out.println(runCaseCommandStr);
    String runName = mRunName == null ? mPackageName : mRunName;
    mParser = new InstrumentationResultParser(runName, listeners);

    try {
      mRemoteDevice.executeShellCommand(runCaseCommandStr, mParser, mMaxTimeToOutputResponse);
    } catch (IOException e) {
      Log.w(LOG_TAG, String.format("IOException %1$s when running tests %2$s on %3$s",
          e.toString(), getPackageName(), mRemoteDevice.getName()));
      // rely on parser to communicate results to listeners
      mParser.handleTestRunFailed(e.toString());
      throw e;
    } catch (ShellCommandUnresponsiveException e) {
      Log.w(LOG_TAG, String.format(
          "ShellCommandUnresponsiveException %1$s when running tests %2$s on %3$s",
          e.toString(), getPackageName(), mRemoteDevice.getName()));
      mParser.handleTestRunFailed(String.format(
          "Failed to receive adb shell test output within %1$d ms. "
              + "Test may have timed out, or adb connection to device became unresponsive",
          mMaxTimeToOutputResponse));
      throw e;
    } catch (TimeoutException e) {
      Log.w(LOG_TAG, String.format(
          "TimeoutException when running tests %1$s on %2$s", getPackageName(),
          mRemoteDevice.getName()));
      mParser.handleTestRunFailed(e.toString());
      throw e;
    } catch (AdbCommandRejectedException e) {
      Log.w(LOG_TAG, String.format(
          "AdbCommandRejectedException %1$s when running tests %2$s on %3$s",
          e.toString(), getPackageName(), mRemoteDevice.getName()));
      mParser.handleTestRunFailed(e.toString());
      throw e;
    }
  }

  public void cancel() {
    if (mParser != null) {
      mParser.cancel();
    }
  }

  /**
   * Returns the full instrumentation command line syntax for the provided instrumentation
   * arguments.
   * Returns an empty string if no arguments were specified.
   */
  private String getArgsCommand() {
    StringBuilder commandBuilder = new StringBuilder();
    for (Map.Entry<String, String> argPair : mArgMap.entrySet()) {
      final String argCmd = String.format(" -e %1$s %2$s", argPair.getKey(),
          argPair.getValue());
      commandBuilder.append(argCmd);
    }
    return commandBuilder.toString();
  }

  public String getRunnerCommand() {
    return String.format("-e %s %s", RUNNER_ARG_NAME, getRunnerName());
  }
}
