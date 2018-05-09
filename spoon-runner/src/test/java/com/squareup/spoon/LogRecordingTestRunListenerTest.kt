package com.squareup.spoon

import com.android.ddmlib.testrunner.TestIdentifier
import com.google.common.truth.Truth.assertThat
import com.squareup.spoon.LogRecordingTestRunListener.Companion.stripParametersInClassName
import org.junit.Before
import org.junit.Test

class LogRecordingTestRunListenerTest {

  private val className = "com.some.BusinessTest"
  private val classNameWithParameterOnline = "$className[ONLINE]"
  private val classNameWithParameterOffline = "$className[OFFLINE]"
  private val classNameWithSquareBrackets = "com.some.[ONLINE]BusinessTest"

  private val testMethod = "someTest"

  private val testIdentifierWithoutParameter =
          TestIdentifier(className, testMethod)
  private val testIdentifierWithoutParameter2 =
          TestIdentifier("${className}2", testMethod)
  private val testIdentifierWithParameterOnline =
          TestIdentifier(classNameWithParameterOnline, testMethod)
  private val testIdentifierWithParameterOffline =
          TestIdentifier(classNameWithParameterOffline, testMethod)

  private lateinit var logRecordingTestRunListener: LogRecordingTestRunListener

  @Before
  fun setUp() {
    logRecordingTestRunListener = LogRecordingTestRunListener()
  }

  @Test
  fun stripParametersInClassNameOfParameterizedTest() {
    val testIdentifier = TestIdentifier(classNameWithParameterOnline, testMethod)

    val strippedTestIdentifier = stripParametersInClassName(testIdentifier)

    assertThat(strippedTestIdentifier.className).isEqualTo(className)
  }

  @Test
  fun classNameWithoutSquaredBracketsRemainsUntouched() {
    val testIdentifier = TestIdentifier(className, testMethod)

    val strippedTestIdentifier = stripParametersInClassName(testIdentifier)

    assertThat(strippedTestIdentifier.className).isEqualTo(className)
  }

  @Test
  fun classNameWithSquaredBracketsRemainsUntouched() {
    val testIdentifier = TestIdentifier(classNameWithSquareBrackets, testMethod)

    val strippedTestIdentifier = stripParametersInClassName(testIdentifier)

    assertThat(strippedTestIdentifier.className).isEqualTo(classNameWithSquareBrackets)
  }

  @Test
  fun trackVariantsOfParameterizedTestJustOnce() {
    logRecordingTestRunListener.testStarted(testIdentifierWithParameterOnline)
    logRecordingTestRunListener.testStarted(testIdentifierWithParameterOffline)

    val result = logRecordingTestRunListener.activeTests()

    assertThat(result).containsExactly(testIdentifierWithoutParameter)
  }

  @Test
  fun removeActiveTestWhenIgnored() {
    logRecordingTestRunListener.testStarted(testIdentifierWithoutParameter)
    logRecordingTestRunListener.testIgnored(testIdentifierWithoutParameter)

    val result = logRecordingTestRunListener.activeTests()

    assertThat(result).isEmpty()
  }

  @Test
  fun addIgnoredTest() {
    logRecordingTestRunListener.testIgnored(testIdentifierWithoutParameter)

    val result = logRecordingTestRunListener.ignoredTests()

    assertThat(result).containsExactly(testIdentifierWithoutParameter)
  }

  @Test
  fun addActiveTest() {
    logRecordingTestRunListener.testStarted(testIdentifierWithoutParameter)

    val result = logRecordingTestRunListener.activeTests()

    assertThat(result).containsExactly(testIdentifierWithoutParameter)
  }

  @Test
  fun addMultipleActiveTest() {
    logRecordingTestRunListener.testStarted(testIdentifierWithoutParameter)
    logRecordingTestRunListener.testStarted(testIdentifierWithoutParameter2)

    val result = logRecordingTestRunListener.activeTests()

    assertThat(result).containsExactly(testIdentifierWithoutParameter, testIdentifierWithoutParameter2)
  }

  @Test
  fun addIgnoredTestHasNoImpactOntoActiveTest() {
    logRecordingTestRunListener.testIgnored(testIdentifierWithoutParameter)

    val result = logRecordingTestRunListener.activeTests()

    assertThat(result).isEmpty()
  }

  @Test
  fun addActiveTestHasNoImpactOntoIgnoredTest() {
    logRecordingTestRunListener.testStarted(testIdentifierWithoutParameter)

    val result = logRecordingTestRunListener.ignoredTests()

    assertThat(result).isEmpty()
  }

  @Test
  fun checkRunName() {
    logRecordingTestRunListener.testRunStarted("NAME", 99)

    assertThat(logRecordingTestRunListener.runName()).isEqualTo("NAME")
  }

  @Test
  fun checkTestCount() {
    logRecordingTestRunListener.testRunStarted("NAME", 99)

    assertThat(logRecordingTestRunListener.testCount()).isEqualTo(99)
  }
}