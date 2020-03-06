package io.jooby.swagger;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SwaggerResourceTest {

  @Test
  public void shouldCheckIndexPage() throws IOException {
    String index = asset("index.html");
    assertTrue(index.contains("${swaggerPath}"), index);
    assertTrue(index.contains("${openAPIPath}"), index);
  }

  private String asset(String resource) throws IOException {
    return IOUtils.toString(getClass().getResource("/swagger-ui/" + resource), "UTF-8");
  }
}
