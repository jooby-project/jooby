/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3746;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jooby.openapi.OpenAPITest;
import io.jooby.openapi.RouteIterator;
import kt.i3746.RunAppInlineWithServerKt;
import kt.i3746.RunAppInlineWithServerModeKt;
import kt.i3746.RunAppWithServerKt;
import kt.i3746.RunAppWithServerModeKt;

public class Issue3746 {

  @OpenAPITest(value = RunAppWithServerKt.class)
  public void shouldParseRunAppWithServer(RouteIterator iterator) {
    checkApp(iterator);
  }

  @OpenAPITest(value = RunAppInlineWithServerKt.class)
  public void shouldParseRunAppInlineWithServer(RouteIterator iterator) {
    checkApp(iterator);
  }

  @OpenAPITest(value = RunAppInlineWithServerModeKt.class)
  public void shouldParseRunAppInlineWithServerMode(RouteIterator iterator) {
    checkApp(iterator);
  }

  @OpenAPITest(value = RunAppWithServerModeKt.class)
  public void shouldParseRunAppWithServerMode(RouteIterator iterator) {
    checkApp(iterator);
  }

  private static void checkApp(RouteIterator iterator) {
    iterator
        .next(
            route -> {
              assertEquals("GET /3746", route.toString());
              assertEquals(Object.class.getName(), route.getDefaultResponse().getJavaType());
              assertEquals("get3746", route.getOperationId());
            })
        .verify();
  }
}
