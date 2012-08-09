package com.squareup.spoon;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;

import java.io.*;

/**
 * TODO DOC
 */
public class Screenshot {

  private static final String SPOON_SCREENSHOTS = "spoon-screenshots";

  /**
   * @param activity
   * @param tag
   */
  public static void snap(Activity activity, String tag) {
    try {
      File screenshotsDir = activity.getDir(SPOON_SCREENSHOTS, Context.MODE_WORLD_READABLE);

      final StackTraceElement element = new Throwable().getStackTrace()[1];
      String testcaseName = element.getClassName();
      String testName = element.getMethodName();

      final File testcaseDir = getOrCreateDir(screenshotsDir, testcaseName);
      final File testDir = getOrCreateDir(testcaseDir, testName);

      takeScreenshot(new File(testDir, tag + ".png"), activity);
    } catch (Exception e) {
      throw new RuntimeException("Unable to take screenshot", e);
    }
  }

  private static void takeScreenshot(File file, Activity activity) throws IOException {
    Bitmap bitmap;
    View v1 = activity.getWindow().getDecorView();
    v1.setDrawingCacheEnabled(true);
    bitmap = Bitmap.createBitmap(v1.getDrawingCache());
    v1.setDrawingCacheEnabled(false);

    OutputStream fos = null;
    try {
      fos = new BufferedOutputStream(new FileOutputStream(file));
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
      bitmap.recycle();

      file.setReadable(true, false);
    } finally {
      if (fos != null) {
        fos.close();
      }
    }
  }

  private static File getOrCreateDir(File parent, String dirName) throws IllegalAccessException {
    File dir = new File(parent, dirName);
    if (!dir.exists() && !dir.mkdir()) {
      throw new IllegalAccessException("Unable to create screenshot dir!");
    }
    dir.setReadable(true, false);
    dir.setWritable(true, false);
    dir.setExecutable(true, false);

    return dir;
  }
}
