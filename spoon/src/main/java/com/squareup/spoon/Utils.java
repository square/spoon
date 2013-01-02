package com.squareup.spoon;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.squareup.spoon.external.AXMLParser;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.commons.io.IOUtils;

import static com.android.ddmlib.SyncService.ISyncProgressMonitor;

final class Utils {
  private static final String ANDROID_MANIFEST_XML = "AndroidManifest.xml";
  private static final String TAG_MANIFEST = "manifest";
  private static final String TAG_INSTRUMENTATION = "instrumentation";
  private static final String ATTR_PACKAGE = "package";
  private static final String ATTR_TARGET_PACKAGE = "targetPackage";
  private static final String ATTR_NAME = "name";

  static final Gson GSON = new GsonBuilder() //
      .registerTypeAdapter(File.class, new TypeAdapter<File>() {
        @Override public void write(JsonWriter jsonWriter, File file) throws IOException {
          jsonWriter.value(file.getAbsolutePath());
        }

        @Override public File read(JsonReader jsonReader) throws IOException {
          return new File(jsonReader.nextString());
        }
      }) //
      .setPrettyPrinting() //
      .create();

  static final ISyncProgressMonitor QUIET_MONITOR = new ISyncProgressMonitor() {
        @Override public void start(int totalWork) {
        }

        @Override public void stop() {
        }

        @Override public boolean isCanceled() {
          return false;
        }

        @Override public void startSubTask(String name) {
        }

        @Override public void advance(int work) {
        }
      };

  /** Find all device serials that are plugged in through ADB. */
  static Set<String> findAllDevices(File sdkPath) {
    Set<String> devices = new HashSet<String>();
    AndroidDebugBridge adb = DdmlibHelper.init(sdkPath);
    for (IDevice realDevice : adb.getDevices()) {
      devices.add(realDevice.getSerialNumber());
    }
    AndroidDebugBridge.terminate();
    return devices;
  }

  /**
   * Parse key information from an instrumentation APK's manifest.
   *
   * @return Three strings: instrumentation package, application package, test runner class.
   */
  static String[] getManifestInfo(File apkTestFile) {
    InputStream is = null;
    try {
      ZipFile zip = new ZipFile(apkTestFile);
      ZipEntry entry = zip.getEntry(ANDROID_MANIFEST_XML);
      is = zip.getInputStream(entry);

      AXMLParser parser = new AXMLParser(is);
      int eventType = parser.getType();

      String[] ret = new String[3];
      while (eventType != AXMLParser.END_DOCUMENT) {
        if (eventType == AXMLParser.START_TAG) {
          String parserName = parser.getName();
          boolean isManifest = TAG_MANIFEST.equals(parserName);
          boolean isInstrumentation = TAG_INSTRUMENTATION.equals(parserName);
          if (isManifest || isInstrumentation) {
            for (int i = 0; i < parser.getAttributeCount(); i++) {
              String parserAttributeName = parser.getAttributeName(i);
              if (isManifest && ATTR_PACKAGE.equals(parserAttributeName)) {
                ret[1] = parser.getAttributeValueString(i);
              } else if (isInstrumentation && ATTR_TARGET_PACKAGE.equals(parserAttributeName)) {
                ret[0] = parser.getAttributeValueString(i);
              } else if (isInstrumentation && ATTR_NAME.equals(parserAttributeName)) {
                ret[2] = parser.getAttributeValueString(i);
              }
            }
          }
        }
        eventType = parser.next();
      }
      if (ret[0] == null || ret[1] == null) {
        throw new IllegalStateException("Unable to find both app and test package.");
      }
      return ret;
    } catch (IOException e) {
      throw new RuntimeException("Unable to parse test app AndroidManifest.xml.", e);
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

  private Utils() {
    // No instances.
  }
}
