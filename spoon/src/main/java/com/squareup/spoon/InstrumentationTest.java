package com.squareup.spoon;

import com.android.ddmlib.testrunner.TestIdentifier;
import com.squareup.spoon.ExecutionTestResult.TestResult;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.TestOnly;

public class InstrumentationTest {
  public transient final TestIdentifier identifier;
  public final String testName;
  public String className;
  public String classSimpleName;
  private final Map<String, ExecutionTestResult> deviceResults =
    new HashMap<String, ExecutionTestResult>();

  public InstrumentationTest(TestIdentifier identifier) {
    testName = identifier.getTestName();
    this.identifier = identifier;
  }

  @TestOnly InstrumentationTest(String className, String classSimpleName, String testName) {
    this.identifier = null;
    this.className = className;
    this.classSimpleName = classSimpleName;
    this.testName = testName;
  }

  public void createResult(String deviceName, ExecutionTestResult result) {
    deviceResults.put(deviceName, result);
  }

  public void setResult(String deviceName, TestResult result, String trace) {
    ExecutionTestResult executionTestResult = deviceResults.get(deviceName);
    if (executionTestResult.result == null) {
      executionTestResult.result = result;
      executionTestResult.trace = trace;
    }
  }

  public ExecutionTestResult getResult(String deviceName) {
    return deviceResults.get(deviceName);
  }

  public Map<String, ExecutionTestResult> getResults() {
    return deviceResults;
  }

  public void mergeResults(InstrumentationTest test) {
    deviceResults.putAll(test.getResults());
  }

  /** Mustache can't read maps. Feed it a list to consume. Nom nom nom. */
  public Collection<ExecutionTestResult> results() {
    return deviceResults.values();
  }

  public int numDevices() {
    return deviceResults.size();
  }
}
