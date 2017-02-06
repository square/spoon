package com.squareup.spoon;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.testrunner.IRemoteAndroidTestRunner;
import com.android.ddmlib.testrunner.ITestRunListener;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.IParameterSplitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.squareup.spoon.html.HtmlRenderer;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.io.FileUtils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.squareup.spoon.DeviceTestResult.Status;
import static com.squareup.spoon.SpoonInstrumentationInfo.parseFromFile;
import static com.squareup.spoon.SpoonLogger.logDebug;
import static com.squareup.spoon.SpoonLogger.logInfo;
import static java.util.Collections.synchronizedSet;

/** Represents a collection of devices and the test configuration to be executed. */
public final class SpoonRunner {
  private static final String DEFAULT_TITLE = "Spoon Execution";
  public static final String DEFAULT_OUTPUT_DIRECTORY = "spoon-output";
  private static final int DEFAULT_ADB_TIMEOUT_SEC = 10 * 60; //10 minutes
  private final ExecutorService threadExecutor;

  private final String title;
  private final File androidSdk;
  private final File applicationApk;
  private final File instrumentationApk;
  private final File output;
  private final boolean debug;
  private final boolean noAnimations;
  private final int adbTimeoutMillis;
  private final List<String> instrumentationArgs;
  private final String className;
  private final String methodName;
  private final Set<String> serials;
  private final Set<String> skipDevices;
  private final boolean shard;
  private final String classpath;
  private final IRemoteAndroidTestRunner.TestSize testSize;
  private boolean codeCoverage;
  private final boolean failIfNoDeviceConnected;
  private final List<ITestRunListener> testRunListeners;
  private final boolean terminateAdb;
  private File initScript;
  private final boolean grantAll;

  private SpoonRunner(String title, File androidSdk, File applicationApk, File instrumentationApk,
      File output, boolean debug, boolean noAnimations, int adbTimeoutMillis, Set<String> serials,
      Set<String> skipDevices,
      boolean shard, String classpath, List<String> instrumentationArgs, String className,
      String methodName, IRemoteAndroidTestRunner.TestSize testSize,
      boolean failIfNoDeviceConnected, List<ITestRunListener> testRunListeners, boolean sequential,
      File initScript, boolean grantAll, boolean terminateAdb, boolean codeCoverage) {
    this.title = title;
    this.androidSdk = androidSdk;
    this.applicationApk = applicationApk;
    this.instrumentationApk = instrumentationApk;
    this.output = output;
    this.debug = debug;
    this.noAnimations = noAnimations;
    this.adbTimeoutMillis = adbTimeoutMillis;
    this.instrumentationArgs = instrumentationArgs;
    this.className = className;
    this.methodName = methodName;
    this.classpath = classpath;
    this.testSize = testSize;
    this.skipDevices = skipDevices;
    this.codeCoverage = codeCoverage;
    this.serials = ImmutableSet.copyOf(serials);
    this.shard = shard;
    this.failIfNoDeviceConnected = failIfNoDeviceConnected;
    this.testRunListeners = testRunListeners;
    this.terminateAdb = terminateAdb;
    this.initScript = initScript;
    this.grantAll = grantAll;

    if (sequential) {
      this.threadExecutor = Executors.newSingleThreadExecutor();
    } else {
      this.threadExecutor = Executors.newCachedThreadPool();
    }
  }

  /**
   * Install and execute the tests on all specified devices.
   *
   * @return {@code true} if there were no test failures or exceptions thrown.
   */
  public boolean run() {
    checkArgument(applicationApk.exists(), "Could not find application APK.");
    checkArgument(instrumentationApk.exists(), "Could not find instrumentation APK.");

    AndroidDebugBridge adb = SpoonUtils.initAdb(androidSdk, adbTimeoutMillis);

    try {
      final SpoonInstrumentationInfo testInfo = parseFromFile(instrumentationApk);

      // If we were given an empty serial set, load all available devices.
      Set<String> serials = this.serials;
      if (serials.isEmpty()) {
        serials = SpoonUtils.findAllDevices(adb, testInfo.getMinSdkVersion());
      }
      if (this.skipDevices != null && !this.skipDevices.isEmpty()) {
        serials.removeAll(this.skipDevices);
      }
      if (failIfNoDeviceConnected && serials.isEmpty()) {
        throw new RuntimeException("No device(s) found.");
      }

      // Execute all the things...
      SpoonSummary summary = runTests(adb, serials, testInfo);
      // ...and render to HTML
      new HtmlRenderer(summary, SpoonUtils.GSON, output).render();
      if (codeCoverage) {
        SpoonCoverageMerger coverageMerger = new SpoonCoverageMerger();
        try {
          coverageMerger.mergeCoverageFiles(serials, output);
          logDebug(debug, "Merging of coverage files done.");
        } catch (IOException exception) {
          throw new RuntimeException("Error while merging coverage files. "
              + "Did you set the \"testCoverageEnabled\" flag in your build.gradle?", exception);
        }
      }

      return parseOverallSuccess(summary);
    } finally {
      if (terminateAdb) {
        AndroidDebugBridge.terminate();
      }
    }
  }

