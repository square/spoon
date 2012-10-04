package com.squareup.spoon;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.SyncException;
import com.android.ddmlib.TimeoutException;
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner;
import com.google.gson.Gson;
import com.squareup.spoon.external.AXMLParser;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.android.ddmlib.FileListingService.FileEntry;
import static com.android.ddmlib.FileListingService.TYPE_DIRECTORY;
import static com.android.ddmlib.SyncService.ISyncProgressMonitor;
import static com.squareup.spoon.Screenshot.SPOON_SCREENSHOTS;

/** Represents a single device and the test configuration to be executed. */
public class ExecutionTarget implements Callable<ExecutionResult> {
  private static final String ANDROID_MANIFEST_XML = "AndroidManifest.xml";
  private static final String TAG_MANIFEST = "manifest";
  private static final String TAG_INSTRUMENTATION = "instrumentation";
  private static final String ATTR_PACKAGE = "package";
  private static final String ATTR_TARGET_PACKAGE = "targetPackage";
  private static final String FILE_EXECUTION = "execution.json";
  private static final String FILE_RESULT = "result.json";
  private static final Gson GSON = new Gson();

  private static final ISyncProgressMonitor QUIET_MONITOR = new ISyncProgressMonitor() {
    @Override public void start(int totalWork) {
    }

    @Override public void stop() {
    }

    @Override public boolean isCanceled() {
      return false;
    }

    @Override public void startSubTask(String name) {
    }

    @Override public void advance(int work) {
    }
  };

  private final String sdkPath;
  private final File apk;
  private final File testApk;
  private final String serial;
  private final File output;

  /**
   * Create a test runner for a single device.
   *
   * @param sdkPath Path to the local Android SDK directory.
   * @param apk Path to application APK.
   * @param testApk Path to test application APK.
   * @param output Path to output directory.
   * @param serial Device to run the test on.
   */
  public ExecutionTarget(String sdkPath, File apk, File testApk, File output, String serial) {
    this.sdkPath = sdkPath;
    this.apk = apk;
    this.testApk = testApk;
    this.serial = serial;
    this.output = new File(output, serial);
  }


  /////////////////////////////////////////////////////////////////////////////
  ////  Main ExecutionSuite Process  //////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////

  /** Serialize ourself to disk and start {@link #main(String...)} in another process. */
  @Override public ExecutionResult call() throws IOException, InterruptedException {
    // Create the output directory.
    output.mkdirs();

    // Write our configuration to a file in the output directory.
    FileWriter execution = new FileWriter(new File(output, FILE_EXECUTION));
    GSON.toJson(this, execution);
    execution.close();

    // Kick off a new process to interface with ADB and perform the real execution.
    String classpath = System.getProperty("java.class.path");
    new ProcessBuilder("java", "-cp", classpath, ExecutionTarget.class.getName(), output.getAbsolutePath())
        .start()
        .waitFor();

    return GSON.fromJson(new FileReader(new File(output, FILE_RESULT)), ExecutionResult.class);
  }


  /////////////////////////////////////////////////////////////////////////////
  ////  Secondary Per-Device Process  /////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////

