package io.jooby.redoc;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RedocResourceTest {
  @Test
  public void shouldCheckIndexPage() throws IOException {
    String index = asset("index.html");
    assertTrue(index.contains("${redocPath}"), index);
    assertTrue(index.contains("${openAPIPath}"), index);
  }

  @Test
  public void shouldCheckBundle() throws IOException {
    String index = asset("redoc.standalone.js");
    assertNotNull(index);
  }

  private String asset(String resource) throws IOException {
    return IOUtils.toString(getClass().getResource("/redoc/" + resource), "UTF-8");
  }
}
