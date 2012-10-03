package com.squareup.spoon;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
  public static class FileConverter implements IStringConverter<File> {
    @Override public File convert(String s) {
      return new File(s);
    }
  }

  public static class Configuration {
    @Parameter(names = { "--title" }, description = "Execution title")
    public String title = "Spoon Execution Summary";

    @Parameter(names = { "--apk" }, description = "Application APK", converter = FileConverter.class)
    public File apk;

    @Parameter(names = { "--test-apk" }, description = "Test application APK", converter = FileConverter.class)
    public File testApk;

    @Parameter(names = { "--output" }, description = "Output path", converter = FileConverter.class)
    public File output = new File("spoon-output");

    @Parameter(names = { "--sdk" }, description = "Path to Android SDK")
    public String sdk = System.getenv("ANDROID_HOME");

    @Parameter(names = { "--debug" }, hidden = true)
    public boolean debug;

    @Parameter(names = { "-h", "--help" }, description = "Command help", help = true, hidden = true)
    public boolean help;
  }

  public static void main(String... args) throws IOException {
    Logger log = Logger.getLogger(Main.class.getSimpleName());
    Configuration config = new Configuration();
    JCommander jc = new JCommander(config);

    try {
      jc.parse(args);
    } catch (ParameterException e) {
      StringBuilder out = new StringBuilder(e.getLocalizedMessage()).append("\n\n");
      jc.usage(out);
      System.err.println(out.toString());
      System.exit(1);
      return;
    }

    if (config.help) {
      jc.usage();
      return;
    }

    log.setLevel(config.debug ? Level.FINE : Level.INFO);

    if (!new File(config.sdk).exists()) {
      throw new IllegalStateException("Could not find Android SDK. Ensure ANDROID_HOME environment variable is set.");
    }

    new ExecutionSuite(config.title, config.sdk, config.apk, config.testApk, config.output).run();
  }
}
