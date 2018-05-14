package com.squareup.spoon;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.CollectingOutputReceiver;
import com.android.ddmlib.IShellEnabledDevice;
import com.android.ddmlib.Log;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

/**
 * Run the "pm clear" command before every `run` invocation.
 */
public class SpoonClearingAndroidTestRunner extends RemoteAndroidTestRunner {

    private static final String LOG_TAG = "SpoonClearingAndroidTestRunner";
    private IShellEnabledDevice mRemoteDevice;
    private CollectingOutputReceiver mOutputReceiver;
    private long mMaxTimeoutMs = 10000L;
    private long mMaxTimeToOutputResponseMs = 10000L;
    private String mAppPackageName;
    private String mClearCommandStr;
    private boolean mClearAppDataBeforeEachTest;

    public SpoonClearingAndroidTestRunner(String appPackageName,
                                          String packageName,
                                          String runnerName,
                                          IShellEnabledDevice remoteDevice,
                                          boolean clearAppDataBeforeEachTest) {
        super(packageName, runnerName, remoteDevice);
        mAppPackageName = appPackageName;
        mRemoteDevice = remoteDevice;
        mOutputReceiver = new CollectingOutputReceiver();
        mClearCommandStr = "pm clear " + mAppPackageName;
        mClearAppDataBeforeEachTest = clearAppDataBeforeEachTest;
    }

    @Override
    public void run(Collection<ITestRunListener> listeners)
            throws TimeoutException, AdbCommandRejectedException,
            ShellCommandUnresponsiveException, IOException {

        try {
            if (mClearAppDataBeforeEachTest) {
                Log.w(LOG_TAG, String.format("Running adb clear command: %s", mClearCommandStr));
                mRemoteDevice.executeShellCommand(mClearCommandStr, mOutputReceiver, mMaxTimeoutMs,
                        mMaxTimeToOutputResponseMs, TimeUnit.MILLISECONDS);
            }
        } catch (IOException e) {
            logErrorString(e);
            throw e;
        } catch (ShellCommandUnresponsiveException e) {
            logErrorString(e);
            throw e;
        } catch (TimeoutException e) {
            logErrorString(e);
            throw e;
        } catch (AdbCommandRejectedException e) {
            logErrorString(e);
            throw e;
        }

        super.run(listeners);
    }

    private void logErrorString(Exception e) {
        String exceptionName = e.getClass().getSimpleName();
        Log.w(LOG_TAG, String.format(
                "%1$s %2$s when running adb 'pm clear' command %3$s on %4$s",
                exceptionName, e.toString(), getPackageName(), mRemoteDevice.getName()));
    }
}
