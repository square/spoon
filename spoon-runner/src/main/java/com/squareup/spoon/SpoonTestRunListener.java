package com.squareup.spoon;

import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.ddmlib.testrunner.TestIdentifier;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.squareup.spoon.SpoonLogger.logDebug;
import static com.squareup.spoon.SpoonLogger.logError;

/** Marshals an {@link ITestRunListener}'s output to a {@link DeviceResult.Builder}. */
final class SpoonTestRunListener implements ITestRunListener {
  private final DeviceResult.Builder result;
  private final Map<TestIdentifier, DeviceTestResult.Builder> methodResults = new HashMap<>();
  private final boolean debug;

  SpoonTestRunListener(DeviceResult.Builder result, boolean debug) {
    checkNotNull(result);
    this.result = result;
    this.debug = debug;
  }

  private DeviceTestResult.Builder obtainMethodResult(TestIdentifier test) {
    return methodResults.computeIfAbsent(test, input -> {
      logError("unknown test=%s", test);
      return new DeviceTestResult.Builder().startTest();
    });
  }

  @Override public void testStarted(TestIdentifier test) {
    logDebug(debug, "started %s", test);
    methodResults.put(test, new DeviceTestResult.Builder().startTest());
  }

  @Override public void testFailed(TestIdentifier test, String trace) {
    logDebug(debug, "failed %s", trace);
    obtainMethodResult(test).markTestAsFailed(trace);
  }

  @Override public void testAssumptionFailure(TestIdentifier test, String trace) {
    logDebug(debug, "assumption failure %s", trace);
    obtainMethodResult(test).markTestAsAssumptionViolation(trace);
  }

  @Override public void testIgnored(TestIdentifier test) {
    logDebug(debug, "ignored %s", test);
    obtainMethodResult(test).markTestAsIgnored();
  }

  @Override public void testEnded(TestIdentifier test, Map<String, String> testMetrics) {
    logDebug(debug, "ended %s", test);
    result.addTestResultBuilder(DeviceTest.from(test), obtainMethodResult(test).endTest());
  }

  @Override public void testRunStarted(String runName, int testCount) {
  }

  @Override public void testRunFailed(String errorMessage) {
    logDebug(debug, "errorMessage=%s", errorMessage);
    result.addException(errorMessage);
  }

  @Override public void testRunStopped(long elapsedTime) {
  }

  @Override public void testRunEnded(long elapsedTime, Map<String, String> runMetrics) {
  }
}
