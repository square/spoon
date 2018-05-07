package com.squareup.spoon

import com.android.ddmlib.testrunner.TestIdentifier
import com.squareup.spoon.LogRecordingTestRunListener.Companion.stripParametersInClassName
import org.hamcrest.CoreMatchers.*
import org.junit.Assert.assertThat
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

        assertThat(strippedTestIdentifier.className, `is`(className))
    }

    @Test
    fun classNameWithoutSquaredBracketsRemainsUntouched() {
        val testIdentifier = TestIdentifier(className, testMethod)

        val strippedTestIdentifier = stripParametersInClassName(testIdentifier)

        assertThat(strippedTestIdentifier.className, `is`(className))
    }

    @Test
    fun classNameWithSquaredBracketsRemainsUntouched() {
        val testIdentifier = TestIdentifier(classNameWithSquareBrackets, testMethod)

        val strippedTestIdentifier = stripParametersInClassName(testIdentifier)

        assertThat(strippedTestIdentifier.className, `is`(classNameWithSquareBrackets))
    }

    @Test
    fun trackVariantsOfParameterizedTestJustOnce() {
        logRecordingTestRunListener.testStarted(testIdentifierWithParameterOnline)
        logRecordingTestRunListener.testStarted(testIdentifierWithParameterOffline)

        val result = logRecordingTestRunListener.activeTests()

        assertThat(result.size, `is`(1))
        assertThat(result, hasItem(testIdentifierWithoutParameter))
    }

    @Test
    fun removeActiveTestWhenIgnored() {
        logRecordingTestRunListener.testStarted(testIdentifierWithoutParameter)
        logRecordingTestRunListener.testIgnored(testIdentifierWithoutParameter)

        val result = logRecordingTestRunListener.activeTests()

        assertThat(result.size, `is`(0))
    }

    @Test
    fun addIgnoredTest() {
        logRecordingTestRunListener.testIgnored(testIdentifierWithoutParameter)

        val result = logRecordingTestRunListener.ignoredTests()

        assertThat(result.size, `is`(1))
        assertThat(result, hasItem(testIdentifierWithoutParameter))
    }

    @Test
    fun addActiveTest() {
        logRecordingTestRunListener.testStarted(testIdentifierWithoutParameter)

        val result = logRecordingTestRunListener.activeTests()

        assertThat(result.size, `is`(1))
        assertThat(result, hasItem(testIdentifierWithoutParameter))
    }

    @Test
    fun addMultipleActiveTest() {
        logRecordingTestRunListener.testStarted(testIdentifierWithoutParameter)
        logRecordingTestRunListener.testStarted(testIdentifierWithoutParameter2)

        val result = logRecordingTestRunListener.activeTests()

        assertThat(result.size, `is`(2))
        assertThat(result, hasItems(testIdentifierWithoutParameter, testIdentifierWithoutParameter2))
    }

    @Test
    fun addIgnoredTestHasNoImpactOntoActiveTest() {
        logRecordingTestRunListener.testIgnored(testIdentifierWithoutParameter)

        val result = logRecordingTestRunListener.activeTests()

        assertThat(result.size, `is`(0))
    }

    @Test
    fun addActiveTestHasNoImpactOntoIgnoredTest() {
        logRecordingTestRunListener.testStarted(testIdentifierWithoutParameter)

        val result = logRecordingTestRunListener.ignoredTests()

        assertThat(result.size, `is`(0))
    }

    @Test
    fun checkRunName() {
        logRecordingTestRunListener.testRunStarted("NAME", 99)

        assertThat(logRecordingTestRunListener.runName(), `is`("NAME"))
    }

    @Test
    fun checkTestCount() {
        logRecordingTestRunListener.testRunStarted("NAME", 99)

        assertThat(logRecordingTestRunListener.testCount(), `is`(99))
    }
}