package com.squareup.spoon.serialization;

import com.android.ddmlib.Log;
import com.android.ddmlib.logcat.LogCatHeader;
import com.android.ddmlib.logcat.LogCatMessage;
import com.android.ddmlib.logcat.LogCatTimestamp;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.lang.reflect.Type;
import java.util.Locale;

public class LogcatDeserializer implements JsonDeserializer<LogCatMessage> {
  @Override
  public LogCatMessage deserialize(
    JsonElement jsonElement, Type type,
    JsonDeserializationContext jsonDeserializationContext) {

    JsonObject jsonObject = (JsonObject) jsonElement;

    LogCatHeader header = new LogCatHeader(
      Log.LogLevel.getByString(jsonObject.get("logLevel").getAsString().toLowerCase(Locale.US)),
      jsonObject.get("pid").getAsInt(),
      jsonObject.get("tid").getAsInt(),
      null,
      jsonObject.get("tag").getAsString(),
      LogCatTimestamp.fromString(jsonObject.get("time").getAsString())
    );
    return new LogCatMessage(
      header,
      jsonObject.get("message").getAsString()
    );
  }
}
