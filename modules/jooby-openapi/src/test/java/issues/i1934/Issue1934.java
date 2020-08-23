package issues.i1934;

import io.jooby.internal.openapi.ResponseExt;
import io.jooby.openapi.OpenAPITest;
import io.jooby.openapi.RouteIterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class Issue1934 {

  @OpenAPITest(value = App1934.class, ignoreArguments = true)
  public void shouldParseContextParamArg(RouteIterator iterator) {
    iterator.next(route -> {
          assertNull(route.getParameters());
          ResponseExt response = route.getDefaultResponse();
          assertEquals("200", response.getCode());
          assertEquals("Success", response.getDescription());
          assertEquals("examples.Person", response.getJavaType());
        }).verify();
  }
}
