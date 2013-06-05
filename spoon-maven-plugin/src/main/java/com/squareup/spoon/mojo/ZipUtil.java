package com.squareup.spoon.mojo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class ZipUtil {
  static void zip(File zipFile, File root) {
    zipFiles(zipFile, root, getAllFiles(root));
  }

  private static List<File> getAllFiles(File directory) {
    List<File> fileList = new ArrayList<File>();
    File[] files = directory.listFiles();
    if (files != null) {
      for (File file : files) {
        fileList.add(file);
        if (file.isDirectory()) {
          fileList.addAll(getAllFiles(file));
        }
      }
    }
    return fileList;
  }

  private static void zipFiles(File output, File root, List<File> fileList) {
    try {
      FileOutputStream fos = new FileOutputStream(output);
      ZipOutputStream zos = new ZipOutputStream(fos);

      for (File file : fileList) {
        if (file.isFile()) {
          addFileToZip(root, file, zos);
        }
      }

      zos.close();
      fos.close();
    } catch (IOException e) {
      throw new RuntimeException("Unable to write zip file.", e);
    }
  }

  private static void addFileToZip(File root, File file, ZipOutputStream zos) throws IOException {
    FileInputStream fis = new FileInputStream(file);

    String filePath = file.getCanonicalPath();
    String zipFilePath =
        filePath.substring(root.getCanonicalPath().length() + 1, filePath.length());
    ZipEntry zipEntry = new ZipEntry(zipFilePath);
    zos.putNextEntry(zipEntry);

    byte[] bytes = new byte[1024];
    int length;
    while ((length = fis.read(bytes)) >= 0) {
      zos.write(bytes, 0, length);
    }

    zos.closeEntry();
    fis.close();
  }
}
