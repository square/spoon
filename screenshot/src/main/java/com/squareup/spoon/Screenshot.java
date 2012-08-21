package com.squareup.spoon;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static android.content.Context.MODE_WORLD_READABLE;

/** Utility class for capturing screenshots for Spoon. */
public class Screenshot {

  static final String SPOON_SCREENSHOTS = "spoon-screenshots";
  private static final String TAGLESS_PREFIX = "image";
  private static final int QUALITY = 100;
  private static final String EXTENSION = ".png";

  /** Whether or not the screenshot output directory needs cleared. */
  private static boolean outputNeedsClear = true;

  /**
   * Take a screenshot.
   *
   * @param activity Activity with which to capture a screenshot.
   */
  public static void snap(Activity activity) {
    try {
      File screenshotDirectory = obtainScreenshotDirectory(activity);

      int number = 1;
      File screenshot;
      do {
        screenshot = new File(screenshotDirectory, TAGLESS_PREFIX + Integer.toString(number++) + EXTENSION);
      } while (screenshot.exists());

      takeScreenshot(screenshot, activity);
    } catch (Exception e) {
      throw new RuntimeException("Unable to take screenshot.", e);
    }
  }

  /**
   * Take a screenshot with the specified tag.
   *
   * @param activity Activity with which to capture a screenshot.
   * @param tag Unique tag to further identify the screenshot.
   */
  public static void snap(Activity activity, String tag) {
    try {
      File screenshotDirectory = obtainScreenshotDirectory(activity);
      takeScreenshot(new File(screenshotDirectory, tag + EXTENSION), activity);
    } catch (Exception e) {
      throw new RuntimeException("Unable to take screenshot.", e);
    }
  }

  private static void takeScreenshot(File file, Activity activity) throws IOException {
    View rootView = activity.getWindow().getDecorView();
    rootView.destroyDrawingCache();
    rootView.setDrawingCacheEnabled(true);
    Bitmap bitmap = Bitmap.createBitmap(rootView.getDrawingCache());
    rootView.setDrawingCacheEnabled(false);

    OutputStream fos = null;
    try {
      fos = new BufferedOutputStream(new FileOutputStream(file));
      bitmap.compress(Bitmap.CompressFormat.PNG, QUALITY, fos);
      bitmap.recycle();

      file.setReadable(true, false);
    } finally {
      if (fos != null) {
        fos.close();
      }
    }
  }

  private static File obtainScreenshotDirectory(Context context) throws IllegalAccessException {
    File screenshotsDir = context.getDir(SPOON_SCREENSHOTS, MODE_WORLD_READABLE);

    if (outputNeedsClear) {
      deletePath(screenshotsDir, false);
      outputNeedsClear = false;
    }

    // The call to this method and one of the snap methods will be the first two on the stack.
    StackTraceElement element = new Throwable().getStackTrace()[2];

    File dirClass = new File(screenshotsDir, element.getClassName());
    File dirMethod = new File(dirClass, element.getMethodName());
    createDir(dirMethod);
    return dirMethod;
  }

  private static void createDir(File dir) throws IllegalAccessException {
    File parent = dir.getParentFile();
    if (!parent.exists()) {
      createDir(parent);
    }
    if (!dir.exists() && !dir.mkdirs()) {
      throw new IllegalAccessException("Unable to create output directory: " + dir.getAbsolutePath());
    }
    dir.setReadable(true, false);
    dir.setWritable(true, false);
    dir.setExecutable(true, false);
  }

  private static void deletePath(File path, boolean deletePath) {
    if (path.isDirectory()) {
      for (File child : path.listFiles()) {
        deletePath(child, true);
      }
    }
    if (deletePath) {
      path.delete();
    }
  }
}
