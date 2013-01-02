package com.squareup.spoon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.File;
import java.io.IOException;

import static com.android.ddmlib.SyncService.ISyncProgressMonitor;

final class Utils {
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

  private Utils() {
    // No instances.
  }
}
