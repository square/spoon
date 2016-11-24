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
    gson = new Gson();
  }

  @Test
  public void merge() throws Exception {
    // Given I have multiple SpoonSummaries
    generateSummaries();
    // And they are in JSON format
    File result1 = renderResult(summary1);
    File result2 = renderResult(summary2);
    File[] results = new File[]{result1, result2};

    // When I let it merge these
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

    assertEquals(resultMap1.size() + resultMap2.size(), resultMapMerged);
    assertMapContainsResults(resultMap1, resultMapMerged);
    assertMapContainsResults(resultMap2, resultMapMerged);
  }

  private void generateSummaries() throws InterruptedException {

    summary1 = new SpoonSummary.Builder()
      .setTitle("summary1")
      .start()
      .addResult("result1", generateDeviceResult(new DeviceTest("class1", "method1")))
      .addResult("result2", generateDeviceResult(new DeviceTest("class1", "method2")))
      .addResult("result3", generateDeviceResult(new DeviceTest("class2", "method1")))
      .end()
      .build();

    // force a later starting time for the second summary
    Thread.sleep(100);

    summary2 = new SpoonSummary.Builder()
      .setTitle("summary1")
      .start()
      .addResult("result1", generateDeviceResult(new DeviceTest("class1", "method1")))
      .addResult("result2", generateDeviceResult(new DeviceTest("class1", "method2")))
      .addResult("result3", generateDeviceResult(new DeviceTest("class2", "method1")))
      .end()
      .build();

    // sanity check
    assertTrue(summary1.getStarted() < summary2.getStarted());
  }

  private DeviceResult generateDeviceResult(DeviceTest deviceTest) {
    return new DeviceResult.Builder()
      .startTests()
      .addTestResultBuilder(deviceTest, new DeviceTestResult.Builder()
        .startTest()
        .endTest())
      .build();
  }

  private File renderResult(SpoonSummary spoonSummary) throws Exception {
    // This method is essentially a rewrite of HtmlRenderer.writeToJson().
    // It would be much nicer to use the actual rendering function as a
    // reference instead.

    File file = File.createTempFile(spoonSummary.getTitle(), "json");
    FileWriter result = new FileWriter(file);
    gson.toJson(spoonSummary, result);
    return file;
  }

  private void assertMapContainsResults(Map<String, DeviceResult> values, Map<String, DeviceResult> container){
    for (Map.Entry<String, DeviceResult> entry : values.entrySet()) {
      String key = entry.getKey();
      DeviceResult value = entry.getValue();
      assertEquals(value, container.get(key));
    }
  }

}