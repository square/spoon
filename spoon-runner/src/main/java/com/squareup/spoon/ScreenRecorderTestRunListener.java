package com.squareup.spoon;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import static org.apache.commons.io.IOUtils.closeQuietly;

import com.android.ddmlib.CollectingOutputReceiver;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.ddmlib.testrunner.TestIdentifier;

final class ScreenRecorderTestRunListener implements ITestRunListener {

    private final IDevice device;
    private final String deviceFilesDir;
    private final ExecutorService executorService;
    private final boolean debug;

    private final Map<TestIdentifier, ScreenRecorder> screenRecorders = new ConcurrentHashMap<>();

    ScreenRecorderTestRunListener(
            IDevice device,
            String deviceFilesDir,
            ExecutorService executorService,
            boolean debug) {
        this.device = device;
        this.deviceFilesDir = deviceFilesDir;
        this.executorService = executorService;
        this.debug = debug;
    }

    @Override
    public void testRunStarted(String runName, int testCount) {
    }

    @Override
    public void testStarted(TestIdentifier test) {
        screenRecorders.put(test, ScreenRecorder.open(device, createDeviceDirectoryFor(test), executorService, debug));
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

    private File createDeviceDirectoryFor(TestIdentifier testIdentifier) {
        try {
            CollectingOutputReceiver outputReceiver = new CollectingOutputReceiver();
            device.executeShellCommand("echo $EXTERNAL_STORAGE", outputReceiver);
            File deviceTestsDirectory = new File(outputReceiver.getOutput().trim(), deviceFilesDir);
            File deviceTestDirectory = new File(
                    deviceTestsDirectory, testIdentifier.getClassName() + "/" + testIdentifier.getTestName());
            outputReceiver = new CollectingOutputReceiver();
            device.executeShellCommand("mkdir -p " + deviceTestDirectory.getAbsolutePath(), outputReceiver);
            return  deviceTestDirectory;
        } catch (Exception e) {
            throw new RuntimeException("Could not get external storage path", e);
        }
    }
}
