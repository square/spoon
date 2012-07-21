package com.squareup.spoon;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.squareup.spoon.model.Density;
import com.squareup.spoon.model.Orientation;
import com.squareup.spoon.model.Resolution;
import com.squareup.spoon.model.ScreenLong;
import com.squareup.spoon.model.ScreenSize;

import java.io.File;
import java.io.IOException;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.PropertyAccessor.FIELD;
import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;

public final class SpoonMapper extends ObjectMapper {
  private static SpoonMapper INSTANCE;

  public static SpoonMapper getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new SpoonMapper();
      INSTANCE.setVisibility(FIELD, ANY);
      INSTANCE.enable(INDENT_OUTPUT);
      INSTANCE.registerModule(new SpoonModule());
    }
    return INSTANCE;
  }

  private SpoonMapper() {
    // Instantiated internally only.
  }

  public static final class SpoonModule extends SimpleModule {
    public SpoonModule() {
      addDeserializer(Density.class, new JsonDeserializer<Density>() {
        @Override
        public Density deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
          return Density.get(jp.getText());
        }
      });
      addDeserializer(Orientation.class, new JsonDeserializer<Orientation>() {
        @Override
        public Orientation deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
          return Orientation.get(jp.getText());
        }
      });
      addDeserializer(Resolution.class, new JsonDeserializer<Resolution>() {
        @Override
        public Resolution deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
          return Resolution.parse(jp.getText());
        }
      });
      addDeserializer(ScreenLong.class, new JsonDeserializer<ScreenLong>() {
        @Override
        public ScreenLong deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
          return ScreenLong.get(jp.getText());
        }
      });
      addDeserializer(ScreenSize.class, new JsonDeserializer<ScreenSize>() {
        @Override
        public ScreenSize deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
          return ScreenSize.get(jp.getText());
        }
      });
      addDeserializer(File.class, new JsonDeserializer<File>() {
        @Override
        public File deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
          return new File(jp.getText());
        }
      });

      addSerializer(File.class, new JsonSerializer<File>() {
        @Override
        public void serialize(File value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
          jgen.writeString(value.getAbsolutePath());
        }
      });
    }
  }
}
