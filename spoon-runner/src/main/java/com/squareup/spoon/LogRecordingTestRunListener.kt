package com.squareup.spoon

import com.android.ddmlib.testrunner.ITestRunListener
import com.android.ddmlib.testrunner.TestIdentifier
import com.google.common.collect.ImmutableList
import java.util.LinkedHashSet

/**
 * Listens to an instrumentation invocation where `log=true` is set and records information about
 * the test suite.
 */
internal class LogRecordingTestRunListener : ITestRunListener {
  private val activeTests = LinkedHashSet<TestIdentifier>()
  private val ignoredTests = LinkedHashSet<TestIdentifier>()
  private var runName: String? = null
  private var testCount: Int = 0

  fun activeTests(): List<TestIdentifier> = ImmutableList.copyOf(activeTests)
  fun ignoredTests(): List<TestIdentifier> = ImmutableList.copyOf(ignoredTests)
  fun runName() = runName
  fun testCount() = testCount

  override fun testRunStarted(runName: String, testCount: Int) {
    this.runName = runName
    this.testCount = testCount
  }

  override fun testStarted(test: TestIdentifier) {
    activeTests.add(test)
  }

  override fun testIgnored(test: TestIdentifier) {
    activeTests.remove(test)
    ignoredTests.add(test)
  }

  override fun testFailed(test: TestIdentifier, trace: String) {}
  override fun testAssumptionFailure(test: TestIdentifier, trace: String) {}
  override fun testEnded(test: TestIdentifier, testMetrics: Map<String, String>) {}
  override fun testRunFailed(errorMessage: String) {}
  override fun testRunStopped(elapsedTime: Long) {}
  override fun testRunEnded(elapsedTime: Long, runMetrics: Map<String, String>) {}
}
