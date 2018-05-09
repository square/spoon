package com.squareup.spoon

import com.android.annotations.VisibleForTesting
import com.android.ddmlib.testrunner.ITestRunListener
import com.android.ddmlib.testrunner.TestIdentifier
import com.google.common.collect.ImmutableList
import java.util.LinkedHashSet

/**
 * Listens to an instrumentation invocation where `log=true` is set and records information about
 * the test suite.
 */
internal class LogRecordingTestRunListener : ITestRunListener {
  companion object {
    private val parameterRegex = "\\[.*\\]$".toRegex()

    @VisibleForTesting
    fun stripParametersInClassName(test: TestIdentifier): TestIdentifier {
      val className = test.className.replace(parameterRegex, "")
      return TestIdentifier(className, test.testName)
    }
  }

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
    val newTest = stripParametersInClassName(test)
    activeTests.add(newTest)
  }

  override fun testIgnored(test: TestIdentifier) {
    val newTest = stripParametersInClassName(test)
    activeTests.remove(newTest)
    ignoredTests.add(newTest)
  }

  override fun testFailed(test: TestIdentifier, trace: String) {}
  override fun testAssumptionFailure(test: TestIdentifier, trace: String) {}
  override fun testEnded(test: TestIdentifier, testMetrics: Map<String, String>) {}
  override fun testRunFailed(errorMessage: String) {}
  override fun testRunStopped(elapsedTime: Long) {}
  override fun testRunEnded(elapsedTime: Long, runMetrics: Map<String, String>) {}
}
