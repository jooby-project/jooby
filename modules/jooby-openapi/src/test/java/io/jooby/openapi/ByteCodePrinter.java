/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.openapi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ByteCodePrinter {

  //  @Test
  //  public void appA() throws Exception {
  //    ASMifier.main(new String[]{RouteIdioms.class.getName()});
  //  }

  static class JsonType {

    public JavaType type;
  }

  @Test
  public void encodeJavaType() throws JsonProcessingException {

    ObjectMapper mapper = new ObjectMapper();

    JsonType value = new JsonType();
    value.type = mapper.getTypeFactory().constructType(String[].class);

    String json = mapper.writer().withDefaultPrettyPrinter().writeValueAsString(value);
    assertEquals("{\n" + "  \"type\" : \"[Ljava.lang.String;\"\n" + "}", json);
    JsonType readIt = mapper.readValue(json, JsonType.class);
    assertEquals(String[].class, readIt.type.getRawClass());
  }
}
