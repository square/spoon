package com.squareup.spoon;

import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.squareup.spoon.SpoonUtils.sanitizeSerial;
import static java.lang.String.format;
import static org.junit.Assert.assertTrue;

public final class SpoonCoverageMergerTest {

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @Test
  public void shouldMergeCoverageFiles() throws Exception {
    String serialId1 = "10.0.0.1:1234";
    String serialId2 = "10.0.0.2:1234";
    File spoonOutputDirectory = testFolder.newFolder("output");
    createTemporaryCoverageFiles(serialId1, serialId2);
    Set<String> serials = ImmutableSet.of(serialId1, serialId2);
    SpoonCoverageMerger spoonCoverageMerger = new SpoonCoverageMerger();

    spoonCoverageMerger.mergeCoverageFiles(serials, spoonOutputDirectory);

    File mergedCoverageFile = new File(spoonOutputDirectory, "/coverage/merged-coverage.ec");
    assertTrue(mergedCoverageFile.exists());
  }

  private void createTemporaryCoverageFiles(String serialId1, String serialId2) throws IOException {
    testFolder.newFolder("output", "coverage", sanitizeSerial(serialId1));
    testFolder.newFolder("output", "coverage", sanitizeSerial(serialId2));
    testFolder.newFile(format("output/coverage/%s/coverage.ec", sanitizeSerial(serialId1)));
    testFolder.newFile(format("output/coverage/%s/coverage.ec", sanitizeSerial(serialId2)));
  }
}
