package com.squareup.spoon;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Environment;
import android.view.View;

import java.io.*;

/**
 * TODO doc me
 */
  public class Screenshot {

//    private static Thread thread;

    /**
     * TODO doc me
     *
     * @param activity
     * @param tag
     */
    public static void snap(Activity activity, String tag) {
      try {
//        if (thread == null) {
//          thread = new Thread(new SocketListenerRunnable());
//          thread.run();
//        }

        File dlCache = Environment.getExternalStorageDirectory(); // TODO dimitris
        final File screenshotRoot = getOrCreateDir(dlCache, "spoon-screenshots");
        final StackTraceElement element = new Throwable().getStackTrace()[1];
        String testcaseName = element.getClassName();
        String testName = element.getMethodName();
        final File testcaseDir = getOrCreateDir(screenshotRoot, testcaseName);
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

      OutputStream fout;
      fout = new BufferedOutputStream(new FileOutputStream(file));
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, fout);
      fout.flush();
      fout.close();

      bitmap.recycle();

    }

  private static File getOrCreateDir(File parent, String dirName) throws IllegalAccessException {
    File screenshotDir = new File(parent, dirName);
    if (!screenshotDir.exists() && !screenshotDir.mkdir()) {
      throw new IllegalAccessException("Unable to create screenshot dir!");
    }
    return screenshotDir;
  }

//  private static class SocketListenerRunnable implements Runnable {
//
//    public static final int TEN_SECONDS = 10 * 1000;
//
//    @Override
//    public void run() {
//      try {
//        ServerSocket server = new ServerSocket(58008);
//        server.setSoTimeout(TEN_SECONDS);
//        final Socket client = server.accept();
//        final PrintWriter writer = new PrintWriter(client.getOutputStream(), true);
//        while()
//      } catch (IOException e) {
//        // TODO figure out what to do if we can't open the socket.
//      }
//    }
//  }
}
