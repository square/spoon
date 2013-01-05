package com.squareup.spoon;

import com.android.ddmlib.AndroidDebugBridge;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.squareup.spoon.DeviceTestResult.Status;
import static com.squareup.spoon.SpoonInstrumentationInfo.parseFromFile;
import static com.squareup.spoon.SpoonUtils.getConfiguredLogger;
import static java.util.Collections.unmodifiableSet;

/** Represents a collection of devices and the test configuration to be executed. */
public final class Spoon {
  private static final String DEFAULT_TITLE = "Spoon Execution";
  static final String DEFAULT_OUTPUT_DIRECTORY = "spoon-output";

  private final String title;
  private final File androidSdk;
  private final File applicationApk;
  private final File instrumentationApk;
  private final File output;
  private final boolean debug;
  private final Set<String> serials;
  private final String classpath;
  private final Logger log;

  private Spoon(String title, File androidSdk, File applicationApk, File instrumentationApk,
      File output, boolean debug, Set<String> serials, String classpath) {
    this.title = title;
    this.androidSdk = androidSdk;
    this.applicationApk = applicationApk;
    this.instrumentationApk = instrumentationApk;
    this.output = output;
    this.debug = debug;
    this.serials = unmodifiableSet(serials);
    this.classpath = classpath;
    this.log = getConfiguredLogger(this, debug);
  }

  /**
   * Install and execute the tests on all specified devices.
   *
   * @return {@code true} if there were no test failures or exceptions thrown.
   */
  public boolean run() {
    checkArgument(applicationApk.exists(), "Could not find application APK.");
    checkArgument(instrumentationApk.exists(), "Could not find instrumentation APK.");

    AndroidDebugBridge adb = SpoonUtils.initAdb(androidSdk);

    try {
      // If we were given an empty serial set, load all available devices.
      Set<String> serials = this.serials;
      if (serials.isEmpty()) {
        serials = SpoonUtils.findAllDevices(adb);
      }

      // Execute all the things...
      SpoonSummary summary = runTests(adb, serials);
      // ...and render to HTML
      new SpoonRenderer(summary, output).render();

      return parseOverallSuccess(summary);
    } finally {
      AndroidDebugBridge.terminate();
    }
  }

