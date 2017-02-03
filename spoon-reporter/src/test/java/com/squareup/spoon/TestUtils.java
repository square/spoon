package com.squareup.spoon;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Utility class for mock data creation and checking test results
 */
public class TestUtils {

  public static SpoonSummary[] generateSummariesForMultipleMachines(int count) throws InterruptedException, IOException {

    SpoonSummary[] summaries = new SpoonSummary[count];
    for(int i=0;i < count; i++){
      SpoonSummary spoonSummary = new SpoonSummary.Builder()
        .setTitle("summary"+i)
        .start()
        .addResult(
          "device_"+i+"1",
          generateDeviceResult(
            new DeviceTest("class1", "method1"),
            new DeviceTest("class1", "method2")
          )
        )
        .addResult(
          "device_"+i+"2",
          generateDeviceResult(
            new DeviceTest("class1", "method1"),
            new DeviceTest("class1", "method2")
          )
        )
        .end()
        .build();

      summaries[i] = spoonSummary;

      // force a later starting time for the next summary
      Thread.sleep(100);
    }

    return summaries;
  }

  private static DeviceResult generateDeviceResult(DeviceTest... deviceTests) throws IOException {
    DeviceResult.Builder builder = new DeviceResult.Builder();
    builder.startTests();

    for (DeviceTest deviceTest : deviceTests) {
      builder.addTestResultBuilder(
        deviceTest,
        new DeviceTestResult.Builder()
          .startTest()
          .addScreenshot(File.createTempFile(deviceTest.getClassName(), deviceTest.getMethodName()+".png"))
          .endTest()
      );
    }

    return builder.build();
  }

  public static File renderResult(Gson gson, SpoonSummary spoonSummary) throws Exception {
    // This method is essentially a rewrite of HtmlRenderer.writeToJson().
    // It would be much nicer to use the actual rendering function as a
    // reference instead.

    File file = File.createTempFile(spoonSummary.getTitle(), "json");
    FileWriter result = new FileWriter(file);
    gson.toJson(spoonSummary, result);
    result.close();

    return file;
  }

  public static void assertMapContainsResults(Map<String, DeviceResult> values, Map<String, DeviceResult> container) {
    for (Map.Entry<String, DeviceResult> entry : values.entrySet()) {
      String key = entry.getKey();
      DeviceResult value = entry.getValue();
      DeviceResult containerValue = container.get(key);
      assertEquals(value, containerValue);
    }
  }

}
