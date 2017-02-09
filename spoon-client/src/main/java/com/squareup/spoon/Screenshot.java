package com.squareup.spoon;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.View;
import java.util.concurrent.CountDownLatch;

import static android.graphics.Bitmap.Config.ARGB_8888;

abstract class Screenshot {
  static Bitmap capture(String tag, Activity activity) {
    return drawCanvas(tag, activity);
  }

  private static Bitmap drawCanvas(String tag, final Activity activity) {
    View view = activity.getWindow().getDecorView();
    if (view.getWidth() == 0 || view.getHeight() == 0) {
      throw new IllegalStateException("Your view has no height or width. Are you sure "
          + activity.getClass().getSimpleName()
          + " is the currently displayed activity?");
    }
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
        throw new RuntimeException("Unable to get screenshot '" + tag + "'", e);
      }
    }
    return bitmap;
  }

  private static void drawDecorViewToBitmap(Activity activity, Bitmap bitmap) {
    Canvas canvas = new Canvas(bitmap);
    activity.getWindow().getDecorView().draw(canvas);
  }
}
