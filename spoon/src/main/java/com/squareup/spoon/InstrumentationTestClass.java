package com.squareup.spoon;

import com.android.ddmlib.testrunner.TestIdentifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InstrumentationTestClass {
  public final String className;
  public final String classSimpleName;
  public int testsFailed;
  public int testsPassed;
  private final Map<String, InstrumentationTest> testList;

  public InstrumentationTestClass(TestIdentifier identifier) {
    className = identifier.getClassName();
    classSimpleName = getClassSimpleName(className);
    testList = new HashMap<String, InstrumentationTest>();
  }

  /* Fake Class#getSimpleName logic. */
  private static String getClassSimpleName(String className) {
    int lastPeriod = className.lastIndexOf(".");
    if (lastPeriod != -1) {
      return className.substring(lastPeriod + 1);
    } else {
      return className;
    }
  }

  public void addTest(InstrumentationTest test) {
    test.className = className;
    test.classSimpleName = classSimpleName;
    testList.put(test.identifier.toString(), test);
  }

  public Collection<InstrumentationTest> tests() {
    return testList.values();
  }

  public boolean containsTest(TestIdentifier identifier) {
    return testList.containsKey(identifier.toString());
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

  public int numTests() {
    return testList.size();
  }

  public int numDevices() {
    // Assuming that all tests run on all devices.
    return new ArrayList<InstrumentationTest>(tests()).get(0).numDevices();
  }
}
