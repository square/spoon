package com.squareup.spoon;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

  public String testName;
  public String className;
  public String classSimpleName;
  public TestResult result;
  public String deviceName;
  public String serial;
  public List<Screenshot> screenshots = new ArrayList<Screenshot>();

  public ExecutionTestResult(InstrumentationTest test) {
    className = test.className;
    classSimpleName = test.classSimpleName;
    testName = test.testName;
  }

  public class Screenshot {
    public final File file;
    public final String id;
    public final String screenshotGroup;

    public Screenshot(File screenshotFile) {
      file = screenshotFile;
      id = (className + "-" + testName + "-" + file.getName())
        .replaceAll("[^A-Za-z0-9_-]", "");
      screenshotGroup = (classSimpleName + "-" + testName)
        .replaceAll("[^A-Za-z0-9_-]", "-");
    }
  }
}
