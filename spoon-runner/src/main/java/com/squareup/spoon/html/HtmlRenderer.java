package com.squareup.spoon.html;

import com.android.ddmlib.logcat.LogCatMessage;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.squareup.spoon.DeviceDetails;
import com.squareup.spoon.DeviceResult;
import com.squareup.spoon.DeviceTest;
import com.squareup.spoon.DeviceTestResult;
import com.squareup.spoon.SpoonSummary;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.lesscss.LessCompiler;

import static java.nio.charset.StandardCharsets.UTF_8;

/** Renders a {@link com.squareup.spoon.SpoonSummary} as static HTML to an output directory. */
public final class HtmlRenderer {
  public static final String INDEX_FILENAME = "index.html";
  private static final String STATIC_DIRECTORY = "static";
  private static final String[] STATIC_ASSETS = {
    "bootstrap.min.css", "bootstrap-responsive.min.css", "bootstrap.min.js", "jquery.min.js",
    "jquery.nivo.slider.pack.js", "nivo-slider.css", "ceiling_android.png", "arrows.png",
    "bullets.png", "loading.gif"
  };

  private final SpoonSummary summary;
  private final Gson gson;
  private final File output;

  public HtmlRenderer(SpoonSummary summary, Gson gson, File output) {
    this.summary = summary;
    this.gson = gson;
    this.output = output;
  }

  public void render() {
    output.mkdirs();

    copyStaticAssets();
    generateCssFromLess();
    writeResultJson();

    MustacheFactory mustacheFactory = new DefaultMustacheFactory();
    generateTvHtml(mustacheFactory);
    generateIndexHtml(mustacheFactory);
    generateDeviceHtml(mustacheFactory);
    generateTestHtml(mustacheFactory);
    generateLogHtml(mustacheFactory);
    saveRawLog();
  }

  private void copyStaticAssets() {
    File statics = new File(output, STATIC_DIRECTORY);
    statics.mkdirs();
    for (String staticAsset : STATIC_ASSETS) {
      copyStaticToOutput(staticAsset, statics);
    }
  }

  private void generateCssFromLess() {
    try {
      LessCompiler compiler = new LessCompiler();
      String less = Resources.toString(getClass().getResource("/spoon.less"), UTF_8);
      String css = compiler.compile(less);
      File cssFile = FileUtils.getFile(output, STATIC_DIRECTORY, "spoon.css");
      FileUtils.writeStringToFile(cssFile, css, UTF_8);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void writeResultJson() {
    try (Writer result = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(new File(output, "result.json")), UTF_8))) {
      gson.toJson(summary, result);
    } catch (IOException e) {
      throw new RuntimeException("Unable to write result.json file.", e);
    }
  }

  private void generateTvHtml(MustacheFactory mustacheFactory) {
    Mustache mustache = mustacheFactory.compile("page/tv.html");
    HtmlTv scope = HtmlTv.from(gson, summary, output);
    File file = new File(output, "tv.html");
    renderMustacheToFile(mustache, scope, file);
  }

  private void generateIndexHtml(MustacheFactory mustacheFactory) {
    Mustache mustache = mustacheFactory.compile("page/index.html");
    HtmlIndex scope = HtmlIndex.from(summary);
    File file = new File(output, INDEX_FILENAME);
    renderMustacheToFile(mustache, scope, file);
  }

  private void generateDeviceHtml(MustacheFactory mustacheFactory) {
    Mustache mustache = mustacheFactory.compile("page/device.html");
    for (Map.Entry<String, DeviceResult> entry : summary.getResults().entrySet()) {
      String serial = entry.getKey();
      HtmlDevice scope = HtmlDevice.from(serial, entry.getValue(), output);
      File file = FileUtils.getFile(output, "device", serial + ".html");
      renderMustacheToFile(mustache, scope, file);
    }
  }

  private void generateTestHtml(MustacheFactory mustacheFactory) {
    Mustache mustache = mustacheFactory.compile("page/test.html");
    // Create a set of unique tests.
    Set<DeviceTest> tests = new LinkedHashSet<>();
    for (DeviceResult deviceResult : summary.getResults().values()) {
      tests.addAll(deviceResult.getTestResults().keySet());
    }
    // Generate a page for each one.
    for (DeviceTest test : tests) {
      HtmlTest scope = HtmlTest.from(test, summary, output);
      File file =
          FileUtils.getFile(output, "test", test.getClassName(), test.getMethodName() + ".html");
      renderMustacheToFile(mustache, scope, file);
    }
  }

  private void generateLogHtml(MustacheFactory mustacheFactory) {
    Mustache mustache = mustacheFactory.compile("page/log.html");
    for (Map.Entry<String, DeviceResult> resultEntry : summary.getResults().entrySet()) {
      String serial = resultEntry.getKey();
      DeviceResult result = resultEntry.getValue();
      DeviceDetails details = result.getDeviceDetails();
      String name = (details != null) ? details.getName() : serial;
      for (Map.Entry<DeviceTest, DeviceTestResult> entry : result.getTestResults().entrySet()) {
        DeviceTest test = entry.getKey();
        HtmlLog scope = HtmlLog.from(name, test, entry.getValue());
        File file = FileUtils.getFile(output, "logs", serial, test.getClassName(),
            test.getMethodName() + ".html");
        renderMustacheToFile(mustache, scope, file);
      }
    }
  }

  private void saveRawLog() {
    for (Map.Entry<String, DeviceResult> resultEntry : summary.getResults().entrySet()) {
      String serial = resultEntry.getKey();
      DeviceResult result = resultEntry.getValue();
      for (Map.Entry<DeviceTest, DeviceTestResult> entry : result.getTestResults().entrySet()) {
        DeviceTest test = entry.getKey();
        File rawFile = FileUtils.getFile(output, "logs", serial, test.getClassName(),
            test.getMethodName() + ".log");
        saveRawLogFile(rawFile, entry.getValue());
      }
    }
  }

  private void saveRawLogFile(File rawFile, DeviceTestResult deviceTestResult) {
    rawFile.getParentFile().mkdirs();
    try {
      if (!rawFile.createNewFile() || !rawFile.canWrite()) {
        return;
      }
    } catch (IOException e) {
      throw new RuntimeException("Unable to create raw log file " + rawFile.getAbsolutePath(), e);
    }

    try (Writer writer = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(rawFile), UTF_8))) {
      for (LogCatMessage logCatMessage : deviceTestResult.getLog()) {
        writer.write(logCatMessage.getTimestamp().toString());
        writer.write(" ");
        writer.write(logCatMessage.getLogLevel().getStringValue());
        writer.write(" ");
        writer.write(logCatMessage.getTag());
        writer.write(" ");
        writer.write(logCatMessage.getMessage());
        writer.write("\n");
      }

    } catch (IOException e) {
      throw new RuntimeException("Unable to write raw log file to " + rawFile.getAbsolutePath(), e);
    }
  }

  private static void renderMustacheToFile(Mustache mustache, Object scope, File file) {
    file.getParentFile().mkdirs();
    try (Writer writer = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(file), UTF_8))) {
      mustache.execute(writer, scope);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void copyStaticToOutput(String resource, File output) {
    InputStream is = null;
    OutputStream os = null;
    try {
      is = HtmlRenderer.class.getResourceAsStream("/static/" + resource);
      os = new FileOutputStream(new File(output, resource));
      IOUtils.copy(is, os);
    } catch (IOException e) {
      throw new RuntimeException("Unable to copy static resource " + resource + " to " + output, e);
    } finally {
      IOUtils.closeQuietly(is);
      IOUtils.closeQuietly(os);
    }
  }
}
