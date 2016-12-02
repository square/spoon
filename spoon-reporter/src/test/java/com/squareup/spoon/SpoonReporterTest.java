package com.squareup.spoon;

import com.android.ddmlib.Log;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class SpoonReporterTest {

  @Test
  public void testRun() throws Exception {

    File input = new File("/home/todorus/projects/spoon/spoon-reporter/src/test/resources/sample/result.json");
    SpoonReporter spoonReporter = new SpoonReporter(SpoonUtils.GSON, "test title", new File[]{input}, new File("/home/todorus/projects/spoon/spoon-reporter/src/test/resources/merged"));
    spoonReporter.run();
    Log.d("TEST", "TEST");
  }


}