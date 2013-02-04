package com.squareup.spoon;

import com.android.ddmlib.testrunner.TestIdentifier;

import static com.google.common.base.Preconditions.checkNotNull;

/** Represents a single test method. */
public final class DeviceTest implements Comparable<DeviceTest> {
  static DeviceTest from(TestIdentifier testIdentifier) {
    return new DeviceTest(testIdentifier.getClassName(), testIdentifier.getTestName());
  }

  private final String className;
  private final String methodName;

  DeviceTest(String className, String methodName) {
    checkNotNull(className);
    checkNotNull(methodName);
    this.className = className;
    this.methodName = methodName;
  }

  /** Test class name. */
  public String getClassName() {
    return className;
  }

  /** Test method name. */
  public String getMethodName() {
    return methodName;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DeviceTest that = (DeviceTest) o;
    if (!className.equals(that.className)) return false;
    if (!methodName.equals(that.methodName)) return false;
    return true;
  }

  @Override public int hashCode() {
    int result = className.hashCode();
    result = 31 * result + methodName.hashCode();
    return result;
  }

  @Override public String toString() {
    return className + "#" + methodName;
  }

  @Override public int compareTo(DeviceTest other) {
    int classCompare = className.compareTo(other.className);
    if (classCompare != 0) {
      return classCompare;
    }
    return methodName.compareTo(other.methodName);
  }
}
