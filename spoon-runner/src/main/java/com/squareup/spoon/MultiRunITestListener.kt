package com.squareup.spoon

import com.android.ddmlib.testrunner.ITestRunListener
import com.android.ddmlib.testrunner.TestIdentifier
import com.google.common.base.Stopwatch
import java.util.concurrent.TimeUnit.SECONDS

/**
 * A [ITestRunListener] which delegates to multiple other [ITestRunListener] instances, except
 * suppressing calls to [ITestRunListener.testRunStarted], [ITestRunListener.testRunEnded], and
 * [ITestRunListener.testRunStopped]. This allows multiple test runs to be aggregated in the
 * `delegates` as if they were a single run.
 */
internal class MultiRunITestListener(val delegates: List<ITestRunListener>) : ITestRunListener {
  private val stopwatch = Stopwatch.createUnstarted()
  private var started = false
  private var stopped = false

  fun multiRunStarted(runName: String?, testCount: Int) {
    check(!started) { "Already started" }
    started = true

    delegates.forEach {
      it.testRunStarted(runName, testCount)
    }
    stopwatch.start()
  }

  fun multiRunEnded() {
    check(started) { "Not started" }
    check(!stopped) { "Already stopped" }
    stopped = true

    stopwatch.elapsed(SECONDS).let { elapsedTime ->
      delegates.forEach {
        it.testRunEnded(elapsedTime, emptyMap())
      }
    }
  }

  override fun testStarted(test: TestIdentifier) {
    delegates.forEach { it.testStarted(test) }
  }

  override fun testAssumptionFailure(test: TestIdentifier, trace: String) {
    delegates.forEach { it.testAssumptionFailure(test, trace) }
  }

  override fun testFailed(test: TestIdentifier, trace: String) {
    delegates.forEach { it.testFailed(test, trace) }
  }

  override fun testEnded(test: TestIdentifier, testMetrics: Map<String, String>) {
    delegates.forEach { it.testEnded(test, testMetrics) }
  }

  override fun testIgnored(test: TestIdentifier) {
    delegates.forEach { it.testIgnored(test) }
  }

  override fun testRunStarted(runName: String, testCount: Int) {}
  override fun testRunStopped(elapsedTime: Long) {}
  override fun testRunEnded(elapsedTime: Long, runMetrics: Map<String, String>) {}
  override fun testRunFailed(errorMessage: String) {
    // TODO something!
  }
}
