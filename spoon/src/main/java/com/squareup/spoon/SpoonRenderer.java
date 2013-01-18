package com.squareup.spoon;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.io.Resources;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.lesscss.LessCompiler;

import static com.google.common.base.Charsets.UTF_8;
import static com.squareup.spoon.SpoonUtils.copyResourceToOutput;

/** Renders a {@link SpoonSummary} as HTML to an output directory. */
final class SpoonRenderer {
  private static final String STATIC_DIRECTORY = "static";
  private static final String[] STATIC_ASSETS = {
    "bootstrap.min.css", "bootstrap-responsive.min.css", "bootstrap.min.js", "jquery.min.js",
    "icon-animated.png", "icon-devices.png", "ceiling_android.png"
  };

  private final SpoonSummary summary;
  private final File output;

  SpoonRenderer(SpoonSummary summary, File output) {
    this.summary = summary;
    this.output = output;
  }

  void render() {
    output.mkdirs();

    copyStaticAssets();
    generateCssFromLess();
    writeResultJson();

    MustacheFactory mustacheFactory = new DefaultMustacheFactory();
    generateIndexHtml(mustacheFactory);
    generateDeviceHtml(mustacheFactory);
    generateTestHtml(mustacheFactory);
  }

  private void copyStaticAssets() {
    File statics = new File(output, STATIC_DIRECTORY);
    statics.mkdir();
    for (String staticAsset : STATIC_ASSETS) {
      copyResourceToOutput(staticAsset, statics);
    }
  }

  private void generateCssFromLess() {
    try {
      LessCompiler compiler = new LessCompiler();
      String less = Resources.toString(getClass().getResource("/spoon.less"), UTF_8);
      String css = compiler.compile(less);
      File cssFile = FileUtils.getFile(output, STATIC_DIRECTORY, "spoon.css");
      FileUtils.writeStringToFile(cssFile, css);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void writeResultJson() {
    FileWriter result = null;
    try {
      result = new FileWriter(new File(output, "result.json"));
      SpoonUtils.GSON.toJson(summary, result);
    } catch (IOException e) {
      throw new RuntimeException("Unable to write result.json file.", e);
    } finally {
      IOUtils.closeQuietly(result);
    }
  }

  private void generateIndexHtml(MustacheFactory mustacheFactory) {
    Mustache mustache = mustacheFactory.compile("index.html");
    HtmlIndex scope = HtmlIndex.from(summary);
    File file = new File(output, "index.html");
    renderMustacheToFile(mustache, scope, file);
  }

  private void generateDeviceHtml(MustacheFactory mustacheFactory) {
    Mustache mustache = mustacheFactory.compile("device.html");
    for (Map.Entry<String, DeviceResult> entry : summary.getResults().entrySet()) {
      String serial = entry.getKey();
      HtmlDevice scope = HtmlDevice.from(serial, entry.getValue(), output);
      File file = FileUtils.getFile(output, "device", serial + ".html");
      renderMustacheToFile(mustache, scope, file);
    }
  }

  private void generateTestHtml(MustacheFactory mustacheFactory) {
    Mustache mustache = mustacheFactory.compile("test.html");
    // Create a set of unique tests.
    Set<DeviceTest> tests = new HashSet<DeviceTest>();
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

  private static void renderMustacheToFile(Mustache mustache, Object scope, File file) {
    FileWriter writer = null;
    try {
      file.getParentFile().mkdirs();
      writer = new FileWriter(file);
      mustache.execute(writer, scope);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      IOUtils.closeQuietly(writer);
    }
  }
}
