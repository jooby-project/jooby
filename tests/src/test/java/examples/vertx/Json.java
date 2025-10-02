/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples.vertx;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jooby.SneakyThrows;

public class Json {

  private static final ObjectMapper mapper = new ObjectMapper();

  public static byte[] encode(Object value) {
    try {
      return mapper.writeValueAsBytes(value);
    } catch (Exception e) {
      throw SneakyThrows.propagate(e);
    }
  }
}
