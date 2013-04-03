package com.squareup.spoon;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

import static android.graphics.Bitmap.CompressFormat.PNG;
import static android.graphics.Bitmap.Config.ARGB_8888;
import static com.squareup.spoon.Chmod.chmodPlusR;
import static com.squareup.spoon.Spoon.TAG;

class Screenshot {
  private static final InetSocketAddress REMOTE_SERVICE = new InetSocketAddress("localhost", 42108);

  static void capture(File file, Activity activity) throws IOException {
    try {
      // Try to capture the entire screen using a remote started service.
      captureNative(file);
    } catch (IOException ignored) {
      // If remote service capture failed, fall back to capturing the activity decor view.
      captureDecorView(file, activity);
    }
  }

  private static void captureNative(File file) throws IOException {
    // Path to the target screenshot in byte form.
    byte[] screenshotPath = file.getAbsolutePath().getBytes("UTF-8");

    // Connect to the remote server.
    Socket socket = new Socket();
    socket.connect(REMOTE_SERVICE, 1000/*ms*/);

    OutputStream os = new BufferedOutputStream(socket.getOutputStream());
    InputStream is = new BufferedInputStream(socket.getInputStream());

    // Send the path and a null termination character.
    os.write(screenshotPath);
    os.write('\0');
    os.flush();

    // Consume exception message, if any.
    ByteArrayOutputStream exception = null;
    byte[] buffer = new byte[4096];
    int count = 0;
    while ((count = is.read(buffer)) != -1) {
      if (exception == null) {
        exception = new ByteArrayOutputStream();
      }
      exception.write(buffer, 0, count);
    }
    if (exception != null) {
      throw new IOException(new String(exception.toByteArray(), "UTF-8"));
    }
  }

  private static void captureDecorView(File file, final Activity activity) throws IOException {
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
        String msg = "Unable to get screenshot " + file.getAbsolutePath();
        Log.e(TAG, msg, e);
        throw new RuntimeException(msg, e);
      }
    }

    writeBitmapToFile(bitmap, file);
  }

  private static void writeBitmapToFile(Bitmap bitmap, File file) throws IOException {
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
}
