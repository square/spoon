package com.squareup.spoon;

import com.android.ddmlib.IDevice;
import com.android.ddmlib.testrunner.ITestRunListener;
import com.android.ddmlib.testrunner.TestIdentifier;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.squareup.spoon.ExecutionSummary.DISPLAY_TIME;
import static com.squareup.spoon.ExecutionTestResult.TestResult.FAILURE;
import static com.squareup.spoon.ExecutionTestResult.TestResult.SUCCESS;

/** Represents the aggregated result of a test execution on a device. */
public class ExecutionResult implements ITestRunListener {
  public final String serial;
  public int testsStarted;
  public int testsFailed;
  public int testsPassed;
  public String deviceName;
  public String deviceManufacturer;
  public String deviceVersion;
  public String deviceApiLevel;
  public String deviceLanguage;
  public String deviceRegion;
  public long testStart;
  public long testEnd;
  public Date testCompleted;
  public long totalTime;
  public String displayTime;
  private final Map<String, InstrumentationTestClass> testClasses =
    new HashMap<String, InstrumentationTestClass>();
  public Exception runtimeException;

  public ExecutionResult(String serial) {
    this.serial = serial;
  }

  @Override public void testRunStarted(String runName, int testCount) {
    System.out.println("[testRunStarted] runName: " + runName + ", " + testCount);
  }

  @Override public void testStarted(TestIdentifier testIdentifier) {
    System.out.println("[testStarted] test: " + testIdentifier);
    if (!testClasses.containsKey(testIdentifier.getClassName())) {
      testClasses.put(testIdentifier.getClassName(), new InstrumentationTestClass(testIdentifier));
    }

    InstrumentationTestClass testClass = testClasses.get(testIdentifier.getClassName());
    if (!testClass.containsTest(testIdentifier)) {
      testClass.addTest(new InstrumentationTest(testIdentifier));
    }

    ExecutionTestResult result = new ExecutionTestResult(testIdentifier);
    result.deviceName = deviceName;
    result.serial = serial;
    testClass.getTest(testIdentifier).createResult(serial, result);

    testsStarted += 1;
  }

  @Override public void testFailed(TestFailure status, TestIdentifier identifier, String trace) {
    System.out.println("[testFailed] status: " + status + ", test: " + identifier + ", trace: "
      + trace);
    getTest(identifier).setResult(serial, FAILURE);
    testsFailed += 1;
    testClasses.get(identifier.getClassName()).testsFailed += 1;
  }

  @Override public void testEnded(TestIdentifier identifier, Map<String, String> metrics) {
    System.out.println("[testEnded] test: " + identifier + ", metrics: " + metrics);
    getTest(identifier).setResult(serial, SUCCESS);
    testsPassed += 1;
    testClasses.get(identifier.getClassName()).testsPassed += 1;
  }

  @Override public void testRunFailed(String errorMessage) {
    System.out.println("[testRunFailed] errorMessage: " + errorMessage);
  }

  @Override public void testRunStopped(long elapsedTime) {
    System.out.println("[testRunStopped] elapsedTime: " + elapsedTime);
  }

  @Override public void testRunEnded(long elapsedTime, Map<String, String> metrics) {
    System.out.println("[testRunEnded] elapsedTime: " + elapsedTime + ", metrics: " + metrics);
  }

  public InstrumentationTest getTest(TestIdentifier identifier) {
    return testClasses.get(identifier.getClassName()).getTest(identifier);
  }

  public void setRuntimeException(Exception exception) {
    runtimeException = exception;
  }

  public Exception getRuntimeException() {
    return runtimeException;
  }

  /** Add a class-level screenshot directory to this execution result. */
  // TODO this begs for a DB
  void addScreenshotDirectory(File classNameDir) {
    File[] testNameDirs = classNameDir.listFiles();
    if (testNameDirs != null) {
      // Loop over all of the test directories inside the class directory.
      for (File testNameDir : testNameDirs) {
        for (InstrumentationTest instrumentationTest : tests()) {
          if (instrumentationTest.className.equals(classNameDir.getName())
              && instrumentationTest.testName.equals(testNameDir.getName())) {
            // If we have matched both class name and test name, add all screenshots to the result.
            ExecutionTestResult result = instrumentationTest.getResult(serial);
            for (File screenshotFile : testNameDir.listFiles()) {
              result.screenshots.add(result.new Screenshot(screenshotFile));
            }
            break; // Continue to next test dir
          }
        }
      }
    }
  }

  /** Mustache can't read maps. Feed it a list to consume. Nom nom nom. */
  public List<InstrumentationTest> tests() {
    List<InstrumentationTestClass> allClasses =
      new ArrayList<InstrumentationTestClass>(testClasses.values());
    List<InstrumentationTest> allTests = new ArrayList<InstrumentationTest>();
    for (InstrumentationTestClass testClass : allClasses) {
      allTests.addAll(testClass.tests());
    }

    Collections.sort(allTests, new Comparator<InstrumentationTest>() {
      @Override public int compare(InstrumentationTest instrumentationTest,
                                   InstrumentationTest other) {
        int className = instrumentationTest.className.compareTo(other.className);
        if (className != 0) {
          return className;
        }
        return instrumentationTest.testName.compareTo(other.testName);
      }
    });
    return allTests;
  }

  /** For similar reasons as {@link #tests()}, we need a list of test results. Mmmmm. */
  public List<InstrumentationTestClass> testClasses() {
    return new ArrayList<InstrumentationTestClass>(testClasses.values());
  }

  /** For similar reasons as {@link #tests()}, we need a list of test results. Omnomnom. */
  public List<ExecutionTestResult> results() {
    List<ExecutionTestResult> results = new ArrayList<ExecutionTestResult>();
    for (InstrumentationTest instrumentationTest : tests()) {
      results.add(instrumentationTest.getResult(serial));
    }
    return results;
  }

  public void configureFor(IDevice realDevice) {
    this.deviceName = realDevice.getProperty("ro.product.model");
    this.deviceManufacturer = realDevice.getProperty("ro.product.manufacturer");
    this.deviceVersion = realDevice.getProperty("ro.build.version.release");
    this.deviceApiLevel = realDevice.getProperty("ro.build.version.sdk");
    this.deviceLanguage = realDevice.getProperty("ro.product.locale.language");
    this.deviceRegion = realDevice.getProperty("ro.product.locale.region");
  }

  public void updateDynamicValues() {
    totalTime = TimeUnit.NANOSECONDS.toSeconds(testEnd - testStart);
    displayTime = DISPLAY_TIME.get().format(testCompleted);
  }
}
