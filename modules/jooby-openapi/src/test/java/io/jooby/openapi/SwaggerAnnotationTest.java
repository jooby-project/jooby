package io.jooby.openapi;

import examples.OpenApiApp;
import io.jooby.internal.openapi.OperationResponse;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SwaggerAnnotationTest {

  @OpenApiTest(value = OpenApiApp.class)
  public void shouldParseSwaggerAnnotations(RouteIterator iterator) {
    iterator
        .next(route -> {
          assertEquals("foo", route.getId());
          assertEquals(Collections.singleton("a"), route.getTags());
          assertEquals("description", route.getDescription());
          assertEquals("summary", route.getSummary());

          OperationResponse response = route.getReturnType();
          assertEquals("200", response.getCode());
          assertEquals("Success", response.getDescription());
          assertEquals("java.util.List<examples.Person>", response.getJavaType());

          OperationResponse notfound = route.getResponse().get(1);
          assertEquals("400", notfound.getCode());
          assertEquals("Bad Request", notfound.getDescription());
          assertEquals(null, notfound.getJavaType());
        })
        .next(route -> {
          assertEquals("find", route.getId());
          assertEquals(null, route.getDescription());
          assertEquals("Find Person by ID", route.getSummary());

          OperationResponse response = route.getReturnType();
          assertEquals("200", response.getCode());
          assertEquals("Found person", response.getDescription());
          assertEquals("examples.Person", response.getJavaType());

//          OperationResponse notfound = route.getResponse().get(1);
//          assertEquals("400", notfound.getCode());
//          assertEquals("Bad Request", notfound.getDescription());
//          assertEquals(null, notfound.getJavaType());
        })
        .verify();
  }
}
