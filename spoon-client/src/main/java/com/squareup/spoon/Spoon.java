package com.squareup.spoon;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;

import static android.content.Context.MODE_WORLD_READABLE;
import static android.graphics.Bitmap.CompressFormat.PNG;
import static android.graphics.Bitmap.Config.ARGB_8888;
import static android.os.Environment.getExternalStorageDirectory;
import static com.squareup.spoon.Chmod.chmodPlusR;
import static com.squareup.spoon.Chmod.chmodPlusRWX;

/** Utility class for capturing screenshots for Spoon. */
public final class Spoon {
  static final String SPOON_SCREENSHOTS = "spoon-screenshots";
  static final String NAME_SEPARATOR = "_";
  static final String TEST_CASE_CLASS_JUNIT_3 = "android.test.InstrumentationTestCase";
  static final String TEST_CASE_METHOD_JUNIT_3 = "runMethod";
  static final String TEST_CASE_CLASS_JUNIT_4 = "org.junit.runners.model.FrameworkMethod$1";
  static final String TEST_CASE_METHOD_JUNIT_4 = "runReflectiveCall";
  private static final String EXTENSION = ".png";
  private static final String TAG = "Spoon";
  private static final Object LOCK = new Object();
  private static final Pattern TAG_VALIDATION = Pattern.compile("[a-zA-Z0-9_-]+");

  /** Whether or not the screenshot output directory needs cleared. */
  private static boolean outputNeedsClear = true;

  /**
   * Take a viewshot with the specified tag.
   * This uses the root view of the passed in view to capture an image, so to capture the image
   * of a dialog, pass it any of the child views in the dialog.
   *
   * @param view View for which to capture a shot.
   * @param tag Unique tag to further identify the screenshot. Must match [a-zA-Z0-9_-]+.
   * @return the image file that was created
   */
  public static File viewshot(View view, String tag) {
    StackTraceElement testClass = findTestClassTraceElement(Thread.currentThread().getStackTrace());
    String className = testClass.getClassName().replaceAll("[^A-Za-z0-9._-]", "_");
    String methodName = testClass.getMethodName();
    return viewshot(view, tag, className, methodName);
  }

  /**
   * Take a viewshot with the specified tag.  This version allows the caller to manually specify
   * the test class name and method name.  This is necessary when the screenshot is not called in
   * the traditional manner.
   *
   * @param view View for which to capture a shot.
   * @param tag Unique tag to further identify the screenshot. Must match [a-zA-Z0-9_-]+.
   * @return the image file that was created
   */
  public static File viewshot(View view, String tag, String testClassName,
      String testMethodName) {
    if (!TAG_VALIDATION.matcher(tag).matches()) {
      throw new IllegalArgumentException("Tag must match " + TAG_VALIDATION.pattern() + ".");
    }
    try {
      File screenshotDirectory =
          obtainScreenshotDirectory(view.getContext().getApplicationContext(), testClassName,
              testMethodName);
      String screenshotName = System.currentTimeMillis() + NAME_SEPARATOR + tag + EXTENSION;
      File screenshotFile = new File(screenshotDirectory, screenshotName);
      takeViewshot(screenshotFile, view);
      Log.d(TAG, "Captured screenshot '" + tag + "'.");
      return screenshotFile;
    } catch (Exception e) {
      throw new RuntimeException("Unable to capture screenshot.", e);
    }
  }

  private static void takeViewshot(File file, final View view) throws IOException {
    View rootView = view.getRootView();
    final Bitmap bitmap = Bitmap.createBitmap(rootView.getWidth(), rootView.getHeight(), ARGB_8888);

    if (Looper.myLooper() == Looper.getMainLooper()) {
      // On main thread already, Just Do It™.
      drawViewToBitmap(view, bitmap);
    } else {
      // On a background thread, post to main.
      final CountDownLatch latch = new CountDownLatch(1);
      view.post(new Runnable() {
        @Override public void run() {
          try {
            drawViewToBitmap(view, bitmap);
          } finally {
            latch.countDown();
          }
        }
      });
      try {
        latch.await();
      } catch (InterruptedException e) {
        String msg = "Unable to get screenshot " + file.getAbsolutePath();
        Log.e(TAG, msg, e);
        throw new RuntimeException(msg, e);
      }
    }

    OutputStream fos = null;
    try {
      fos = new BufferedOutputStream(new FileOutputStream(file));
      bitmap.compress(PNG, 100 /* quality */, fos);

      chmodPlusR(file);
    } finally {
      bitmap.recycle();
      if (fos != null) {
        fos.close();
      }
    }
  }

