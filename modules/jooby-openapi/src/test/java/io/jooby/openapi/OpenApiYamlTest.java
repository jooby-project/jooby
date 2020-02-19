package io.jooby.openapi;

import examples.MinApp;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OpenApiYamlTest {

  @OpenApiTest(MinApp.class)
  public void shouldGenerateMinApp(String yaml) {
    assertEquals("", yaml);
  }
}
