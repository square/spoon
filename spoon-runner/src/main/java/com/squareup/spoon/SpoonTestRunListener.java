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

  SpoonTestRunListener(DeviceResult.Builder result) {
    checkNotNull(result);
    this.result = result;
  }

  @Override public void testRunStarted(String runName, int testCount) {
    result.startTests();
  }

  @Override public void testStarted(TestIdentifier test) {
    DeviceTestResult.Builder methodResult = new DeviceTestResult.Builder().startTest();
    methodResults.put(test, methodResult);
  }

  @Override public void testFailed(TestFailure status, TestIdentifier test, String trace) {
    DeviceTestResult.Builder methodResult = methodResults.get(test);
    switch (status) {
      case FAILURE:
        methodResult.markTestAsFailed(trace);
        break;
      case ERROR:
        methodResult.markTestAsError(trace);
        break;
      default:
        throw new IllegalArgumentException("Unknown test failure status: " + status);
    }
  }

  @Override public void testEnded(TestIdentifier test, Map<String, String> testMetrics) {
    DeviceTestResult.Builder methodResultBuilder = methodResults.get(test).endTest();
    result.addTestResultBuilder(DeviceTest.from(test), methodResultBuilder);
  }

  @Override public void testRunFailed(String errorMessage) {
    result.addException(errorMessage);
  }

  @Override public void testRunStopped(long elapsedTime) {
  }

  @Override public void testRunEnded(long elapsedTime, Map<String, String> runMetrics) {
    result.endTests();
  }
}
