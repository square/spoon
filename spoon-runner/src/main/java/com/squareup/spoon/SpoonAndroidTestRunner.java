package com.squareup.spoon;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.CollectingOutputReceiver;
import com.android.ddmlib.IShellEnabledDevice;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static com.squareup.spoon.SpoonLogger.logDebug;
import static com.squareup.spoon.SpoonLogger.logError;

/**
 * Allows the option of running the "pm clear" command before every "run" invocation.
 */
public class SpoonAndroidTestRunner extends RemoteAndroidTestRunner {

  private static final long MAX_TIMEOUT_MS = 10000L;
  private static final long MAX_TIME_TO_OUTPUT_RESPONSE_MILLIS = 10000L;

  private IShellEnabledDevice remoteDevice;
  private CollectingOutputReceiver outputReceiver;
  private String clearCommandStr;
  private boolean clearAppDataBeforeEachTest;
  private boolean debug;

  public SpoonAndroidTestRunner(String appPackageName,
                                String packageName,
                                String runnerName,
                                IShellEnabledDevice remoteDevice,
                                boolean clearAppDataBeforeEachTest,
                                boolean debug) {
    super(packageName, runnerName, remoteDevice);
    this.remoteDevice = remoteDevice;
    this.outputReceiver = new CollectingOutputReceiver();
    this.clearCommandStr = "pm clear " + appPackageName;
    this.clearAppDataBeforeEachTest = clearAppDataBeforeEachTest;
    this.debug = debug;
  }

  @Override
  public void run(Collection<ITestRunListener> listeners)
      throws TimeoutException, AdbCommandRejectedException,
      ShellCommandUnresponsiveException, IOException {
    try {
      if (clearAppDataBeforeEachTest) {
        logDebug(debug, String.format("Running adb command: %s", clearCommandStr));
        remoteDevice.executeShellCommand(clearCommandStr, outputReceiver, MAX_TIMEOUT_MS,
                MAX_TIME_TO_OUTPUT_RESPONSE_MILLIS, TimeUnit.MILLISECONDS);

        String output = outputReceiver.getOutput();
        if (output == null || !output.contains("Success")) {
          logError("adb clear command failed with the following output: " + output);
        }
      }
    } catch (IOException
            | ShellCommandUnresponsiveException
            | TimeoutException
            | AdbCommandRejectedException e) {
      String exceptionName = e.getClass().getSimpleName();
      logError(String.format(
          "%1$s %2$s when running adb 'pm clear' command %3$s on %4$s",
          exceptionName, e.toString(), getPackageName(), remoteDevice.getName()));
      throw e;
    }

    super.run(listeners);
  }
}
