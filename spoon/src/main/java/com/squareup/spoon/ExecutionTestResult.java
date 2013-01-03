package com.squareup.spoon;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.squareup.spoon.Screenshot.EXTENSION;
import static com.squareup.spoon.Screenshot.NAME_SEPARATOR;
import static com.squareup.spoon.Utils.prettifyTestName;

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
  public String prettyTestName;
  public String className;
  public String classSimpleName;
  public TestResult result;
  public String trace;
  public String deviceName;
  public String serial;
  public List<Screenshot> screenshots = new ArrayList<Screenshot>();

  public ExecutionTestResult(InstrumentationTest test) {
    className = test.className;
    classSimpleName = test.classSimpleName;
    testName = test.testName;
    prettyTestName = prettifyTestName(testName);
  }

  public boolean hasScreenshots() {
    return !screenshots.isEmpty();
  }

  public void addScreenshot(File screenshotFile) {
    screenshots.add(new Screenshot(screenshotFile));
  }

  public String getTrace() {
    if (trace == null) {
      return null;
    }
    String[] lines = trace.replaceAll("\r\n", "\n").split("\n");
    StringBuilder builder = new StringBuilder(lines[0]);
    for (int i = 1; i < lines.length; i++) {
      builder.append("\n    ").append(lines[i]);
    }
    return builder.toString();
  }

  public class Screenshot {
    static final String HTML_SEPARATOR = "-";
    static final String INVALID_CHARS = "[^A-Za-z0-9_]";

    public final File file;
    public final Date taken;
    public final String tag;
    public final String id;
    public final String screenshotGroup;

    public Screenshot(File screenshotFile) {
      String name = screenshotFile.getName();
      if (!name.endsWith(EXTENSION)) {
        throw new IllegalArgumentException("Invalid screenshot extension: " + name);
      }
      String[] nameData = name.split(NAME_SEPARATOR, 2);
      if (nameData.length != 2) {
        throw new IllegalArgumentException("Invalid screenshot name: " + name);
      }
      taken = new Date(Long.valueOf(nameData[0]));
      tag = nameData[1].substring(0, nameData[1].length() - EXTENSION.length());

      file = screenshotFile;

      id = (className + HTML_SEPARATOR + testName + HTML_SEPARATOR + tag) //
          .replaceAll(INVALID_CHARS, "-");
      screenshotGroup = (classSimpleName + HTML_SEPARATOR + testName) //
          .replaceAll(INVALID_CHARS, "-");
    }
  }
}
