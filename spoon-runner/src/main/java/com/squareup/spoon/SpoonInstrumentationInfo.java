package com.squareup.spoon;

import com.squareup.spoon.internal.thirdparty.axmlparser.AXMLParser;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import static com.google.common.base.Preconditions.checkNotNull;

/** Detailed instrumentation information. */
final class SpoonInstrumentationInfo {
  private final String applicationPackage;
  private final Integer minSdkVersion;
  private final String instrumentationPackage;
  private final String testRunnerClass;

  SpoonInstrumentationInfo(String applicationPackage, Integer minSdkVersion,
      String instrumentationPackage, String testRunnerClass) {
    this.applicationPackage = applicationPackage;
    this.minSdkVersion = minSdkVersion;
    this.instrumentationPackage = instrumentationPackage;
    this.testRunnerClass = testRunnerClass;
  }

  String getApplicationPackage() {
    return applicationPackage;
  }

  Integer getMinSdkVersion() {
    return minSdkVersion;
  }

  String getInstrumentationPackage() {
    return instrumentationPackage;
  }

  String getTestRunnerClass() {
    return testRunnerClass;
  }

  @Override public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  /** Parse key information from an instrumentation APK's manifest. */
  static SpoonInstrumentationInfo parseFromFile(File apkTestFile) {
    InputStream is = null;
    try {
      ZipFile zip = new ZipFile(apkTestFile);
      ZipEntry entry = zip.getEntry("AndroidManifest.xml");
      is = zip.getInputStream(entry);

      AXMLParser parser = new AXMLParser(is);
      int eventType = parser.getType();

      String appPackage = null;
      Integer minSdkVersion = null;
      String testPackage = null;
      String testRunnerClass = null;
      while (eventType != AXMLParser.END_DOCUMENT) {
        if (eventType == AXMLParser.START_TAG) {
          String parserName = parser.getName();
          boolean isManifest = "manifest".equals(parserName);
          boolean isUsesSdk = "uses-sdk".equals(parserName);
          boolean isInstrumentation = "instrumentation".equals(parserName);
          if (isManifest || isInstrumentation || isUsesSdk) {
            for (int i = 0; i < parser.getAttributeCount(); i++) {
              String parserAttributeName = parser.getAttributeName(i);
              if (isManifest && "package".equals(parserAttributeName)) {
                testPackage = parser.getAttributeValueString(i);
              } else if (isUsesSdk && "minSdkVersion".equals(parserAttributeName)) {
                minSdkVersion = parser.getAttributeValue(i);
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

      return new SpoonInstrumentationInfo(appPackage, minSdkVersion, testPackage, testRunnerClass);
    } catch (IOException e) {
      throw new RuntimeException("Unable to parse test app AndroidManifest.xml.", e);
    } finally {
      IOUtils.closeQuietly(is);
    }
  }
}
