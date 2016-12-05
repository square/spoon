package com.squareup.spoon.serialization;


import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.squareup.spoon.DeviceTestResult;

import java.lang.reflect.Type;

public class StatusDeserializer implements JsonDeserializer<DeviceTestResult.Status>,
  JsonSerializer<DeviceTestResult.Status> {

  @Override
  public DeviceTestResult.Status deserialize(
    JsonElement jsonElement, Type type,
    JsonDeserializationContext jsonDeserializationContext) {
    return fromString(jsonElement.getAsString());
  }

  private DeviceTestResult.Status fromString(String statusString) {

    for (DeviceTestResult.Status value : DeviceTestResult.Status.values()) {
      if (value.name().equals(statusString)) {
        return value;
      }
    }

    if (statusString != null && statusString.equals("ERROR")) {
      return DeviceTestResult.Status.FAIL;
    }

    return null;
  }

  @Override
  public JsonElement serialize(
    DeviceTestResult.Status status, Type type,
    JsonSerializationContext jsonSerializationContext) {

    if (status != null) {
      return new JsonPrimitive(status.toString());
    }

    return new JsonPrimitive(DeviceTestResult.Status.FAIL.toString());
  }
}
