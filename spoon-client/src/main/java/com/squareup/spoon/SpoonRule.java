package com.squareup.spoon;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static android.content.Context.MODE_WORLD_READABLE;
import static android.graphics.Bitmap.CompressFormat.PNG;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static android.os.Environment.getExternalStorageDirectory;
import static com.squareup.spoon.Chmod.chmodPlusR;
import static com.squareup.spoon.Chmod.chmodPlusRWX;
import static com.squareup.spoon.internal.Constants.NAME_SEPARATOR;
import static com.squareup.spoon.internal.Constants.SPOON_SCREENSHOTS;

/**
 * A test rule which captures screenshots and associates them with the test class and test method
 * for Spoon.
 *
 * <pre><code>
 * &#064;Rule public final SpoonRule spoon = new SpoonRule();
 * &#064;Rule public final ActivityTestRule&lt;MyActivity> activityRule = // ...
 *
 * &#064;Test public void methodUnderTest() {
 *   MyActivity activity = activityRule.getActivity();
 *   spoon.screenshot(activity, "start");
 *   // ...
 * }
 * </code></pre>
 */
public final class SpoonRule implements TestRule {
  private static final String EXTENSION = ".png";
  private static final String TAG = "Spoon";
  private static final Pattern TAG_VALIDATION = Pattern.compile("[a-zA-Z0-9_-]+");
  private static final Object LOCK = new Object();

  /** Holds a set of directories that have been cleared for this test */
  private static final Set<String> clearedOutputDirectories = new LinkedHashSet<>();

  private String className;
  private String methodName;

  @Override public Statement apply(Statement base, Description description) {
    className = description.getClassName();
    methodName = description.getMethodName();
    return base; // Pass-through. We're just here to capture the description information.
  }

  public File screenshot(Activity activity, String tag) {
    if (!TAG_VALIDATION.matcher(tag).matches()) {
      throw new IllegalArgumentException("Tag must match " + TAG_VALIDATION.pattern() + ".");
    }
    File screenshotDirectory =
        obtainScreenshotDirectory(activity.getApplicationContext(), className, methodName);
    String screenshotName = System.currentTimeMillis() + NAME_SEPARATOR + tag + EXTENSION;
    File screenshotFile = new File(screenshotDirectory, screenshotName);
    Bitmap bitmap = Screenshot.capture(tag, activity);
    writeBitmapToFile(bitmap, screenshotFile);
    Log.d(TAG, "Captured screenshot '" + tag + "'.");
    return screenshotFile;
  }

  private static void writeBitmapToFile(Bitmap bitmap, File file) {
    OutputStream fos = null;
    try {
      fos = new BufferedOutputStream(new FileOutputStream(file));
      bitmap.compress(PNG, 100 /* quality */, fos);

      chmodPlusR(file);
    } catch (FileNotFoundException e) {
      throw new RuntimeException("Cannot write screenshot to " + file, e);
    } finally {
      bitmap.recycle();
      if (fos != null) {
        try {
          fos.close();
        } catch (IOException ignored) {
        }
      }
    }
  }

  private static File obtainScreenshotDirectory(Context context, String testClassName,
      String testMethodName) {
    File directory;
    if (SDK_INT >= LOLLIPOP) {
      // Use external storage.
      directory = new File(getExternalStorageDirectory(), "app_" + SPOON_SCREENSHOTS);
    } else {
      // Use internal storage.
      directory = context.getDir(SPOON_SCREENSHOTS, MODE_WORLD_READABLE);
    }

    synchronized (LOCK) {
      if (clearedOutputDirectories.add(SPOON_SCREENSHOTS)) {
        deletePath(directory, false);
      }
    }

    File dirClass = new File(directory, testClassName);
    File dirMethod = new File(dirClass, testMethodName);
    createDir(dirMethod);
    return dirMethod;
  }

  private static void createDir(File dir) {
    File parent = dir.getParentFile();
    if (!parent.exists()) {
      createDir(parent);
    }
    if (!dir.exists() && !dir.mkdirs()) {
      throw new RuntimeException("Unable to create output dir: " + dir.getAbsolutePath());
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
}
