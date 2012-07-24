package com.squareup.spoon;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.squareup.spoon.model.Device;
import com.squareup.spoon.model.DeviceConfigs;
import com.squareup.spoon.model.RunConfig;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {
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

    @Parameter(names = {"--debug"}, hidden = true)
    public boolean debug;

    @Parameter(names = {"-h", "--help"}, description = "Command help", help = true, hidden = true)
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

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory())
        .registerModule(new SpoonMapper.SpoonModule());

    DeviceConfigs known = mapper.readValue(Main.class.getResourceAsStream("/devices.yml"), DeviceConfigs.class);
    RunConfig rc = mapper.readValue(config.runConfig, RunConfig.class);
    DeviceConfigs dc = mapper.readValue(config.deviceConfig, DeviceConfigs.class);

    log.fine("Known: " + known);
    log.fine("Run config: " + rc);
    log.fine("Device configs: " + dc);

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
