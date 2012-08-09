package com.squareup.spoon;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.squareup.spoon.model.RunConfig;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ExecutionSummary {
  public final RunConfig config;
  public final List<ExecutionResult> results = Collections.synchronizedList(new ArrayList<ExecutionResult>());
  public final List<Exception> exceptions = Collections.synchronizedList(new ArrayList<Exception>());
  public long testStart;
  public long testEnd;
  public int totalTests;
  public int totalSuccess;
  public int totalFailure;
  public long totalTime;
  public String displayTime;

  public ExecutionSummary(RunConfig config) {
    this.config = config;
  }

  public void updateDynamicValues() {
    totalTests = 0;
    totalSuccess = 0;
    totalFailure = 0;
    for (ExecutionResult result : results) {
      totalTests += result.testsStarted;
      totalSuccess += result.testsStarted - result.testsFailed;
      totalFailure += result.testsFailed;
    }

    totalTime = (testEnd - testStart) / 1000;

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");
    displayTime = sdf.format(new Date(testEnd));
  }

  public void generateHtml() {
    updateDynamicValues();

    copyResourceToOutput("bootstrap.min.css", config.output);
    copyResourceToOutput("spoon.css", config.output);

    Mustache m = new DefaultMustacheFactory().compile("index.mustache");
    try {
      m.execute(new FileWriter(new File(config.output, "index.html")), this).flush();
    } catch (IOException e) {
      throw new RuntimeException("Unable to write output index.html", e);
    }
  }

  private static void copyResourceToOutput(String resource, File outputDirectory) {
    InputStream is = null;
    OutputStream os = null;
    try {
      is = ExecutionSummary.class.getResourceAsStream("/" + resource);
      os = new FileOutputStream(new File(outputDirectory, resource));
      IOUtils.copy(is, os);
    } catch (IOException e) {
      throw new RuntimeException("Unable to copy resource " + resource, e);
    } finally {
      try {
        if (is != null) is.close();
      } catch (Exception ignored) {
      }
      try {
        if (os != null) os.close();
      } catch (Exception ignored) {
      }
    }
  }
}
