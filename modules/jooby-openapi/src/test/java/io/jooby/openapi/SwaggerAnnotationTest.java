package io.jooby.openapi;

import examples.OpenApiApp;
import io.jooby.internal.openapi.Response;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SwaggerAnnotationTest {

  @OpenApiTest(value = OpenApiApp.class, ignoreArguments = true)
  public void shouldParseSwaggerAnnotations(RouteIterator iterator) {
    iterator
        .next(route -> {
          assertEquals("foo", route.getOperationId());
          assertEquals(Collections.singletonList("a"), route.getTags());
          assertEquals("description", route.getDescription());
          assertEquals("summary", route.getSummary());

          Response response = route.getResponse();
          assertEquals("default", response.getCode());
          assertEquals("Success", response.getDescription());
          assertEquals("java.util.List<examples.Person>", response.getJavaType());

          Response notfound = route.getResponse("400");
          assertEquals("Bad Request", notfound.getDescription());
          assertEquals(null, notfound.getJavaType());
        })
        .next(route -> {
          assertEquals("find", route.getOperationId());
          assertEquals(null, route.getDescription());
          assertEquals("Find Person by ID", route.getSummary());

          Response response = route.getResponse();
          assertEquals("default", response.getCode());
          assertEquals("Found person", response.getDescription());
          assertEquals("examples.Person", response.getJavaType());
        })
        .verify();
  }
}
