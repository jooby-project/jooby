/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jackson;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import io.jooby.Projected;
import io.jooby.Projection;

public class JacksonProjectedSerializer extends JsonSerializer<Projected> {
  private final Map<Projection<?>, ObjectWriter> writerCache = new ConcurrentHashMap<>();

  private final ObjectMapper mapper;

  public JacksonProjectedSerializer(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public void serialize(
      Projected projected, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
      throws IOException {
    // Create a dynamic provider for this specific projection
    var writer =
        writerCache.computeIfAbsent(
            projected.getProjection(),
            p -> {
              var filters =
                  new SimpleFilterProvider()
                      .addFilter(JacksonModule.FILTER_ID, new JacksonProjectionFilter(p));
              return mapper.writer(filters);
            });

    // Write the value using the filtered writer
    writer.writeValue(jsonGenerator, projected.getValue());
  }
}
