package com.squareup.spoon;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.FileConverter;
import com.google.gson.Gson;
import com.squareup.spoon.html.HtmlRenderer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * processes output of the Spoon runner and generates human readable reports
 */
public class SpoonReporter {

  private static final String DEFAULT_TITLE = "Spoon Execution";

  private final SpoonSummaryMerger summaryMerger;
  private final String title;
  private final File[] inputs;
  private final File output;

  SpoonReporter(Gson gson, String title, File[] inputs, File output) throws FileNotFoundException {
    this.summaryMerger = new SpoonSummaryMerger(gson);
    this.title = title;
    this.inputs = inputs;
    this.output = output;
  }

  public static void main(String... args) throws IOException {
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

    SpoonReporter reporter = new SpoonReporter(
      SpoonUtils.GSON,
      parsedArgs.title,
      parsedArgs.input,
      parsedArgs.output);
    reporter.run();
  }

  public void run() throws IOException {
    output.mkdirs();
    File[] modified = SpoonFilesMerger.copyAndRewrite(output, inputs);
    SpoonSummary summary = summaryMerger.merge(title, modified);
    new HtmlRenderer(summary, SpoonUtils.GSON, output).render();
  }

  static class CommandLineArgs {
    @Parameter(names = {"--title" }, description = "Execution title")
    public String title = DEFAULT_TITLE;

    @Parameter(names = {"--input" }, description = "Comma seperated paths to Spoon reports",
      converter = FilesConverter.class, required = true)
    public File[] input;


    @Parameter(names = {"--output" }, description = "Output path",
      converter = FileConverter.class)
    public File output = cleanFile(SpoonRunner.DEFAULT_OUTPUT_DIRECTORY);


    @Parameter(names = {"--debug" }, hidden = true)
    public boolean debug;


    @Parameter(names = {"-h", "--help" }, description = "Command help", help = true, hidden = true)
    public boolean help;
  }

  private static File cleanFile(String path) {
    if (path == null) {
      return null;
    }
    return new File(path);
  }

}
