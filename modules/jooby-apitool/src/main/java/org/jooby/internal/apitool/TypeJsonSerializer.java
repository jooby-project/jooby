package org.jooby.internal.apitool;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.lang.reflect.Type;

class TypeJsonSerializer extends JsonSerializer<Type> {
  @Override public void serialize(final Type value, final JsonGenerator gen,
      final SerializerProvider serializers) throws IOException {
    gen.writeObject(value.getTypeName());
  }
}