  private static void drawViewToBitmap(View view, Bitmap bitmap) {
    Canvas canvas = new Canvas(bitmap);
    view.getRootView().draw(canvas);
  }

  /**
   * Take a screenshot with the specified tag.
   *
   * @param activity Activity with which to capture a screenshot.
   * @param tag Unique tag to further identify the screenshot. Must match [a-zA-Z0-9_-]+.
   * @return the image file that was created
   */
  public static File screenshot(Activity activity, String tag) {
    StackTraceElement testClass = findTestClassTraceElement(Thread.currentThread().getStackTrace());
    String className = testClass.getClassName().replaceAll("[^A-Za-z0-9._-]", "_");
    String methodName = testClass.getMethodName();
    return screenshot(activity, tag, className, methodName);
  }

  /**
   * Take a screenshot with the specified tag.  This version allows the caller to manually specify
   * the test class name and method name.  This is necessary when the screenshot is not called in
   * the traditional manner.
   *
   * @param activity Activity with which to capture a screenshot.
   * @param tag Unique tag to further identify the screenshot. Must match [a-zA-Z0-9_-]+.
   * @return the image file that was created
   */
  public static File screenshot(Activity activity, String tag, String testClassName,
      String testMethodName) {
    if (!TAG_VALIDATION.matcher(tag).matches()) {
      throw new IllegalArgumentException("Tag must match " + TAG_VALIDATION.pattern() + ".");
    }
    try {
      File screenshotDirectory =
          obtainScreenshotDirectory(activity.getApplicationContext(), testClassName,
              testMethodName);
      String screenshotName = System.currentTimeMillis() + NAME_SEPARATOR + tag + EXTENSION;
      File screenshotFile = new File(screenshotDirectory, screenshotName);
      takeScreenshot(screenshotFile, activity);
      Log.d(TAG, "Captured screenshot '" + tag + "'.");
      return screenshotFile;
    } catch (Exception e) {
      throw new RuntimeException("Unable to capture screenshot.", e);
    }
  }

  private static void takeScreenshot(File file, final Activity activity) throws IOException {
    View view = activity.getWindow().getDecorView();
    final Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), ARGB_8888);

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
        String msg = "Unable to get screenshot " + file.getAbsolutePath();
        Log.e(TAG, msg, e);
        throw new RuntimeException(msg, e);
      }
    }

    OutputStream fos = null;
    try {
      fos = new BufferedOutputStream(new FileOutputStream(file));
      bitmap.compress(PNG, 100 /* quality */, fos);

      chmodPlusR(file);
    } finally {
      bitmap.recycle();
      if (fos != null) {
        fos.close();
      }
    }
  }

  private static void drawDecorViewToBitmap(Activity activity, Bitmap bitmap) {
    Canvas canvas = new Canvas(bitmap);
    activity.getWindow().getDecorView().draw(canvas);
  }

  private static File obtainScreenshotDirectory(Context context, String testClassName,
      String testMethodName) throws IllegalAccessException {
    File screenshotsDir;
    if (Build.VERSION.SDK_INT >= 21) {
      // Use external storage.
      screenshotsDir = new File(getExternalStorageDirectory(), "app_" + SPOON_SCREENSHOTS);
    } else {
      // Use internal storage.
      screenshotsDir = context.getDir(SPOON_SCREENSHOTS, MODE_WORLD_READABLE);
    }

    synchronized (LOCK) {
      if (outputNeedsClear) {
        deletePath(screenshotsDir, false);
        outputNeedsClear = false;
      }
    }

    File dirClass = new File(screenshotsDir, testClassName);
    File dirMethod = new File(dirClass, testMethodName);
    createDir(dirMethod);
    return dirMethod;
  }

  /** Returns the test class element by looking at the method InstrumentationTestCase invokes. */
  static StackTraceElement findTestClassTraceElement(StackTraceElement[] trace) {
    for (int i = trace.length - 1; i >= 0; i--) {
      StackTraceElement element = trace[i];

      if (TEST_CASE_CLASS_JUNIT_3.equals(element.getClassName()) //
          && TEST_CASE_METHOD_JUNIT_3.equals(element.getMethodName())) {
        return trace[i - 3];
      }

      if (TEST_CASE_CLASS_JUNIT_4.equals(element.getClassName()) //
          && TEST_CASE_METHOD_JUNIT_4.equals(element.getMethodName())) {
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
    chmodPlusRWX(dir);
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

  private Spoon() {
    // No instances.
  }
}
