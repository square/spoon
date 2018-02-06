package com.squareup.spoon;

import java.io.File;
import java.io.IOException;

/**
 * An {@link com.android.ddmlib.testrunner.XmlTestRunListener XmlTestRunListener} that points
 * directly to an output file.
 */
class XmlTestRunListener extends com.android.ddmlib.testrunner.XmlTestRunListener {
  private final File file;

  XmlTestRunListener(File file) {
    if (file == null) {
      throw new IllegalArgumentException("File may not be null.");
    }
    this.file = file;
  }

  @Override
  public void testRunStarted(String runName, int numTests) {
    getRunResult().testRunStarted(runName, numTests);
  }

  @Override protected File getResultFile(File reportDir) throws IOException {
    file.getParentFile().mkdirs();
    return file;
  }
}
