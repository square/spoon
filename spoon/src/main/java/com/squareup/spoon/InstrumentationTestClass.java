package com.squareup.spoon;

import com.android.ddmlib.testrunner.TestIdentifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InstrumentationTestClass {
  private final Map<String, InstrumentationTest> testList =
      new HashMap<String, InstrumentationTest>();

  public final String className;
  public final String classSimpleName;
  public int testsFailed;

  public InstrumentationTestClass(TestIdentifier identifier) {
    className = identifier.getClassName();
    classSimpleName = Utils.getClassSimpleName(className);
  }

  public void addTest(InstrumentationTest test) {
    test.className = className;
    test.classSimpleName = classSimpleName;
    if (test.identifier != null) {
      testList.put(test.identifier.toString(), test);
    }
  }

  public Collection<InstrumentationTest> tests() {
    return testList.values();
  }

  public boolean containsTest(TestIdentifier identifier) {
    return identifier != null && testList.containsKey(identifier.toString());
  }

  public InstrumentationTest getTest(TestIdentifier identifier) {
    return testList.get(identifier.toString());
  }

  public List<ExecutionTestResult> results() {
    List<ExecutionTestResult> results = new ArrayList<ExecutionTestResult>();
    for (InstrumentationTest instrumentationTest : tests()) {
      results.addAll(instrumentationTest.results());
    }
    return results;
  }

  public int testsPassed() {
    return numTests() - testsFailed;
  }

  public int numTests() {
    return testList.size();
  }

  public int numDevices() {
    // Assuming that all tests run on all devices.
    return new ArrayList<InstrumentationTest>(tests()).get(0).numDevices();
  }
}