  public static void main(String... args) throws IOException, ShellCommandUnresponsiveException, AdbCommandRejectedException, TimeoutException, SyncException {
    if (args.length != 1) {
      throw new IllegalArgumentException("Must be started with a device directory.");
    }

    String outputDirName = args[0];
    File outputDir = new File(outputDirName);
    File executionFile = new File(outputDir, FILE_EXECUTION);
    if (!executionFile.exists()) {
      throw new IllegalArgumentException("Device directory and/or execution file does not exist.");
    }

    Logger log = Logger.getLogger(ExecutionTarget.class.getSimpleName());
    FileHandler handler = new FileHandler(new File(outputDir, "output.txt").getAbsolutePath());
    handler.setFormatter(new SimpleFormatter());
    log.addHandler(handler);

    ExecutionTarget target = GSON.fromJson(new FileReader(executionFile), ExecutionTarget.class);
    ExecutionResult result = new ExecutionResult(target.serial);

    String[] packages = getManifestPackages(target.testApk);
    final String appPackage = packages[0];
    final String testPackage = packages[1];

    log.fine(appPackage + " in " + target.apk.getAbsolutePath());
    log.fine(testPackage + " in " + target.testApk.getAbsolutePath());

    IDevice realDevice = null;
    try {
      AndroidDebugBridge adb = AdbHelper.init(target.sdkPath);

      realDevice = obtainRealDevice(adb, target.serial);
      result.configureFor(realDevice);

      // Install the main application and the testApk package.
      realDevice.installPackage(target.apk.getAbsolutePath(), true);
      realDevice.installPackage(target.testApk.getAbsolutePath(), true);

      // Run all the tests! o/
      result.testStart = System.currentTimeMillis();
      new RemoteAndroidTestRunner(testPackage, realDevice).run(result);
      result.testEnd = System.currentTimeMillis();
      log.info("");

    } catch (Exception e) {
      log.throwing(ExecutionTarget.class.getSimpleName(), "main", e);
      // TODO record exception
    } finally {
      try {
        AndroidDebugBridge.terminate();
      } catch (Exception ignore) {
      }
    }

    // Sync device screenshots, if any, to the local filesystem.
    String screenshotDirName = "app_" + SPOON_SCREENSHOTS;
    FileEntry deviceDir = obtainDirectoryFileEntry("/data/data/" + appPackage + "/" + screenshotDirName);
    realDevice.getSyncService().pull(new FileEntry[] { deviceDir }, outputDirName, QUIET_MONITOR);

    File screenshotDir = new File(outputDir, screenshotDirName);
    if (screenshotDir.exists()) {
      // Move all children of the screenshot directory into the output folder.
      File[] classNameDirs = screenshotDir.listFiles();
      if (classNameDirs != null) {
        for (File classNameDir : classNameDirs) {
          File destDir = new File(outputDir, classNameDir.getName());
          FileUtils.deleteDirectory(destDir);
          FileUtils.moveDirectory(classNameDir, destDir);
          result.addScreenshotDirectory(destDir);
        }
      }
      FileUtils.deleteDirectory(screenshotDir);
    }

    // Write device result file.
    FileWriter writer = new FileWriter(new File(outputDir, FILE_RESULT));
    GSON.toJson(result, writer);
    writer.close();
  }

  /** Fetch or create a real device that corresponds to a device model. */
  private static IDevice obtainRealDevice(AndroidDebugBridge adb, String serial) {
    // Get an existing real device.
    for (IDevice adbDevice : adb.getDevices()) {
      if (adbDevice.getSerialNumber().equals(serial)) {
        return adbDevice;
      }
    }
    throw new IllegalArgumentException("Unknown device serial: " + serial);
  }

  private static FileEntry obtainDirectoryFileEntry(String path) {
    try {
      FileEntry lastEntry = null;
      Constructor<FileEntry> c = FileEntry.class.getDeclaredConstructor(FileEntry.class, String.class, int.class, boolean.class);
      c.setAccessible(true);
      for (String part : path.split("/")) {
        lastEntry = c.newInstance(lastEntry, part, TYPE_DIRECTORY, lastEntry == null);
      }
      return lastEntry;
    } catch (NoSuchMethodException ignored) {
    } catch (InvocationTargetException ignored) {
    } catch (InstantiationException ignored) {
    } catch (IllegalAccessException ignored) {
    }
    return null;
  }

  private static String[] getManifestPackages(File apkTestFile) {
    InputStream is = null;
    try {
      ZipFile zip = new ZipFile(apkTestFile);
      ZipEntry entry = zip.getEntry(ANDROID_MANIFEST_XML);
      is = zip.getInputStream(entry);

      AXMLParser parser = new AXMLParser(is);
      int eventType = parser.getType();

      String[] ret = new String[2];
      while (eventType != AXMLParser.END_DOCUMENT) {
        if (eventType == AXMLParser.START_TAG) {
          String parserName = parser.getName();
          if (TAG_MANIFEST.equals(parserName) || TAG_INSTRUMENTATION.equals(parserName)) {
            for (int i = 0; i < parser.getAttributeCount(); i++) {
              String parserAttributeName = parser.getAttributeName(i);
              if (ATTR_PACKAGE.equals(parserAttributeName)) {
                ret[1] = parser.getAttributeValueString(i);
                break;
              }
              if (ATTR_TARGET_PACKAGE.equals(parserAttributeName)) {
                ret[0] = parser.getAttributeValueString(i);
                break;
              }
            }
          }
        }
        eventType = parser.next();
      }
      if (ret[0] == null || ret[1] == null) {
        throw new IllegalStateException("Unable to find both app and test package.");
      }
      return ret;
    } catch (IOException e) {
      throw new RuntimeException("Unable to parse test app AndroidManifest.xml.", e);
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          // Ignored.
        }
      }
    }
  }
}
