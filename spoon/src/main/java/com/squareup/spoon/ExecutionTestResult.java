package com.squareup.spoon;

import com.android.ddmlib.testrunner.TestIdentifier;

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

  public InstrumentationTest test;
  public TestResult result;
  public String deviceName;
  public String serial;
  public List<Screenshot> screenshots = new ArrayList<Screenshot>();

  public ExecutionTestResult(TestIdentifier identifier) {
    this.test = new InstrumentationTest(identifier);
  }

  public class Screenshot {
    public final File file;
    public final String id;
    public final String screenshotGroup;

    public Screenshot(File screenshotFile) {
      file = screenshotFile;
      id = (test.className + "-" + test.testName + "-" + file.getName())
        .replaceAll("[^A-Za-z0-9_-]", "");
      screenshotGroup = (test.classSimpleName + "-" + test.testName)
        .replaceAll("[^A-Za-z0-9_-]", "-");
    }
  }
}
