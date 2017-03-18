package com.squareup.spoon;

import com.squareup.spoon.html.HtmlRenderer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public final class SpoonHtmlRendererTest {

  private static final String SPOON_IN_RUSSIAN = "\u041B\u043E\u0436\u043A\u0430";
  private static final String[] FILE_EXTENSIONS_TO_CHECK = {"html", "css", "json", "js"};

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @Test
  public void correctRenderingOfNonLatinCharacters() throws IOException {

    SpoonSummary summary = prepareNonLatinSummary();
    File folder = testFolder.getRoot();
    CharsetDecoder utf8Decoder = StandardCharsets.UTF_8.newDecoder();

    HtmlRenderer htmlRenderer = new HtmlRenderer(summary, SpoonUtils.GSON, folder);
    htmlRenderer.render();

    setDefaultCharset(StandardCharsets.US_ASCII);
    Iterator<File> it = FileUtils.iterateFiles(folder, FILE_EXTENSIONS_TO_CHECK, true);
    File nextFile = null;

    try {
      while (it.hasNext()) {
        nextFile = it.next();
        decode(nextFile, utf8Decoder);
      }
    } catch (MalformedInputException ex) {
      throw new IllegalStateException(String.format("Found wrong file [%s]", nextFile.getName()));
    } finally {
      setDefaultCharset(null);
    }
  }

  private SpoonSummary prepareNonLatinSummary() {
    DeviceTest device = new DeviceTest("foo", "bar");
    return new SpoonSummary.Builder() //
        .setTitle(SPOON_IN_RUSSIAN) //
        .start() //
        .addResult(SPOON_IN_RUSSIAN, new DeviceResult.Builder().startTests() //
            .addTestResultBuilder(device, new DeviceTestResult.Builder() //
                .startTest() //
                .markTestAsFailed(SPOON_IN_RUSSIAN).endTest()) //
            .addException(new RuntimeException(SPOON_IN_RUSSIAN)) //
            .build()) //
        .end() //
        .build();
  }

  private void setDefaultCharset(Charset value) {
    Field charset;
    try {
      charset = Charset.class.getDeclaredField("defaultCharset");
      charset.setAccessible(true);
      charset.set(null, value);
    } catch (NoSuchFieldException | IllegalAccessException ignored) {
    }
  }

  private void decode(File file, CharsetDecoder charsetDecoder) throws IOException {
    try (FileInputStream fis = new FileInputStream(file)) {
      FileChannel fileChannel = fis.getChannel();
      ByteBuffer buffer = ByteBuffer.allocate((int) fileChannel.size());
      fileChannel.read(buffer);
      buffer.rewind();
      charsetDecoder.decode(buffer);
    }
  }

}