  private SpoonSummary runTests(AndroidDebugBridge adb, Set<String> serials,
      final SpoonInstrumentationInfo testInfo) {
    int targetCount = serials.size();
    logInfo("Executing instrumentation suite on %d device(s).", targetCount);

    try {
      FileUtils.deleteDirectory(output);
    } catch (IOException e) {
      throw new RuntimeException("Unable to clean output directory: " + output, e);
    }

    logDebug(debug, "Application: %s from %s", testInfo.getApplicationPackage(),
        applicationApk.getAbsolutePath());
    logDebug(debug, "Instrumentation: %s from %s", testInfo.getInstrumentationPackage(),
        instrumentationApk.getAbsolutePath());

    final SpoonSummary.Builder summary = new SpoonSummary.Builder().setTitle(title).start();

    if (testSize != null) {
      summary.setTestSize(testSize);
    }

    executeInitScript();

    if (targetCount == 1) {
      // Since there is only one device just execute it synchronously in this process.
      String serial = Iterables.getOnlyElement(serials);
      String safeSerial = SpoonUtils.sanitizeSerial(serial);
      try {
        logDebug(debug, "[%s] Starting execution.", serial);
        summary.addResult(safeSerial, getTestRunner(serial, 0, 0, testInfo).run(adb));
      } catch (Exception e) {
        logDebug(debug, "[%s] Execution exception!", serial);
        e.printStackTrace(System.out);
        summary.addResult(safeSerial, new DeviceResult.Builder().addException(e).build());
      } finally {
        logDebug(debug, "[%s] Execution done.", serial);
      }
    } else {
      // Spawn a new thread for each device and wait for them all to finish.
      final CountDownLatch done = new CountDownLatch(targetCount);
      final Set<String> remaining = synchronizedSet(new HashSet<>(serials));

      int shardIndex = 0;
      final int numShards = shard ? serials.size() : 0;
      for (final String serial : serials) {
        final String safeSerial = SpoonUtils.sanitizeSerial(serial);
        logDebug(debug, "[%s] Starting execution.", serial);
        final int safeShardIndex = shardIndex;
        Runnable runnable = new Runnable() {
          @Override public void run() {
            try {
              summary.addResult(safeSerial,
                  getTestRunner(serial, safeShardIndex, numShards, testInfo).runInNewProcess());
            } catch (Exception e) {
              e.printStackTrace(System.out);
              summary.addResult(safeSerial, new DeviceResult.Builder().addException(e).build());
            } finally {
              done.countDown();
              remaining.remove(serial);
              logDebug(debug, "[%s] Execution done. (%s remaining %s)", serial, done.getCount(),
                  remaining);
            }
          }
        };
        if (shard) {
          shardIndex++;
          logDebug(debug, "shardIndex [%d]", shardIndex);
        }
        threadExecutor.execute(runnable);
      }

      try {
        done.await();
        threadExecutor.shutdown();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }

    if (!debug) {
      // Clean up anything in the work directory.
      try {
        FileUtils.deleteDirectory(new File(output, SpoonDeviceRunner.TEMP_DIR));
      } catch (IOException ignored) {
      }
    }

    return summary.end().build();
  }

  /** Execute the script file specified in param --init-script */
  private void executeInitScript() {
    if (initScript != null && initScript.exists()) {
      try {
        Runtime run = Runtime.getRuntime();
        Process proc = run.exec(new String[] {
            "/bin/bash", "-c", initScript.getAbsolutePath()
        });
        BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String line;

        logInfo("Output of running script is");
        while ((line = br.readLine()) != null) {
          logInfo(line);
        }
        proc.waitFor(); // Wait for the process to finish.
        logInfo("Script executed successfully %s", initScript.getAbsolutePath());
      } catch (IOException e) {
        logDebug(debug, "Error executing script for path: %s", initScript.getAbsolutePath());
        e.printStackTrace(System.out);
      } catch (InterruptedException e) {
        logDebug(debug, "Script execution interrupted for path: %s", initScript.getAbsolutePath());
        e.printStackTrace(System.out);
      }
    }
  }

  /** Returns {@code false} if a test failed on any device. */
  static boolean parseOverallSuccess(SpoonSummary summary) {
    for (DeviceResult result : summary.getResults().values()) {
      if (result.getInstallFailed()) {
        return false; // App and/or test installation failed.
      }
      if (!result.getExceptions().isEmpty() || result.getTestResults().isEmpty()) {
        return false; // Top-level exception present, or no tests were run.
      }
      for (DeviceTestResult methodResult : result.getTestResults().values()) {
        if (methodResult.getStatus() != Status.PASS) {
          return false; // Individual test failure.
        }
      }
    }
    return true;
  }

  private SpoonDeviceRunner getTestRunner(String serial, int shardIndex, int numShards,
      SpoonInstrumentationInfo testInfo) {
    return new SpoonDeviceRunner(androidSdk, applicationApk, instrumentationApk, output, serial,
        shardIndex, numShards, debug, noAnimations, adbTimeoutMillis, classpath, testInfo,
        instrumentationArgs, className, methodName, testSize, testRunListeners, codeCoverage,
        grantAll);
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
    private Set<String> skipDevices;
    private String classpath = System.getProperty("java.class.path");
    private List<String> instrumentationArgs;
    private String className;
    private String methodName;
    private boolean noAnimations;
    private IRemoteAndroidTestRunner.TestSize testSize;
    private int adbTimeoutMillis = DEFAULT_ADB_TIMEOUT_SEC * 1000;
    private boolean failIfNoDeviceConnected;
    private List<ITestRunListener> testRunListeners = new ArrayList<>();
    private boolean sequential;
    private File initScript;
    private boolean grantAll;
    private boolean terminateAdb = true;
    private boolean codeCoverage;
    private boolean shard = false;

    /** Identifying title for this execution. */
    public Builder setTitle(String title) {
      checkNotNull(title, "Title cannot be null.");
      this.title = title;
      return this;
    }

    /** Path to the local Android SDK directory. */
    public Builder setAndroidSdk(File androidSdk) {
      checkNotNull(androidSdk, "SDK path not specified.");
      checkArgument(androidSdk.exists(), "SDK path does not exist.");
      this.androidSdk = androidSdk;
      return this;
    }

    /** Path to application APK. */
    public Builder setApplicationApk(File apk) {
      checkNotNull(apk, "APK path not specified.");
      checkArgument(apk.exists(), "APK path does not exist.");
      this.applicationApk = apk;
      return this;
    }

    /** Path to instrumentation APK. */
    public Builder setInstrumentationApk(File apk) {
      checkNotNull(apk, "Instrumentation APK path not specified.");
      checkArgument(apk.exists(), "Instrumentation APK path does not exist.");
      this.instrumentationApk = apk;
      return this;
    }

    /** Path to output directory. */
    public Builder setOutputDirectory(File output) {
      checkNotNull(output, "Output directory not specified.");
      this.output = output;
      return this;
    }

    /** Whether or not debug logging is enabled. */
    public Builder setDebug(boolean debug) {
      this.debug = debug;
      return this;
    }

    /** Whether or not animations are enabled. */
    public Builder setNoAnimations(boolean noAnimations) {
      this.noAnimations = noAnimations;
      return this;
    }

    /** Set ADB timeout. */
    public Builder setAdbTimeout(int value) {
      this.adbTimeoutMillis = value;
      return this;
    }

    /** Add a device serial for test execution. */
    public Builder addDevice(String serial) {
      checkNotNull(serial, "Serial cannot be null.");
      checkArgument(serials == null || !serials.isEmpty(), "Already marked as using all devices.");
      if (serials == null) {
        serials = new LinkedHashSet<>();
      }
      serials.add(serial);
      return this;
    }

    /** Add a device serial for skipping test execution. */
    public Builder skipDevice(String serial) {
      checkNotNull(serial, "Serial cannot be null.");
      if (skipDevices == null) {
        skipDevices = new LinkedHashSet<>();
      }
      skipDevices.add(serial);
      return this;
    }

    /** Use all currently attached device serials when executed. */
    public Builder useAllAttachedDevices() {
      if (this.serials != null) {
        throw new IllegalStateException("Serial list already contains entries.");
      }
      if (this.skipDevices != null) {
        logInfo("Skipping devices not empty, will skip following serials: ", skipDevices);
      }
      if (this.androidSdk == null) {
        throw new IllegalStateException("SDK must be set before calling this method.");
      }
      this.serials = Collections.emptySet();
      return this;
    }

    /** Classpath to use for new JVM processes. */
    public Builder setClasspath(String classpath) {
      checkNotNull(classpath, "Classpath cannot be null.");
      this.classpath = classpath;
      return this;
    }

    public Builder setInstrumentationArgs(List<String> instrumentationArgs) {
      this.instrumentationArgs = instrumentationArgs;
      return this;
    }

    public Builder setClassName(String className) {
      this.className = className;
      return this;
    }

    public Builder setTestSize(IRemoteAndroidTestRunner.TestSize testSize) {
      this.testSize = testSize;
      return this;
    }

    public Builder setFailIfNoDeviceConnected(boolean failIfNoDeviceConnected) {
      this.failIfNoDeviceConnected = failIfNoDeviceConnected;
      return this;
    }

    public Builder setSequential(boolean sequential) {
      this.sequential = sequential;
      return this;
    }

    public Builder setInitScript(File initScript) {
      if (initScript != null) {
        checkArgument(initScript.exists(),
            "Script path does not exist " + initScript.getAbsolutePath());
      }
      this.initScript = initScript;
      return this;
    }

    public Builder setGrantAll(boolean grantAll) {
      this.grantAll = grantAll;
      return this;
    }

    public Builder setMethodName(String methodName) {
      this.methodName = methodName;
      return this;
    }

    public Builder setCodeCoverage(boolean codeCoverage) {
      this.codeCoverage = codeCoverage;
      return this;
    }

    public Builder setShard(boolean shard) {
      this.shard = shard;
      return this;
    }

    public Builder addTestRunListener(ITestRunListener testRunListener) {
      checkNotNull(testRunListener, "TestRunListener cannot be null.");
      testRunListeners.add(testRunListener);
      return this;
    }

    public Builder setTerminateAdb(boolean terminateAdb) {
      this.terminateAdb = terminateAdb;
      return this;
    }

    public SpoonRunner build() {
      checkNotNull(androidSdk, "SDK is required.");
      checkArgument(androidSdk.exists(), "SDK path does not exist.");
      checkNotNull(applicationApk, "Application APK is required.");
      checkNotNull(instrumentationApk, "Instrumentation APK is required.");
      checkNotNull(output, "Output path is required.");
      checkNotNull(serials, "Device serials are required.");
      if (!isNullOrEmpty(methodName)) {
        checkArgument(!isNullOrEmpty(className),
            "Must specify class name if you're specifying a method name.");
      }

      return new SpoonRunner(title, androidSdk, applicationApk, instrumentationApk, output, debug,
          noAnimations, adbTimeoutMillis, serials, skipDevices, shard, classpath,
          instrumentationArgs, className, methodName, testSize, failIfNoDeviceConnected,
          testRunListeners, sequential, initScript, grantAll, terminateAdb, codeCoverage);
    }
  }

  /**
   * Instances where string input should not be split there needs to
   * be a way override the default splitting behavior.
   * This class provides that options
   */
  public static class NoSplitter implements IParameterSplitter {
    @Override public List<String> split(String value) {
      return Collections.singletonList(value);
    }
  }

  static class CommandLineArgs {
    @Parameter(names = { "--title" }, description = "Execution title") //
    public String title = DEFAULT_TITLE;

    @Parameter(names = { "--apk" }, description = "Application APK",
        converter = FileConverter.class, required = true) //
    public File apk;

    @Parameter(names = { "--test-apk" }, description = "Test application APK",
        converter = FileConverter.class, required = true) //
    public File testApk;

    @Parameter(names = { "--e" }, variableArity = true, splitter = NoSplitter.class,
        description = "Arguments to pass to the Instrumentation Runner. This can be used multiple"
            + " times for multiple entries. Usage: --e <NAME>=<VALUE>.") //
    public List<String> instrumentationArgs;

    @Parameter(names = { "--class-name" }, description = "Test class name to run (fully-qualified)")
    public String className;

    @Parameter(names = { "--method-name" },
        description = "Test method name to run (must also use --class-name)") //
    public String methodName;

    @Parameter(names = { "--size" }, converter = TestSizeConverter.class,
        description = "Only run methods with corresponding size annotation (small, medium, large)")
    public IRemoteAndroidTestRunner.TestSize size;

    @Parameter(names = { "--output" }, description = "Output path",
        converter = FileConverter.class) //
    public File output = cleanFile(SpoonRunner.DEFAULT_OUTPUT_DIRECTORY);

    @Parameter(names = { "--sdk" }, description = "Path to Android SDK") //
    public File sdk = cleanFile(System.getenv("ANDROID_HOME"));

    @Parameter(names = { "--fail-on-failure" }, description = "Non-zero exit code on failure")
    public boolean failOnFailure;

    @Parameter(names = { "--fail-if-no-device-connected" },
        description = "Fail if no device is connected") //
    public boolean failIfNoDeviceConnected;

    @Parameter(names = { "--sequential" },
        description = "Execute tests sequentially (one device at a time)") //
    public boolean sequential;

    @Parameter(names = { "--init-script" },
        description = "Script file executed between each devices",
        converter = FileConverter.class) //
    public File initScript;

    @Parameter(names = { "--grant-all" },
        description = "Grant all runtime permissions during installation on Marshmallow and above"
                + " devices") //
    public boolean grantAll;

    @Parameter(names = { "--no-animations" }, description = "Disable animated gif generation")
    public boolean noAnimations;

    @Parameter(names = { "--adb-timeout" },
        description = "Set maximum execution time per test in seconds (10min default)") //
    public int adbTimeoutSeconds = DEFAULT_ADB_TIMEOUT_SEC;

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") //
    @Parameter(names = "-serial",
        description = "Serial of the device to use (May be used multiple times)")
    private List<String> serials = new ArrayList<>();

    @Parameter(names = "-skipDevices",
        description = "Serial of the device to skip (May be used multiple times)")
    private List<String> skipDevices = new ArrayList<>();

    @Parameter(names = { "--shard" },
        description = "Automatically shard across all specified serials") //
    public boolean shard;

    @Parameter(names = { "--debug" }, hidden = true) //
    public boolean debug;

    @Parameter(names = { "--coverage" }, description = "Code coverage flag", arity = 1)
    public Boolean codeCoverage = false;

    @Parameter(names = { "-h", "--help" }, description = "Command help", help = true, hidden = true)
    public boolean help;
  }

  private static File cleanFile(String path) {
    if (path == null) {
      return null;
    }
    return new File(path);
  }

  /* JCommander deems it necessary that this class be public. Lame. */
  public static class FileConverter implements IStringConverter<File> {
    @Override public File convert(String s) {
      return cleanFile(s);
    }
  }

  public static class TestSizeConverter
      implements IStringConverter<IRemoteAndroidTestRunner.TestSize> {
    @Override public IRemoteAndroidTestRunner.TestSize convert(String value) {
      try {
        return IRemoteAndroidTestRunner.TestSize.getTestSize(value);
      } catch (IllegalArgumentException e) {
        throw new ParameterException(e.getMessage());
      }
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

    Builder builder = new SpoonRunner.Builder() //
        .setTitle(parsedArgs.title)
        .setApplicationApk(parsedArgs.apk)
        .setInstrumentationApk(parsedArgs.testApk)
        .setOutputDirectory(parsedArgs.output)
        .setDebug(parsedArgs.debug)
        .setAndroidSdk(parsedArgs.sdk)
        .setNoAnimations(parsedArgs.noAnimations)
        .setTestSize(parsedArgs.size)
        .setAdbTimeout(parsedArgs.adbTimeoutSeconds * 1000)
        .setFailIfNoDeviceConnected(parsedArgs.failIfNoDeviceConnected)
        .setSequential(parsedArgs.sequential)
        .setInitScript(parsedArgs.initScript)
        .setGrantAll(parsedArgs.grantAll)
        .setInstrumentationArgs(parsedArgs.instrumentationArgs)
        .setCodeCoverage(parsedArgs.codeCoverage)
        .setClassName(parsedArgs.className)
        .setMethodName(parsedArgs.methodName)
        .setShard(parsedArgs.shard);

    if (parsedArgs.serials == null || parsedArgs.serials.isEmpty()) {
      builder.useAllAttachedDevices();
    } else {
      parsedArgs.serials.forEach(builder::addDevice);
    }

    //checks if there are any devices to skip testing on
    if (parsedArgs.skipDevices != null && !parsedArgs.skipDevices.isEmpty()) {
      parsedArgs.skipDevices.forEach(builder::skipDevice);
    }

    SpoonRunner spoonRunner = builder.build();

    if (!spoonRunner.run() && parsedArgs.failOnFailure) {
      System.exit(1);
    }
  }
}
