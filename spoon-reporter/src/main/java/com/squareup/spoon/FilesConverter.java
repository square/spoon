package com.squareup.spoon;

import com.beust.jcommander.IStringConverter;

import java.io.File;

/** Converts a comma seperated list of paths to files to an array of {@link File}s.*/
public class FilesConverter implements IStringConverter<File[]> {
  public FilesConverter() {
  }

  public File[] convert(String value) {
    String[] paths = value.split(",");

    File[] files = new File[paths.length];
    for(int i=0; i < paths.length; i++){
      files[i] = new File(paths[i]);
    }
    return files;
  }
}
