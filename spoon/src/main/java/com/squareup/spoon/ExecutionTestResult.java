package com.squareup.spoon;

import com.android.ddmlib.testrunner.TestIdentifier;

public class ExecutionTestResult {
  public enum TestResult {
    SUCCESS("pass"), FAILURE("fail");

    public final String className;

    private TestResult(String className) {
      this.className = className;
    }

    @Override public String toString() {
      return className;
    }
  }

  public String className;
  public String classSimpleName;
  public String testName;
  public TestResult result;

  public ExecutionTestResult() {
    //Used for Jackson
  }
  public ExecutionTestResult(TestIdentifier identifier) {
    classSimpleName = className = identifier.getClassName();
    testName = identifier.getTestName();

    // Fake Class#getSimpleName logic.
    int lastPeriod = classSimpleName.lastIndexOf(".");
    if (lastPeriod != -1) {
      classSimpleName = classSimpleName.substring(lastPeriod + 1);
    }
  }
}