  private SpoonSummary runTests(AndroidDebugBridge adb, Set<String> serials) {
    int targetCount = serials.size();
    log.info("Executing instrumentation on " + targetCount + " devices.");

    try {
      FileUtils.deleteDirectory(output);
    } catch (IOException e) {
      throw new RuntimeException("Unable to clean output directory: " + output, e);
    }

    final SpoonInstrumentationInfo testInfo = parseFromFile(instrumentationApk);
    log.fine(testInfo.getApplicationPackage() + " in " + applicationApk.getAbsolutePath());
    log.fine(testInfo.getInstrumentationPackage() + " in " + instrumentationApk.getAbsolutePath());

    final SpoonSummary.Builder summary = new SpoonSummary.Builder()
        .setTitle(title)
        .start();

    if (targetCount == 1) {
      // Since there is only one device just execute it synchronously in this process.
      String serial = serials.iterator().next();
      try {
        summary.addResult(serial, getTestRunner(serial, testInfo).run(adb));
      } catch (Exception e) {
        summary.addResult(serial, new DeviceResult.Builder().addException(e).build());
      }
    } else {
      // Spawn a new thread for each device and wait for them all to finish.
      final CountDownLatch done = new CountDownLatch(targetCount);
      for (final String serial : serials) {
        new Thread(new Runnable() {
          @Override public void run() {
            try {
              summary.addResult(serial, getTestRunner(serial, testInfo).runInNewProcess());
            } catch (Exception e) {
              summary.addResult(serial, new DeviceResult.Builder().addException(e).build());
            } finally {
              done.countDown();
            }
          }
        }).start();
      }

      try {
        done.await();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    return summary.end().build();
  }

  /** Returns {@code false} if a test failed on any device. */
  private boolean parseOverallSuccess(SpoonSummary summary) {
    for (DeviceResult result : summary.getResults().values()) {
      if (result.getInstallFailed()) {
        return false;
      }
      for (DeviceTestResult methodResult : result.getTestResults()) {
        if (methodResult.getStatus() != Status.PASS) {
          return false;
        }
      }
    }
    return true;
  }

  private SpoonDeviceRunner getTestRunner(String serial, SpoonInstrumentationInfo testInfo) {
    return new SpoonDeviceRunner(androidSdk, applicationApk, instrumentationApk, output, serial,
        debug, classpath, testInfo);
  }

  /** Build a test suite for the specified devices and configuration. */
  public static class Builder {
    private String title = DEFAULT_TITLE;
    private File androidSdk;
    private File applicationApk;
    private File instrumentationApk;
    private File output;
    private boolean debug = false;
    private Set<String> serials;
    private String classpath = System.getProperty("java.class.path");

    /** Identifying title for this execution. */
    public Builder setTitle(String title) {
      checkNotNull(title);
      this.title = title;
      return this;
    }

    /** Path to the local Android SDK directory. */
    public Builder setAndroidSdk(File androidSdk) {
      checkNotNull(androidSdk);
      this.androidSdk = androidSdk;
      return this;
    }

    /** Path to application APK. */
    public Builder setApplicationApk(File apk) {
      checkNotNull(apk);
      this.applicationApk = apk;
      return this;
    }

    /** Path to instrumentation APK. */
    public Builder setInstrumentationApk(File apk) {
      checkNotNull(apk);
      this.instrumentationApk = apk;
      return this;
    }

    /** Path to output directory. */
    public Builder setOutputDirectory(File output) {
      checkNotNull(output);
      this.output = output;
      return this;
    }

    /** Whether or not debug logging is enabled. */
    public Builder setDebug(boolean debug) {
      this.debug = debug;
      return this;
    }

    /** Add a device serial for test execution. */
    public Builder addDevice(String serial) {
      checkNotNull(serial);
      checkArgument(!serials.isEmpty(), "Already marked as using all devices.");
      if (serials == null) {
        serials = new HashSet<String>();
      }
      serials.add(serial);
      return this;
    }

    /** Use all currently attached device serials when executed. */
    public Builder useAllAttachedDevices() {
      if (this.serials != null) {
        throw new IllegalStateException("Serial list already contains entries.");
      }
      if (this.androidSdk == null) {
        throw new IllegalStateException("SDK must be set before calling this method.");
      }
      this.serials = Collections.emptySet();
      return this;
    }

    /** Classpath to use for new JVM processes. */
    public Builder setClasspath(String classpath) {
      checkNotNull(classpath);
      this.classpath = classpath;
      return this;
    }

    public Spoon build() {
      checkNotNull(androidSdk, "SDK is required.");
      checkArgument(androidSdk.exists(), "SDK path does not exist.");
      checkNotNull(applicationApk, "Application APK is required.");
      checkNotNull(instrumentationApk, "Instrumentation APK is required.");
      checkNotNull(output, "Output path is required.");
      checkNotNull(serials, "Device serials are required.");

      return new Spoon(title, androidSdk, applicationApk, instrumentationApk, output, debug,
          serials, classpath);
    }
  }

  static class CommandLineArgs {
    @Parameter(names = { "--title" }, description = "Execution title")
    public String title = DEFAULT_TITLE;

    @Parameter(names = { "--apk" }, description = "Application APK",
        converter = FileConverter.class, required = true)
    public File apk;

    @Parameter(names = { "--test-apk" }, description = "Test application APK",
        converter = FileConverter.class, required = true)
    public File testApk;

    @Parameter(names = { "--output" }, description = "Output path",
        converter = FileConverter.class)
    public File output = new File(Spoon.DEFAULT_OUTPUT_DIRECTORY);

    @Parameter(names = { "--sdk" }, description = "Path to Android SDK")
    public File sdk = new File(System.getenv("ANDROID_HOME"));

    @Parameter(names = { "--fail-on-failure" }, description = "Non-zero exit code on failure")
    public boolean failOnFailure;

    @Parameter(names = { "--debug" }, hidden = true)
    public boolean debug;

    @Parameter(names = { "-h", "--help" }, description = "Command help", help = true, hidden = true)
    public boolean help;
  }

  /* JCommander deems it necessary that this class be public. Lame. */
  public static class FileConverter implements IStringConverter<File> {
    @Override public File convert(String s) {
      return new File(s);
    }
  }

  public static void main(String... args) {
    CommandLineArgs parsedArgs = new CommandLineArgs();
    JCommander jc = new JCommander(parsedArgs);

    try {
      jc.parse(args);
    } catch (ParameterException e) {
      StringBuilder out = new StringBuilder(e.getLocalizedMessage()).append("\n\n");
      jc.usage(out);
      System.err.println(out.toString());
      System.exit(1);
      return;
    }
    if (parsedArgs.help) {
      jc.usage();
      return;
    }

    Spoon spoon = new Spoon.Builder() //
        .setTitle(parsedArgs.title)
        .setApplicationApk(parsedArgs.apk)
        .setInstrumentationApk(parsedArgs.testApk)
        .setOutputDirectory(parsedArgs.output)
        .setDebug(parsedArgs.debug)
        .setAndroidSdk(parsedArgs.sdk)
        .useAllAttachedDevices()
        .build();

    if (!spoon.run() && parsedArgs.failOnFailure) {
      System.exit(1);
    }
  }
}
