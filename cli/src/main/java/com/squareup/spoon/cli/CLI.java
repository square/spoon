package com.squareup.spoon.cli;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.squareup.spoon.ExecutionSuite;
import com.squareup.spoon.SpoonMapper;
import com.squareup.spoon.model.Device;
import com.squareup.spoon.model.DeviceConfigs;
import com.squareup.spoon.model.RunConfig;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CLI {
  public static class FileConverter implements IStringConverter<File> {
    @Override public File convert(String s) {
      return new File(s);
    }
  }

  public static class Configuration {
    @Parameter(names = {"-rc", "--run-config"}, description = "Run configuration", converter = FileConverter.class)
    public File runConfig = new File("./run-config.yml");

    @Parameter(names = {"-dc", "--device-config"}, description = "Device configuration", converter = FileConverter.class)
    public File deviceConfig = new File("./device-config.yml");

    @Parameter(names = {"-o", "--output"}, description = "Output directory", converter = FileConverter.class)
    public File outputDir = new File("./matrix-results/");

    @Parameter(names = {"-v", "--verbose"}, description = "Verbose mode")
    public boolean verbose;

    @Parameter(description = "[DeviceID ...]")
    public List<String> ids;

    @Parameter(names = {"-h", "--help"}, description = "Command help", help = true, hidden = true)
    public boolean help;
  }

  public static void main(String... args) throws IOException {
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

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
        .registerModule(new SpoonMapper.SpoonModule());

    DeviceConfigs known = mapper.readValue(CLI.class.getResourceAsStream("/devices.yml"), DeviceConfigs.class);
    RunConfig rc = mapper.readValue(config.runConfig, RunConfig.class);
    DeviceConfigs dc = mapper.readValue(config.deviceConfig, DeviceConfigs.class);

    System.out.println("Known: " + known);
    System.out.println("Run file: " + rc);
    System.out.println("Device file: " + dc);
    System.out.println("Output: " + config.outputDir);
    System.out.println("Verbose: " + config.verbose);
    System.out.println("IDs: " + config.ids);

    System.out.println("\n");

    Set<Device> devices = new HashSet<Device>();
    if (dc.custom != null) {
      for (Map.Entry<String, Device> custom : dc.custom.entrySet()) {
        devices.add(custom.getValue());
      }
    }

    String sdkPath = System.getenv("ANDROID_HOME");
    if (sdkPath == null || !new File(sdkPath).exists()) {
      throw new IllegalStateException("Could not find Android SDK. Ensure ANDROID_HOME environment variable is set.");
    }

    new ExecutionSuite(sdkPath, rc, devices, dc.all).run();
  }
}
