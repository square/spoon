package com.squareup.spoon;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.Log;
import com.android.ddmlib.Log.LogLevel;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.SyncException;
import com.android.ddmlib.TimeoutException;
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.squareup.spoon.external.AXMLParser;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import static com.android.ddmlib.FileListingService.FileEntry;
import static com.android.ddmlib.FileListingService.TYPE_DIRECTORY;
import static com.android.ddmlib.SyncService.ISyncProgressMonitor;
import static com.squareup.spoon.Screenshot.SPOON_SCREENSHOTS;

/** Represents a single device and the test configuration to be executed. */
public class ExecutionTarget implements Callable<ExecutionResult> {
  static final String FILE_RESULT = "result.json";
  static final String OUTPUT_FILE = "output.txt";

  private static final String ANDROID_MANIFEST_XML = "AndroidManifest.xml";
  private static final String TAG_MANIFEST = "manifest";
  private static final String TAG_INSTRUMENTATION = "instrumentation";
  private static final String ATTR_PACKAGE = "package";
  private static final String ATTR_TARGET_PACKAGE = "targetPackage";
  private static final String ATTR_NAME = "name";
  private static final String FILE_EXECUTION = "execution.json";

  private static final Gson GSON = new GsonBuilder() //
      .registerTypeAdapter(File.class, new TypeAdapter<File>() {
        @Override public void write(JsonWriter jsonWriter, File file) throws IOException {
          jsonWriter.value(file.getAbsolutePath());
        }

        @Override public File read(JsonReader jsonReader) throws IOException {
          return new File(jsonReader.nextString());
        }
      }).create();

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
  private final boolean debug;
  private final File output;

  /**
   * Create a test runner for a single device.
   *
   * @param sdkPath Path to the local Android SDK directory.
   * @param apk Path to application APK.
   * @param testApk Path to test application APK.
   * @param output Path to output directory.
   * @param serial Device to run the test on.
   * @param debug Whether or not debug logging is enabled.
   */
  public ExecutionTarget(String sdkPath, File apk, File testApk, File output, String serial,
      boolean debug) {
    this.sdkPath = sdkPath;
    this.apk = apk;
    this.testApk = testApk;
    this.serial = serial;
    this.debug = debug;
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
    String name = ExecutionTarget.class.getName();
    Process process = new ProcessBuilder("java", "-cp", classpath, name,
      output.getAbsolutePath()).start();
    process.waitFor();
    IOUtils.copy(process.getErrorStream(), System.out);

    return GSON.fromJson(new FileReader(new File(output, FILE_RESULT)), ExecutionResult.class);
  }

  /////////////////////////////////////////////////////////////////////////////
  ////  Secondary Per-Device Process  /////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////

  public static void main(String... args)
      throws IOException, ShellCommandUnresponsiveException, AdbCommandRejectedException,
      TimeoutException, SyncException {
    Logger log = Logger.getLogger(ExecutionTarget.class.getSimpleName());
    try {
      if (args.length != 1) {
        throw new IllegalArgumentException("Must be started with a device directory.");
      }

      String outputDirName = args[0];
      File outputDir = new File(outputDirName);
      File executionFile = new File(outputDir, FILE_EXECUTION);
      if (!executionFile.exists()) {
        throw new IllegalArgumentException("Device directory and/or execution file doesn't exist.");
      }

      ExecutionTarget target = GSON.fromJson(new FileReader(executionFile), ExecutionTarget.class);
      ExecutionResult result = new ExecutionResult(target.serial);

      FileHandler handler = new FileHandler(new File(outputDir, OUTPUT_FILE).getAbsolutePath());
      handler.setFormatter(new SimpleFormatter());
      log.addHandler(handler);
      log.setLevel(target.debug ? Level.FINE : Level.INFO);

      if (!target.apk.exists()) {
        throw new IllegalArgumentException(String.format("App APK %s does not exist.",
          target.apk.getAbsolutePath()));
      }
      if (!target.testApk.exists()) {
        throw new IllegalArgumentException(String.format("Test APK %s does not exist.",
          target.testApk.getAbsolutePath()));
      }

      String[] packages = getManifestInfo(target.testApk);
      final String appPackage = packages[0];
      final String testPackage = packages[1];
      final String testRunner = packages[2];

      log.fine(appPackage + " in " + target.apk.getAbsolutePath());
      log.fine(testPackage + " in " + target.testApk.getAbsolutePath());

      if (target.debug) {
        setInternalLoggingLevel();
      }

      AndroidDebugBridge adb = AdbHelper.init(target.sdkPath);

      IDevice realDevice = obtainRealDevice(adb, target.serial);
      result.configureFor(realDevice);

      // Install the main application and the testApk package.
      realDevice.installPackage(target.apk.getAbsolutePath(), true);
      realDevice.installPackage(target.testApk.getAbsolutePath(), true);

      // Run all the tests! o/
      result.testStart = System.nanoTime();
      new RemoteAndroidTestRunner(testPackage, testRunner, realDevice).run(result);
      result.testEnd = System.nanoTime();
      result.testCompleted = new Date();

      // Sync device screenshots, if any, to the local filesystem.
      String dirName = "app_" + SPOON_SCREENSHOTS;
      FileEntry deviceDir = obtainDirectoryFileEntry("/data/data/" + appPackage + "/" + dirName);
      realDevice.getSyncService().pull(new FileEntry[] {deviceDir}, outputDirName, QUIET_MONITOR);

      File screenshotDir = new File(outputDir, dirName);
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
    } catch (IllegalArgumentException ex) {
      // Arguments thrown by us, log them before dying.
      log.severe(ex.getMessage());
    } catch (Exception ex) {
      log.throwing(ExecutionTarget.class.getSimpleName(), "main", ex);
    } finally {
      try {
        AndroidDebugBridge.terminate();
      } catch (Exception ignore) {
      }
    }
  }

  private static void setInternalLoggingLevel() {
    try {
      Field level = Log.class.getDeclaredField("mLevel");
      level.setAccessible(true);
      level.set(Log.class, LogLevel.DEBUG);
    } catch (NoSuchFieldException ignored) {
    } catch (IllegalAccessException ignored) {
    }
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
      Constructor<FileEntry> c =
          FileEntry.class.getDeclaredConstructor(FileEntry.class, String.class, int.class,
              boolean.class);
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

  private static String[] getManifestInfo(File apkTestFile) {
    InputStream is = null;
    try {
      ZipFile zip = new ZipFile(apkTestFile);
      ZipEntry entry = zip.getEntry(ANDROID_MANIFEST_XML);
      is = zip.getInputStream(entry);

      AXMLParser parser = new AXMLParser(is);
      int eventType = parser.getType();

      String[] ret = new String[3];
      while (eventType != AXMLParser.END_DOCUMENT) {
        if (eventType == AXMLParser.START_TAG) {
          String parserName = parser.getName();
          boolean isManifest = TAG_MANIFEST.equals(parserName);
          boolean isInstrumentation = TAG_INSTRUMENTATION.equals(parserName);
          if (isManifest || isInstrumentation) {
            for (int i = 0; i < parser.getAttributeCount(); i++) {
              String parserAttributeName = parser.getAttributeName(i);
              if (isManifest && ATTR_PACKAGE.equals(parserAttributeName)) {
                ret[1] = parser.getAttributeValueString(i);
              } else if (isInstrumentation && ATTR_TARGET_PACKAGE.equals(parserAttributeName)) {
                ret[0] = parser.getAttributeValueString(i);
              } else if (isInstrumentation && ATTR_NAME.equals(parserAttributeName)) {
                ret[2] = parser.getAttributeValueString(i);
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
