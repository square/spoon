package com.squareup.spoon;

import com.google.common.io.Files;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.assertTrue;

public class SpoonReporterTest {

  @Test
  public void run() throws Exception {
    URL fileUrl = getClass().getResource("/sample/result.json");
    File input = new File(fileUrl.getFile());
    File output = Files.createTempDir();

    SpoonReporter reporter = new SpoonReporter(SpoonUtils.GSON, "test title", new File[]{input}, output);
    reporter.run();

    assertTrue(true);
  }

}