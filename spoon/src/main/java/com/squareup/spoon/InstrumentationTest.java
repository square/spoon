package com.squareup.spoon;

import com.android.ddmlib.testrunner.TestIdentifier;
import com.squareup.spoon.ExecutionTestResult.TestResult;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class InstrumentationTest {
  public final TestIdentifier identifier;
  public final String className;
  public final String classSimpleName;
  public final String testName;
  private final Map<String, ExecutionTestResult> deviceResults =
    new HashMap<String, ExecutionTestResult>();

  public InstrumentationTest(TestIdentifier identifier) {
    className = identifier.getClassName();
    testName = identifier.getTestName();
    this.identifier = identifier;

    // Fake Class#getSimpleName logic.
    int lastPeriod = className.lastIndexOf(".");
    if (lastPeriod != -1) {
      classSimpleName = className.substring(lastPeriod + 1);
    } else {
      classSimpleName = className;
    }
  }

  public void createResult(String deviceName, ExecutionTestResult result) {
    deviceResults.put(deviceName, result);
  }

  public void setResult(String deviceName, TestResult result) {
    if (deviceResults.get(deviceName).result == null) {
      deviceResults.get(deviceName).result = result;
    }
  }

  public ExecutionTestResult getResult(String deviceName) {
    return deviceResults.get(deviceName);
  }

  public Map<String, ExecutionTestResult> getResults() {
    return deviceResults;
  }

  /** Mustache can't read maps. Feed it a list to consume. Nom nom nom. */
  public Collection<ExecutionTestResult> results() {
    return deviceResults.values();
  }
}
