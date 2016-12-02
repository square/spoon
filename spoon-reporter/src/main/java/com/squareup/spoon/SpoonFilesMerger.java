package com.squareup.spoon;

import com.google.common.io.Files;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Copies any files in a {@link SpoonSummary} json file and rewrites the file paths to resemble the
 * new location
 */
public class SpoonFilesMerger {

  private static final Pattern FINDER_PATTERN =
    Pattern.compile("(\\S+\\.png\"|\\S+\\.gif\")");
  private static final Pattern REPLACER_PNG_PATTERN =
    Pattern.compile("[^\"]+\\/([^\\/]+\\.png\",*)$");
  private static final Pattern REPLACE_GIF_PATTERN =
    Pattern.compile("[^\"]+\\/([^\\/]+\\.gif\",*)$");

  public static File[] copyAndRewrite(File outputDir, File[] summaries) throws IOException {
    if (!outputDir.isDirectory()) {
      throw new IllegalArgumentException("output path should be a directory");
    }

    List<File> modifiedSummaries = new ArrayList<File>();
    for (File summaryFile : summaries) {
      File modifiedSummary = mergeSummary(summaryFile, outputDir);
      modifiedSummaries.add(modifiedSummary);
    }

    File[] result = new File[modifiedSummaries.size()];
    return modifiedSummaries.toArray(result);
  }

  private static File mergeSummary(File summaryFile, File outputDir) throws IOException {
    final String imagePath = outputDir.getPath() + "/images/";
    final File imageDir = new File(imagePath);
    imageDir.mkdirs();

    BufferedReader originalReader = new BufferedReader(new FileReader(summaryFile));
    File modifiedSummaryFile = File.createTempFile(summaryFile.getName(), null, outputDir);
    FileWriter modifiedWriter = new FileWriter(modifiedSummaryFile);

    String line;
    while ((line = originalReader.readLine()) != null) {

      copyFiles(line, imageDir);
      modifiedWriter.write(
        rewriteFilePaths(line, imagePath)
      );
    }

    modifiedWriter.flush();
    modifiedWriter.close();
    originalReader.close();

    return modifiedSummaryFile;
  }

  private static int copyFiles(String line, File outputDir) throws IOException {
    int noFilesCopied = 0;
    Matcher matcher = FINDER_PATTERN.matcher(line);

    if (matcher.find()) {
      for (int i = 0; i < matcher.groupCount(); i++) {
        //strip off the parenthesis
        String path = matcher.group(i);
        path = path.substring(1, path.length() - 1);

        File original = new File(path);
        File copy = new File(outputDir, original.getName());
        Files.copy(original, copy);
        noFilesCopied++;
      }
    }

    return noFilesCopied;
  }

  private static String rewriteFilePaths(String line, String outputPath) {
    String modifiedLine = line;
    Matcher replacer;

    replacer = REPLACER_PNG_PATTERN.matcher(line);
    if (replacer.find()) {
      modifiedLine = replacer.replaceAll(outputPath + "$1");
    }

    replacer = REPLACE_GIF_PATTERN.matcher(line);
    if (replacer.find()) {
      modifiedLine = replacer.replaceAll(outputPath + "$1");
    }

    return modifiedLine;
  }

}
