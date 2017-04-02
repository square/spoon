package com.squareup.spoon;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import org.jacoco.core.tools.ExecFileLoader;

import static com.google.common.collect.Collections2.transform;
import static com.squareup.spoon.SpoonDeviceRunner.COVERAGE_DIR;
import static com.squareup.spoon.SpoonDeviceRunner.COVERAGE_FILE;

final class SpoonCoverageMerger {
  private static final String MERGED_COVERAGE_FILE = "merged-coverage.ec";
  private ExecFileLoader execFileLoader;

  SpoonCoverageMerger() {
    this.execFileLoader = new ExecFileLoader();
  }

  public void mergeCoverageFiles(Set<String> serials, File outputDirectory) throws IOException {
    Collection<String> sanitizeSerials = transform(serials, SpoonUtils::sanitizeSerial);
    for (String serial : sanitizeSerials) {
      String coverageFilePath = COVERAGE_DIR + "/" + serial + "/" + COVERAGE_FILE;
      execFileLoader.load(new File(outputDirectory, coverageFilePath));
    }
    String mergedCoverageFile = COVERAGE_DIR + "/" + MERGED_COVERAGE_FILE;
    execFileLoader.save(new File(outputDirectory, mergedCoverageFile), false);
  }
}
