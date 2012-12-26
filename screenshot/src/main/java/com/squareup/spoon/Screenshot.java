package com.squareup.spoon;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;

import static android.content.Context.MODE_WORLD_READABLE;
import static android.graphics.Bitmap.CompressFormat.PNG;
import static android.graphics.Bitmap.Config.ARGB_8888;

/** Utility class for capturing screenshots for Spoon. */
public final class Screenshot {
  static final String SPOON_SCREENSHOTS = "spoon-screenshots";
  static final String TEST_CASE_CLASS = "android.test.InstrumentationTestCase";
  static final String TEST_CASE_METHOD = "runMethod";
  private static final String TAG = "SpoonScreenshot";
  private static final String EXTENSION = ".png";
  private static final Object LOCK = new Object();

  /** Whether or not the screenshot output directory needs cleared. */
  private static boolean outputNeedsClear = true;

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
      throw new RuntimeException("Unable to capture screenshot.", e);
    }
  }

  private static void takeScreenshot(File file, final Activity activity) throws IOException {
    DisplayMetrics dm = activity.getResources().getDisplayMetrics();
    final Bitmap bitmap = Bitmap.createBitmap(dm.widthPixels, dm.heightPixels, ARGB_8888);

    if (Looper.myLooper() == Looper.getMainLooper()) {
      // On main thread already, Just Do It™.
      drawDecorViewToBitmap(activity, bitmap);
    } else {
      // On a background thread, post to main.
      final CountDownLatch latch = new CountDownLatch(1);
      activity.runOnUiThread(new Runnable() {
        @Override public void run() {
          try {
            drawDecorViewToBitmap(activity, bitmap);
          } finally {
            latch.countDown();
          }
        }
      });
      try {
        latch.await();
      } catch (InterruptedException e) {
        Log.e(TAG, "Unable to get screenshot " + file.getAbsolutePath(), e);
        return;
      }
    }

    OutputStream fos = null;
    try {
      fos = new BufferedOutputStream(new FileOutputStream(file));
      bitmap.compress(PNG, 100 /* quality */, fos);
      bitmap.recycle();

      file.setReadable(true, false);
    } finally {
      if (fos != null) {
        fos.close();
      }
    }
  }

  private static void drawDecorViewToBitmap(Activity activity, Bitmap bitmap) {
    Canvas canvas = new Canvas(bitmap);
    activity.getWindow().getDecorView().draw(canvas);
  }

  private static File obtainScreenshotDirectory(Context context) throws IllegalAccessException {
    File screenshotsDir = context.getDir(SPOON_SCREENSHOTS, MODE_WORLD_READABLE);

    synchronized (LOCK) {
      if (outputNeedsClear) {
        deletePath(screenshotsDir, false);
        outputNeedsClear = false;
      }
    }

    StackTraceElement testClass = findTestClassTraceElement(Thread.currentThread().getStackTrace());
    String className = testClass.getClassName().replaceAll("[^A-Za-z0-9._-]", "_");
    File dirClass = new File(screenshotsDir, className);
    File dirMethod = new File(dirClass, testClass.getMethodName());
    createDir(dirMethod);
    return dirMethod;
  }

  /** Returns the test class element by looking at the method InstrumentationTestCase invokes. */
  static StackTraceElement findTestClassTraceElement(StackTraceElement[] trace) {
    for (int i = trace.length - 1; i >= 0; i--) {
      StackTraceElement element = trace[i];
      if (TEST_CASE_CLASS.equals(element.getClassName())
         && TEST_CASE_METHOD.equals(element.getMethodName())) {
        return trace[i - 3];
      }
    }

    throw new IllegalArgumentException("Could not find test class!");
  }

  private static void createDir(File dir) throws IllegalAccessException {
    File parent = dir.getParentFile();
    if (!parent.exists()) {
      createDir(parent);
    }
    if (!dir.exists() && !dir.mkdirs()) {
      throw new IllegalAccessException("Unable to create output dir: " + dir.getAbsolutePath());
    }
    dir.setReadable(true, false);
    dir.setWritable(true, false);
    dir.setExecutable(true, false);
  }

  private static void deletePath(File path, boolean inclusive) {
    if (path.isDirectory()) {
      File[] children = path.listFiles();
      if (children != null) {
        for (File child : children) {
          deletePath(child, true);
        }
      }
    }
    if (inclusive) {
      path.delete();
    }
  }

  private Screenshot() {
    // No instances.
  }
}
