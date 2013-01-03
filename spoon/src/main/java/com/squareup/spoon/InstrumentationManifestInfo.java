package com.squareup.spoon;

import com.squareup.spoon.external.AXMLParser;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.io.IOUtils;

import static com.google.common.base.Preconditions.checkNotNull;

public final class InstrumentationManifestInfo {
  private final String applicationPackage;
  private final String instrumentationPackage;
  private final String testRunnerClass;

  public InstrumentationManifestInfo(String applicationPackage, String instrumentationPackage,
      String testRunnerClass) {
    this.applicationPackage = applicationPackage;
    this.instrumentationPackage = instrumentationPackage;
    this.testRunnerClass = testRunnerClass;
  }

  public String getApplicationPackage() {
    return applicationPackage;
  }

  public String getInstrumentationPackage() {
    return instrumentationPackage;
  }

  public String getTestRunnerClass() {
    return testRunnerClass;
  }

  /** Parse key information from an instrumentation APK's manifest. */
  public static InstrumentationManifestInfo parseFromFile(File apkTestFile) {
    InputStream is = null;
    try {
      ZipFile zip = new ZipFile(apkTestFile);
      ZipEntry entry = zip.getEntry("AndroidManifest.xml");
      is = zip.getInputStream(entry);

      AXMLParser parser = new AXMLParser(is);
      int eventType = parser.getType();

      String appPackage = null;
      String testPackage = null;
      String testRunnerClass = null;
      while (eventType != AXMLParser.END_DOCUMENT) {
        if (eventType == AXMLParser.START_TAG) {
          String parserName = parser.getName();
          boolean isManifest = "manifest".equals(parserName);
          boolean isInstrumentation = "instrumentation".equals(parserName);
          if (isManifest || isInstrumentation) {
            for (int i = 0; i < parser.getAttributeCount(); i++) {
              String parserAttributeName = parser.getAttributeName(i);
              if (isManifest && "package".equals(parserAttributeName)) {
                testPackage = parser.getAttributeValueString(i);
              } else if (isInstrumentation && "targetPackage".equals(parserAttributeName)) {
                appPackage = parser.getAttributeValueString(i);
              } else if (isInstrumentation && "name".equals(parserAttributeName)) {
                testRunnerClass = parser.getAttributeValueString(i);
              }
            }
          }
        }
        eventType = parser.next();
      }
      checkNotNull(testPackage, "Could not find test application package.");
      checkNotNull(appPackage, "Could not find application package.");
      checkNotNull(testRunnerClass, "Could not find test runner class.");

      // Support relative declaration of instrumentation test runner.
      if (testRunnerClass.startsWith(".")) {
        testRunnerClass = testPackage + testRunnerClass;
      } else if (!testRunnerClass.contains(".")) {
        testRunnerClass = testPackage + "." + testRunnerClass;
      }

      return new InstrumentationManifestInfo(appPackage, testPackage, testRunnerClass);
    } catch (IOException e) {
      throw new RuntimeException("Unable to parse test app AndroidManifest.xml.", e);
    } finally {
      IOUtils.closeQuietly(is);
    }
  }
}
