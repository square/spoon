package com.squareup.spoon;

import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SpoonSummaryMergerTest {

  private Gson gson;
  private SpoonSummary summary1;
  private SpoonSummary summary2;

  @Before
  public void setUp() throws Exception {
    gson = SpoonUtils.GSON;
  }

  @Test
  public void mergeAcrossMultipleTestMachines() throws Exception {
    // Given I have multiple SpoonSummaries, from multiple machines that run the same tests
    SpoonSummary[] generated = TestUtils.generateSummariesForMultipleMachines(2);
    summary1 = generated[0];
    summary2 = generated[1];
    // And they are in JSON format
    File result1 = TestUtils.renderResult(gson, summary1);
    File result2 = TestUtils.renderResult(gson, summary2);
    File[] results = new File[]{result1, result2};

    // When I let it merge these summaries
    SpoonSummaryMerger subject = new SpoonSummaryMerger(gson);
    SpoonSummary merged = subject.merge(results);

    // Then it should create one SpoonSummary
    assertNotNull(merged);
    // And it should use the oldest time as the start time
    assertEquals(summary1.getStarted(), merged.getStarted());
    // And it should contain the results of all summaries
    Map<String, DeviceResult> resultMap1 = summary1.getResults();
    Map<String, DeviceResult> resultMap2 = summary2.getResults();
    Map<String, DeviceResult> resultMapMerged = merged.getResults();

    assertEquals(resultMap1.size() + resultMap2.size(), resultMapMerged.size());
    TestUtils.assertMapContainsResults(resultMap1, resultMapMerged);
    TestUtils.assertMapContainsResults(resultMap2, resultMapMerged);
  }



}