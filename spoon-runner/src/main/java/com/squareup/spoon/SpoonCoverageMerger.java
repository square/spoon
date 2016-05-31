package com.squareup.spoon;

import com.google.common.base.Function;
import org.jacoco.core.tools.ExecFileLoader;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import static com.google.common.collect.Collections2.transform;
import static com.squareup.spoon.SpoonDeviceRunner.COVERAGE_DIR;
import static com.squareup.spoon.SpoonDeviceRunner.COVERAGE_FILE;
import static com.squareup.spoon.SpoonUtils.sanitizeSerial;

final class SpoonCoverageMerger {
  private static final String MERGED_COVERAGE_FILE = "merged-coverage.ec";
  private ExecFileLoader execFileLoader;

  public SpoonCoverageMerger() {
    this.execFileLoader = new ExecFileLoader();
  }

  public void mergeCoverageFiles(Set<String> serials, File outputDirectory) throws IOException {
    Collection<String> sanitizeSerials = transform(serials, toSanitizeSerials());
    for (String serial : sanitizeSerials) {
      String coverageFilePath = COVERAGE_DIR + "/" + serial + "/" + COVERAGE_FILE;
      execFileLoader.load(new File(outputDirectory, coverageFilePath));
    }
    String mergedCoverageFile = COVERAGE_DIR + "/" + MERGED_COVERAGE_FILE;
    execFileLoader.save(new File(outputDirectory, mergedCoverageFile), false);
  }

  private Function<String, String> toSanitizeSerials() {
    return new Function<String, String>() {
      @Override
      public String apply(String serials) {
        return sanitizeSerial(serials);
      }
    };
  }
}
