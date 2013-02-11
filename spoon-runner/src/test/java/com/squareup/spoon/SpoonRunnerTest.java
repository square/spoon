package com.squareup.spoon;

import org.junit.Test;

import static com.squareup.spoon.SpoonRunner.parseOverallSuccess;
import static org.fest.assertions.api.Assertions.assertThat;

public class SpoonRunnerTest {
  @Test public void parsingSuccess() {
    SpoonSummary summary;
    DeviceTest device = new DeviceTest("foo", "bar");

    // PASS: No devices attached.
    summary = new SpoonSummary.Builder() //
        .setTitle("test") //
        .build(); //
    assertThat(parseOverallSuccess(summary)).isTrue();

    // FAIL: Unable to install application.
    summary = new SpoonSummary.Builder() //
        .setTitle("test") //
        .start() //
        .addResult("123", new DeviceResult.Builder() //
            .markInstallAsFailed("") //
            .build()) //
        .end() //
        .build(); //
    assertThat(parseOverallSuccess(summary)).isFalse();

    // FAIL: Top-level exception, no tests run.
    summary = new SpoonSummary.Builder() //
        .setTitle("test") //
        .start() //
        .addResult("123", new DeviceResult.Builder() //
            .addException(new RuntimeException()) //
            .build()) //
        .end() //
        .build(); //
    assertThat(parseOverallSuccess(summary)).isFalse();

    // PASS: No tests run.
    summary = new SpoonSummary.Builder() //
        .setTitle("test") //
        .start() //
        .addResult("123", new DeviceResult.Builder() //
            .startTests() //
            .endTests() //
            .build()) //
        .end() //
        .build(); //
    assertThat(parseOverallSuccess(summary)).isTrue();

    // FAIL: Test failure.
    summary = new SpoonSummary.Builder() //
        .setTitle("test") //
        .start() //
        .addResult("123", new DeviceResult.Builder() //
            .startTests() //
            .addTestResultBuilder(device, new DeviceTestResult.Builder() //
                .startTest() //
                .markTestAsFailed("java.fake.Exception: Failed!")
                .endTest()) //
            .build()) //
        .end() //
        .build(); //
    assertThat(parseOverallSuccess(summary)).isFalse();

    // FAIL: Test error.
    summary = new SpoonSummary.Builder() //
        .setTitle("test") //
        .start() //
        .addResult("123", new DeviceResult.Builder() //
            .startTests() //
            .addTestResultBuilder(device, new DeviceTestResult.Builder() //
                .startTest() //
                .markTestAsError("java.fake.Exception: Failed!")
                .endTest()) //
            .build()) //
        .end() //
        .build(); //
    assertThat(parseOverallSuccess(summary)).isFalse();

    // PASS: Test success.
    summary = new SpoonSummary.Builder() //
        .setTitle("test") //
        .start() //
        .addResult("123", new DeviceResult.Builder() //
            .startTests() //
            .addTestResultBuilder(device, new DeviceTestResult.Builder() //
                .startTest() //
                .endTest()) //
            .build()) //
        .end() //
        .build(); //
    assertThat(parseOverallSuccess(summary)).isTrue();
  }
}
