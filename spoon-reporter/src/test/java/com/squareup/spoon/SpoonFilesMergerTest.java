package com.squareup.spoon;

import com.android.ddmlib.Log;
import com.google.common.io.Files;
import com.google.gson.Gson;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class SpoonFilesMergerTest {
  @Test
  public void merge() throws Exception {
    Gson gson = SpoonUtils.GSON;

    // Given I have multiple SpoonSummaries, from multiple machines that run the same tests
    SpoonSummary[] generated = TestUtils.generateSummariesForMultipleMachines(2);
    SpoonSummary summary1 = generated[0];
    SpoonSummary summary2 = generated[1];
    // And they are in JSON format
    File result1 = TestUtils.renderResult(gson, summary1);
    File result2 = TestUtils.renderResult(gson, summary2);
    File[] originalResults = new File[]{result1, result2};

    // When I let it merge these summaries
    File outputDir = Files.createTempDir();
    File[] modifiedResults = SpoonFilesMerger.copyAndRewrite(outputDir, originalResults);

    // Then it should rewrite the summary files with the new paths
    for(int i = 0; i < originalResults.length; i++){
      SpoonSummary original = gson.fromJson(new FileReader(originalResults[i]), SpoonSummary.class);
      SpoonSummary modified = gson.fromJson(new FileReader(modifiedResults[i]), SpoonSummary.class);
      checkModifiedFiles(outputDir, original, modified);
    }
    // And copy the files to these new paths
    // TODO find out if this is testable with temporary files
  }

  private void checkModifiedFiles(File outputDir, SpoonSummary original, SpoonSummary modified) {
    DeviceResult[] originalDeviceResults = new DeviceResult[original.getResults().values().size()];
    original.getResults().values().toArray(originalDeviceResults);
    DeviceResult[] modifiedDeviceResults = new DeviceResult[modified.getResults().values().size()];
    modified.getResults().values().toArray(modifiedDeviceResults);

    for(int i=0; i < originalDeviceResults.length; i++){
      DeviceResult originalDeviceResult = originalDeviceResults[i];
      DeviceResult modifiedDeviceResult = modifiedDeviceResults[i];

      DeviceTestResult[] originalDeviceTestResults = new DeviceTestResult[originalDeviceResult.getTestResults().size()];
      originalDeviceResult.getTestResults().values().toArray(originalDeviceTestResults);
      DeviceTestResult[] modifiedDeviceTestResults = new DeviceTestResult[modifiedDeviceResult.getTestResults().size()];
      modifiedDeviceResult.getTestResults().values().toArray(modifiedDeviceTestResults);

      for(int j=0; j < originalDeviceTestResults.length; j++){
        DeviceTestResult originalDeviceTestResult = originalDeviceTestResults[j];
        DeviceTestResult modifiedDeviceTestResult = modifiedDeviceTestResults[j];

        List<File> originalScreenshots = originalDeviceTestResult.getScreenshots();
        List<File> modifiedScreenshots = modifiedDeviceTestResult.getScreenshots();

        for(int k=0; k < originalScreenshots.size(); k++) {
          File originalScreenshot = originalScreenshots.get(k);
          File modifiedScreenshot = modifiedScreenshots.get(k);

          String expectedPath = new File(outputDir, originalScreenshot.getName()).getPath();
          assertEquals(expectedPath, modifiedScreenshot.getPath());
        }
      }
    }
  }

}