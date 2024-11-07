/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.swagger;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

public class SwaggerResourceTest {

  @Test
  public void shouldCheckIndexPage() throws IOException {
    String index = asset("index.html");
    assertTrue(index.contains("${swaggerPath}"), index);
    assertTrue(index.contains("${swaggerPath}/index.css"), index);
  }

  @Test
  public void shouldCheckSwaggerInitializer() throws IOException {
    String index = asset("swagger-initializer.js");
    assertTrue(index.contains("${openAPIPath}"), index);
  }

  private String asset(String resource) throws IOException {
    return IOUtils.toString(
        Objects.requireNonNull(getClass().getResource("/swagger-ui/" + resource)),
        StandardCharsets.UTF_8);
  }
}
