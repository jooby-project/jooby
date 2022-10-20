/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i1934;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.jooby.internal.openapi.ResponseExt;
import io.jooby.openapi.OpenAPITest;
import io.jooby.openapi.RouteIterator;

public class Issue1934 {

  @OpenAPITest(value = App1934.class, ignoreArguments = true)
  public void shouldParseContextParamArg(RouteIterator iterator) {
    iterator
        .next(
            route -> {
              assertNull(route.getParameters());
              ResponseExt response = route.getDefaultResponse();
              assertEquals("200", response.getCode());
              assertEquals("Success", response.getDescription());
              assertEquals("examples.Person", response.getJavaType());
            })
        .verify();
  }
}
