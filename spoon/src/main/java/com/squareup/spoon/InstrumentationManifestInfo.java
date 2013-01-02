package com.squareup.spoon;

import com.squareup.spoon.external.AXMLParser;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.io.IOUtils;

import static com.google.common.base.Preconditions.checkNotNull;

public class InstrumentationManifestInfo {
  private static final String ANDROID_MANIFEST_XML = "AndroidManifest.xml";
  private static final String TAG_MANIFEST = "manifest";
  private static final String TAG_INSTRUMENTATION = "instrumentation";
  private static final String ATTR_PACKAGE = "package";
  private static final String ATTR_TARGET_PACKAGE = "targetPackage";
  private static final String ATTR_NAME = "name";

  public final String applicationPackage;
  public final String instrumentationPackage;
  public final String testRunnerClass;

  public InstrumentationManifestInfo(String applicationPackage, String instrumentationPackage,
      String testRunnerClass) {
    this.applicationPackage = applicationPackage;
    this.instrumentationPackage = instrumentationPackage;
    this.testRunnerClass = testRunnerClass;
  }

  /** Parse key information from an instrumentation APK's manifest. */
  public static InstrumentationManifestInfo parseFromFile(File apkTestFile) {
    InputStream is = null;
    try {
      ZipFile zip = new ZipFile(apkTestFile);
      ZipEntry entry = zip.getEntry(ANDROID_MANIFEST_XML);
      is = zip.getInputStream(entry);

      AXMLParser parser = new AXMLParser(is);
      int eventType = parser.getType();

      String appPackage = null;
      String testPackage = null;
      String testRunnerClass = null;
      while (eventType != AXMLParser.END_DOCUMENT) {
        if (eventType == AXMLParser.START_TAG) {
          String parserName = parser.getName();
          boolean isManifest = TAG_MANIFEST.equals(parserName);
          boolean isInstrumentation = TAG_INSTRUMENTATION.equals(parserName);
          if (isManifest || isInstrumentation) {
            for (int i = 0; i < parser.getAttributeCount(); i++) {
              String parserAttributeName = parser.getAttributeName(i);
              if (isManifest && ATTR_PACKAGE.equals(parserAttributeName)) {
                testPackage = parser.getAttributeValueString(i);
              } else if (isInstrumentation && ATTR_TARGET_PACKAGE.equals(parserAttributeName)) {
                appPackage = parser.getAttributeValueString(i);
              } else if (isInstrumentation && ATTR_NAME.equals(parserAttributeName)) {
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
      return new InstrumentationManifestInfo(appPackage, testPackage, testRunnerClass);
    } catch (IOException e) {
      throw new RuntimeException("Unable to parse test app AndroidManifest.xml.", e);
    } finally {
      IOUtils.closeQuietly(is);
    }
  }
}
