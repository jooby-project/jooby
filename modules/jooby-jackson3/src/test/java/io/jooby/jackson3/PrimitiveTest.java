/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jackson3;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

public class PrimitiveTest {

  @Test
  public void shouldParsePrimitive() {
    var mapper = new ObjectMapper();
    assertEquals(1, mapper.readValue("1", long.class));
  }
}
