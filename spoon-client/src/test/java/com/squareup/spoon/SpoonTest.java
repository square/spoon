// Copyright 2012 Square, Inc.
package com.squareup.spoon;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

import static com.squareup.spoon.Spoon.*;
import static org.fest.assertions.api.Assertions.assertThat;

// TODO custom test runner, fill out what the ?s actually are
public class SpoonTest {
  private static final String SCREENSHOT = Spoon.class.getName();
  private static final String EXPECTED_CLASS = "SomeClass";
  private static final String EXPECTED_METHOD = "someMethod";

  @Test(expected = IllegalArgumentException.class)
  public void invalidStackTraceThrowsException() {
    findTestClassTraceElement(new StackTraceBuilder() //
        .add("com.intellij.rt.execution.junit.JUnitStarter", "main", "JUnitStarter.java", 63)
        .add("org.junit.runner.JUnitCore", "run", "JUnitCore.java", 157)
        .add("org.junit.runners.ParentRunner", "run", "ParentRunner.java", 300)
        .add("org.junit.runners.ParentRunner$2", "evaluate", "ParentRunner.java", 222)
        .add("org.junit.runners.ParentRunner", "access$000", "ParentRunner.java", 50)
        .add("org.junit.runners.ParentRunner", "runChildren", "ParentRunner.java", 229)
        .add("org.junit.runners.ParentRunner$1", "schedule", "ParentRunner.java", 60)
        .add("org.junit.runners.ParentRunner$3", "run", "ParentRunner.java", 231)
        .add("org.junit.runners.ParentRunner", "runLeaf", "ParentRunner.java", 263)
        .add("java.lang.reflect.Method", "invoke", "Method.java", 597)
        .add("sun.reflect.NativeMethodAccessorImpl", "invoke", "NativeMethodAccessorImpl.java", 39)
        .add("sun.reflect.NativeMethodAccessorImpl", "invoke0", "NativeMethodAccessorImpl.java", -2)
        .add("java.lang.Thread", "getStackTrace", "Thread.java", 1503)
        .build());
  }

  @Test public void directScreenshotCall() {
    StackTraceElement actual = findTestClassTraceElement(new StackTraceBuilder() //
        .add("some.Class", "someMethod", "Class.java", 1)
        .add("this.That", "thatThis", "That.java", 2)
        .add(TEST_CASE_CLASS_JUNIT_3, TEST_CASE_METHOD_JUNIT_3, "A.java", 3)
        .add("?", "?", "?", 0)
        .add("?", "?", "?", 0)
        .add(EXPECTED_CLASS, EXPECTED_METHOD, "Whatever.java", 50)
        .add(SCREENSHOT, "screenshot", "Spoon.java", 30)
        .add(SCREENSHOT, "obtainScreenshotDirectory", "Spoon.java", 40)
        .build());

    assertThat(actual.getClassName()).isEqualTo(EXPECTED_CLASS);
    assertThat(actual.getMethodName()).isEqualTo(EXPECTED_METHOD);
  }

  @Test public void withConvenienceMethodCall() {
    StackTraceElement actual = findTestClassTraceElement(new StackTraceBuilder() //
        .add("some.Class", "someMethod", "Class.java", 1)
        .add("this.That", "thatThis", "That.java", 2)
        .add(TEST_CASE_CLASS_JUNIT_3, TEST_CASE_METHOD_JUNIT_3, "A.java", 3)
        .add("?", "?", "?", 0)
        .add("?", "?", "?", 0)
        .add(EXPECTED_CLASS, EXPECTED_METHOD, "Whatever.java", 50)
        .add("com.example.Utils", "captureScreen", "Utils.java", 100)
        .add(SCREENSHOT, "screenshot", "Spoon.java", 30)
        .add(SCREENSHOT, "obtainScreenshotDirectory", "Spoon.java", 40)
        .build());

    assertThat(actual.getClassName()).isEqualTo(EXPECTED_CLASS);
    assertThat(actual.getMethodName()).isEqualTo(EXPECTED_METHOD);
  }

  @Test public void directScreenshotCallJUnit4() {
    StackTraceElement actual = findTestClassTraceElement(new StackTraceBuilder() //
        .add("some.Class", "someMethod", "Class.java", 1)
        .add("this.That", "thatThis", "That.java", 2)
        .add(TEST_CASE_CLASS_JUNIT_4, TEST_CASE_METHOD_JUNIT_4, "A.java", 3)
        .add("java.lang.reflect.Method", "invoke", "Native Invoke", 0)
        .add("java.lang.reflect.Method", "invokeNative", "Method.java", 515)
        .add(EXPECTED_CLASS, EXPECTED_METHOD, "Whatever.java", 50)
        .add(SCREENSHOT, "screenshot", "Spoon.java", 30)
        .add(SCREENSHOT, "obtainScreenshotDirectory", "Spoon.java", 40)
        .build());
    assertThat(actual.getClassName()).isEqualTo(EXPECTED_CLASS);
    assertThat(actual.getMethodName()).isEqualTo(EXPECTED_METHOD);
  }

  @Test public void withConvenienceMethodCallJUnit4() {
    StackTraceElement actual = findTestClassTraceElement(new StackTraceBuilder() //
        .add("some.Class", "someMethod", "Class.java", 1)
        .add("this.That", "thatThis", "That.java", 2)
        .add(TEST_CASE_CLASS_JUNIT_4, TEST_CASE_METHOD_JUNIT_4, "A.java", 3)
        .add("java.lang.reflect.Method", "invoke", "Native Invoke", 0)
        .add("java.lang.reflect.Method", "invokeNative", "Method.java", 515)
        .add(EXPECTED_CLASS, EXPECTED_METHOD, "Whatever.java", 50)
        .add("com.example.Utils", "captureScreen", "Utils.java", 100)
        .add(SCREENSHOT, "screenshot", "Spoon.java", 30)
        .add(SCREENSHOT, "obtainScreenshotDirectory", "Spoon.java", 40)
        .build());
    assertThat(actual.getClassName()).isEqualTo(EXPECTED_CLASS);
    assertThat(actual.getMethodName()).isEqualTo(EXPECTED_METHOD);
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidTagThrowsException() {
    Spoon.screenshot(null, "!@#$%^&*()");
  }

  private static class StackTraceBuilder {
    private final List<StackTraceElement> elements = new ArrayList<StackTraceElement>();

    public StackTraceBuilder add(String declaringClass, String methodName, String fileName,
        int lineNumber) {
      // Insert first since the array is in reverse.
      elements.add(0, new StackTraceElement(declaringClass, methodName, fileName, lineNumber));
      return this;
    }

    @SuppressWarnings("ToArrayCallWithZeroLengthArrayArgument") // For type erasure.
    public StackTraceElement[] build() {
      return elements.toArray(new StackTraceElement[0]);
    }
  }
}
