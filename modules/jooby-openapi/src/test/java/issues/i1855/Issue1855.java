package issues.i1855;

import io.jooby.internal.openapi.ResponseExt;
import io.jooby.openapi.OpenAPITest;
import io.jooby.openapi.RouteIterator;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Issue1855 {

  @OpenAPITest(value = App1855.class, ignoreArguments = true)
  public void shouldParseSwaggerAnnotations(RouteIterator iterator) {
    iterator.next(route -> {
          ResponseExt response = route.getDefaultResponse();
          assertEquals("200", response.getCode());
          assertEquals("Success", response.getDescription());
          assertEquals("examples.Person", response.getJavaType());
        }).verify();
  }
}
