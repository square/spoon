// Copyright 2012 Square, Inc.
package com.squareup.spoon;

import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public final class SpoonRuleTest {
  @Rule public final SpoonRule spoon = new SpoonRule();
  private static final String className = SpoonRuleTest.class.getName();
  private static final String dirPath = className+"_random/SpoonRuleTest/";
  private static final String pngName = dirPath+className+"_some.png";
  private static final String xmlName = dirPath+className+"_some.xml";
  @After
  public void tearDown() {
    safeDelete(new File(xmlName));
    safeDelete(new File(dirPath));
    safeDelete(new File(pngName));
  }

  private void safeDelete(File file) {
    if(file.exists()) {
      file.delete();
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidTagThrowsException() {
    spoon.screenshot(null, "!@#$%^&*()");
  }

  @Test(expected = IllegalArgumentException.class)
  public void screenshotWithFileThrowsExceptionWhenInvalidTag() {
    spoon.screenshot(null, "!@#$%^&*()",null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void screenshotWithFileThrowsExceptionWhenNullFile() {
    spoon.screenshot(null, "SOME_TAG",null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void screenshotWithFileThrowsExceptionWhenFileDoesNotExist() {
    spoon.screenshot(null, "SOME_TAG",new File(pngName));
  }

  @Test(expected = IllegalArgumentException.class)
  public void screenshotWithFileThrowsExceptionWhenFileExtentionNotPNG() throws IOException {

    File file = new File(xmlName);
    file.getParentFile().mkdirs();
    file.createNewFile();

    Assert.assertTrue(file.exists());
    Assert.assertTrue(file.isFile());
    spoon.screenshot(null, "SOME_TAG",file);

  }

  @Test(expected = IllegalArgumentException.class)
  public void screenshotWithFileThrowsExceptionWhenFileIsDir() throws IOException {

    File file = new File(dirPath);
    file.mkdirs();

    Assert.assertTrue(file.exists());
    Assert.assertTrue(file.isDirectory());
    spoon.screenshot(null, "SOME_TAG",file);

  }

  @Test(expected = NullPointerException.class)
  public void screenshotWithFileThrowsExceptionWhenContextIsNull() throws IOException {

    File file = new File(pngName);
    file.getParentFile().mkdirs();
    file.createNewFile();

    Assert.assertTrue(file.exists());
    Assert.assertTrue(file.isFile());
    spoon.screenshot(null, "SOME_TAG",file);

  }


}
