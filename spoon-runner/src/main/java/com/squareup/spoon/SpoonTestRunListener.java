package com.squareup.spoon;

import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.ddmlib.testrunner.TestIdentifier;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/** Marshals an {@link ITestRunListener}'s output to a {@link DeviceResult.Builder}. */
final class SpoonTestRunListener implements ITestRunListener {
  private final DeviceResult.Builder result;
  private final Map<TestIdentifier, DeviceTestResult.Builder> methodResults =
      new HashMap<TestIdentifier, DeviceTestResult.Builder>();
  private final SpoonLogger log;

  SpoonTestRunListener(DeviceResult.Builder result, SpoonLogger log) {
    checkNotNull(result);
    this.result = result;
    this.log = log;
  }

  @Override public void testRunStarted(String runName, int testCount) {
    log.fine("STRL.testRunStarted %d %s", testCount, runName);
    result.startTests();
  }

  @Override public void testStarted(TestIdentifier test) {
    log.fine("STRL.testStarted %s", test);
    DeviceTestResult.Builder methodResult = new DeviceTestResult.Builder().startTest();
    methodResults.put(test, methodResult);
  }

  @Override public void testFailed(TestFailure status, TestIdentifier test, String trace) {
    DeviceTestResult.Builder methodResult = methodResults.get(test);
    switch (status) {
      case FAILURE:
        log.fine("STRL.testFailed FAILURE %s", trace);
        methodResult.markTestAsFailed(trace);
        break;
      case ERROR:
        log.fine("STRL.testFailed ERROR %s", trace);
        methodResult.markTestAsError(trace);
        break;
      default:
        throw new IllegalArgumentException("Unknown test failure status: " + status);
    }
  }

  @Override public void testEnded(TestIdentifier test, Map<String, String> testMetrics) {
    log.fine("STRL.testEnded %s", test);
    DeviceTestResult.Builder methodResultBuilder = methodResults.get(test).endTest();
    result.addTestResultBuilder(DeviceTest.from(test), methodResultBuilder);
  }

  @Override public void testRunFailed(String errorMessage) {
    log.fine("STRL.testRunFailed %s", errorMessage);
    result.addException(errorMessage);
  }

  @Override public void testRunStopped(long elapsedTime) {
    log.fine("STRL.testRunStopped elapsedTime=%d", elapsedTime);
  }

  @Override public void testRunEnded(long elapsedTime, Map<String, String> runMetrics) {
    log.fine("STRL.testRunEnded elapsedTime=%d", elapsedTime);
    result.endTests();
  }
}
