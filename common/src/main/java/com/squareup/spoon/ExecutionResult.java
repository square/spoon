package com.squareup.spoon;

import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.ddmlib.testrunner.TestIdentifier;

import java.util.Map;

/** Represents the aggregated result of a test execution on a device. */
public class ExecutionResult implements ITestRunListener {
  public int testsStarted;
  public int testsFailed;

  @Override public void testRunStarted(String runName, int testCount) {
    System.out.println("[testRunStarted] runName: " + runName + ", " + testCount);
  }

  @Override public void testStarted(TestIdentifier test) {
    System.out.println("[testStarted] test: " + test);
    testsStarted += 1;
  }

  @Override public void testFailed(TestFailure status, TestIdentifier test, String trace) {
    System.out.println("[testFailed] status: " + status + ", test: " + test + ", trace: " + trace);
    testsFailed += 1;
  }

  @Override public void testEnded(TestIdentifier test, Map<String, String> testMetrics) {
    System.out.println("[testEnded] test: " + test + ", testMetrics: " + testMetrics);
  }

  @Override public void testRunFailed(String errorMessage) {
    System.out.println("[testRunFailed] errorMessage: " + errorMessage);
  }

  @Override public void testRunStopped(long elapsedTime) {
    System.out.println("[testRunStopped] elapsedTime: " + elapsedTime);
  }

  @Override public void testRunEnded(long elapsedTime, Map<String, String> runMetrics) {
    System.out.println("[testRunEnded] elapsedTime: " + elapsedTime + ", runMetrics: " + runMetrics);
  }
}
