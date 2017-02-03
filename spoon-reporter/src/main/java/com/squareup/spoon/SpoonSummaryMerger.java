package com.squareup.spoon;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * Takes several {@link SpoonSummary} instances and merges them into a single one
 */
public class SpoonSummaryMerger {

  private final Gson gson;

  public SpoonSummaryMerger(Gson gson) {
    this.gson = gson;
  }

  public SpoonSummary merge(String title, File[] spoonResults) throws FileNotFoundException {
    SpoonSummary[] spoonSummaries = new SpoonSummary[spoonResults.length];
    for (int i = 0; i < spoonResults.length; i++) {
      File result = spoonResults[i];
      Reader reader = new BufferedReader(new FileReader(result));
      SpoonSummary spoonSummary = gson.fromJson(reader, SpoonSummary.class);
      spoonSummaries[i] = spoonSummary;
    }

    return merge(title, spoonSummaries);
  }

  private SpoonSummary merge(String title, SpoonSummary[] spoonSummaries) {
    long start = mergeStartTimes(spoonSummaries);
    long duration = mergeEndTimes(spoonSummaries) - start;

    title = title != null ? title : spoonSummaries[0].getTitle();

    SpoonSummary.Builder builder = new SpoonSummary.Builder()
      .setTitle(title)
      .setStart(start)
      .setDuration(duration);

    Map<String, DeviceResult> testsResultMap = mergeTestResults(spoonSummaries);
    for (Map.Entry<String, DeviceResult> entry : testsResultMap.entrySet()) {
      builder.addResult(entry.getKey(), entry.getValue());
    }

    return builder.build();
  }

  private long mergeStartTimes(SpoonSummary[] spoonSummaries) {
    long earliestStartTime = 0;
    for (SpoonSummary summary : spoonSummaries) {
      if (earliestStartTime == 0 || summary.getStarted() < earliestStartTime) {
        earliestStartTime = summary.getStarted();
      }
    }
    return earliestStartTime;
  }

  private long mergeEndTimes(SpoonSummary[] spoonSummaries) {
    long latestEndTime = 0;
    for (SpoonSummary summary : spoonSummaries) {
      long endTime = summary.getStarted() + summary.getDuration();
      if (latestEndTime == 0 || endTime > latestEndTime) {
        latestEndTime = endTime;
      }
    }
    return latestEndTime;
  }

  private Map<String, DeviceResult> mergeTestResults(SpoonSummary[] spoonSummaries) {
    Map<String, DeviceResult> allTestResults = new HashMap<String, DeviceResult>();

    for (SpoonSummary spoonSummary : spoonSummaries) {
      for (Map.Entry<String, DeviceResult> result : spoonSummary.getResults().entrySet()) {
        String deviceSerial = result.getKey();

        if (!allTestResults.containsKey(deviceSerial)) {
          allTestResults.put(deviceSerial, result.getValue());
        } else {
          DeviceResult master = allTestResults.get(deviceSerial);
          DeviceResult addition = result.getValue();

          mergeTestResults(master, addition);
        }

      }
    }

    return allTestResults;
  }

  /**
   * Adds entries from one {@link DeviceResult} to another. If both contain an entry
   * for the same {@link DeviceTest}, then the value of the master will be used.
   *
   * @param master   the {@link DeviceResult} that will be added to.
   * @param addition the {@link }
   */
  private void mergeTestResults(DeviceResult master, DeviceResult addition) {
    Map<DeviceTest, DeviceTestResult> masterResults = master.getTestResults();
    Map<DeviceTest, DeviceTestResult> additionResults = addition.getTestResults();

    for (Map.Entry<DeviceTest, DeviceTestResult> additionEntry : additionResults.entrySet()) {
      if (!masterResults.containsKey(additionEntry.getKey())) {
        masterResults.put(additionEntry.getKey(), additionEntry.getValue());
      }
    }
  }

}
