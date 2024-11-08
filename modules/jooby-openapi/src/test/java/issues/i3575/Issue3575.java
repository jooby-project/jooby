/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3575;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.openapi.OpenAPIResult;
import io.jooby.openapi.OpenAPITest;

public class Issue3575 {

  @OpenAPITest(value = App3575.class)
  public void shouldHideFromLambdaReference(OpenAPIResult result) {
    assertEquals(
        "openapi: 3.0.1\n"
            + "info:\n"
            + "  title: 3575 API\n"
            + "  description: 3575 API description\n"
            + "  version: \"1.0\"\n"
            + "paths: {}\n",
        result.toYaml());
  }
}
